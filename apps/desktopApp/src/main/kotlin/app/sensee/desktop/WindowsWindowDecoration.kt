package app.sensee.desktop

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.WindowState
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD.LONG_PTR
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.LRESULT
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinUser.WindowProc
import com.sun.jna.win32.W32APIOptions
import java.util.logging.Level
import java.util.logging.Logger

private const val WM_NCCALCSIZE = 0x0083
private const val GWLP_WNDPROC = -4

// GetSystemMetrics indices for the resize-frame thickness on each axis plus
// the padded border that Aero/DWM adds on top. Their sum is the inset Windows
// hides behind decorations on a maximized window; we have to subtract it
// ourselves to keep a borderless window inside the work area.
private const val SM_CXSIZEFRAME = 32
private const val SM_CYSIZEFRAME = 33
private const val SM_CXPADDEDBORDER = 92

// Byte offsets within a Win32 RECT (4 × 32-bit ints).
private const val RECT_LEFT = 0L
private const val RECT_TOP = 4L
private const val RECT_RIGHT = 8L
private const val RECT_BOTTOM = 12L

private val logger: Logger = Logger.getLogger("app.sensee.desktop.WindowsWindowDecoration")

// Installed window procedures are held for the whole process so the JNA
// callback is never garbage-collected while Windows still calls into it.
private val installedProcedures = mutableListOf<Any>()

internal val isWindows: Boolean
    get() = System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

/**
 * Turns [window] into a borderless-yet-native window on Windows: a custom
 * window procedure answers `WM_NCCALCSIZE` with 0 so the whole window becomes
 * client area (no native title bar), while the window keeps being a real
 * native window — so the OS still owns the minimize/maximize/restore/open/close
 * animations, the drop shadow and Windows 11 rounded corners that a plain
 * `undecorated` window loses.
 *
 * No-op on macOS/Linux. Background: https://youtrack.jetbrains.com/issue/CMP-3388
 */
internal fun installWindowsWindowDecoration(window: ComposeWindow) {
    if (!isWindows) return
    try {
        installedProcedures += WindowsWindowProcedure(HWND(Pointer(window.windowHandle)))
    } catch (error: Throwable) {
        logger.log(Level.WARNING, "Custom window decoration unavailable", error)
    }
}

/**
 * Minimizes [window]. On Windows the native `ShowWindow(SW_MINIMIZE)` call is
 * used so the taskbar animation plays (`WindowState.isMinimized` alone does not
 * animate a borderless window); elsewhere it falls back to [WindowState].
 */
internal fun minimizeWindow(
    window: ComposeWindow,
    windowState: WindowState,
) {
    if (!isWindows) {
        windowState.isMinimized = true
        return
    }
    try {
        User32.INSTANCE.ShowWindow(HWND(Pointer(window.windowHandle)), WinUser.SW_MINIMIZE)
    } catch (error: Throwable) {
        logger.log(Level.WARNING, "Native minimize failed; using fallback", error)
        windowState.isMinimized = true
    }
}

/** JNA bindings to the [User32] entry points used for the window-procedure swap. */
@Suppress("FunctionName")
private interface User32Ex : User32 {
    fun SetWindowLong(
        hWnd: HWND,
        nIndex: Int,
        procedure: WindowProc,
    ): LONG_PTR

    fun SetWindowLongPtr(
        hWnd: HWND,
        nIndex: Int,
        procedure: WindowProc,
    ): LONG_PTR

    fun CallWindowProc(
        proc: LONG_PTR,
        hWnd: HWND,
        uMsg: Int,
        uParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT

    fun IsZoomed(hWnd: HWND): Boolean
}

/**
 * DWM frame margins; (0, -1, 0, -1) extends the native border/shadow over the
 * client area. Must be `internal` (not `private`): JNA reflects over the fields
 * from its own package, which a package-private class would deny.
 */
@Structure.FieldOrder("left", "right", "top", "bottom")
internal class WindowMargins(
    @JvmField var left: Int,
    @JvmField var right: Int,
    @JvmField var top: Int,
    @JvmField var bottom: Int,
) : Structure(),
    Structure.ByReference

private class WindowsWindowProcedure(
    private val handle: HWND,
) : WindowProc {
    private val user32: User32Ex =
        Native.load("user32", User32Ex::class.java, W32APIOptions.DEFAULT_OPTIONS)

    // Swapping in our procedure returns the original; every message except
    // WM_NCCALCSIZE is delegated back to it.
    private val defaultProcedure: LONG_PTR =
        if (is64Bit()) {
            user32.SetWindowLongPtr(handle, GWLP_WNDPROC, this)
        } else {
            user32.SetWindowLong(handle, GWLP_WNDPROC, this)
        }

    init {
        enableResizability()
        enableBorderAndShadow()
    }

    override fun callback(
        hWnd: HWND,
        uMsg: Int,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT =
        when (uMsg) {
            WM_NCCALCSIZE -> handleNcCalcSize(hWnd, wParam, lParam)
            else -> user32.CallWindowProc(defaultProcedure, hWnd, uMsg, wParam, lParam)
        }

    // 0 tells Windows the non-client area is empty: the whole window becomes
    // our client area, so no native title bar is drawn. The maximized branch
    // additionally shrinks the proposed client rect by the frame thickness —
    // otherwise Windows extends the window past the work area (the border that
    // decorations would normally hide).
    private fun handleNcCalcSize(
        hWnd: HWND,
        wParam: WPARAM,
        lParam: LPARAM,
    ): LRESULT {
        if (wParam.toLong() != 0L && user32.IsZoomed(hWnd)) {
            shrinkMaximizedClientRect(lParam)
        }
        return LRESULT(0)
    }

    private fun shrinkMaximizedClientRect(lParam: LPARAM) {
        val rect = Pointer(lParam.toLong())
        val frameX =
            user32.GetSystemMetrics(SM_CXSIZEFRAME) + user32.GetSystemMetrics(SM_CXPADDEDBORDER)
        val frameY =
            user32.GetSystemMetrics(SM_CYSIZEFRAME) + user32.GetSystemMetrics(SM_CXPADDEDBORDER)
        rect.setInt(RECT_LEFT, rect.getInt(RECT_LEFT) + frameX)
        rect.setInt(RECT_TOP, rect.getInt(RECT_TOP) + frameY)
        rect.setInt(RECT_RIGHT, rect.getInt(RECT_RIGHT) - frameX)
        rect.setInt(RECT_BOTTOM, rect.getInt(RECT_BOTTOM) - frameY)
    }

    // Re-adding WS_CAPTION keeps native resize and maximize working on the
    // otherwise borderless window.
    private fun enableResizability() {
        val style = user32.GetWindowLong(handle, WinUser.GWL_STYLE)
        user32.SetWindowLong(handle, WinUser.GWL_STYLE, style or WinUser.WS_CAPTION)
    }

    private fun enableBorderAndShadow() {
        try {
            NativeLibrary
                .getInstance("dwmapi")
                .getFunction("DwmExtendFrameIntoClientArea")
                .invoke(arrayOf(handle, WindowMargins(left = 0, right = -1, top = 0, bottom = -1)))
        } catch (error: Throwable) {
            logger.log(Level.WARNING, "Native window shadow/border unavailable", error)
        }
    }

    private fun is64Bit(): Boolean =
        System.getProperty("sun.arch.data.model") == "64" ||
            System.getProperty("os.arch").orEmpty().contains("64")
}
