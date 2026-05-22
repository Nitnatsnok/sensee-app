package app.sensee.appShell.primary

import app.sensee.core.platform.Platform
import app.sensee.ui.adaptive.AppAdaptiveInfo
import app.sensee.ui.adaptive.HeightSizeClass
import app.sensee.ui.adaptive.WidthSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class PrimaryNavLayoutTest {
    private val wideWindow = AppAdaptiveInfo(WidthSizeClass.ExtraLarge, HeightSizeClass.Expanded)
    private val compactWindow = AppAdaptiveInfo(WidthSizeClass.Compact, HeightSizeClass.Expanded)

    @Test
    fun `web in a wide window uses the top bar`() {
        assertEquals(PrimaryNavLayout.TopBar, selectPrimaryNavLayout(Platform.Js, wideWindow))
        assertEquals(PrimaryNavLayout.TopBar, selectPrimaryNavLayout(Platform.WasmJs, wideWindow))
    }

    @Test
    fun `non-web in a wide window keeps the navigation rail`() {
        assertEquals(PrimaryNavLayout.NavigationRail, selectPrimaryNavLayout(Platform.Desktop, wideWindow))
        assertEquals(PrimaryNavLayout.NavigationRail, selectPrimaryNavLayout(Platform.Android, wideWindow))
        assertEquals(PrimaryNavLayout.NavigationRail, selectPrimaryNavLayout(Platform.Ios, wideWindow))
    }

    @Test
    fun `a compact window keeps the bottom bar on every platform`() {
        assertEquals(PrimaryNavLayout.BottomBar, selectPrimaryNavLayout(Platform.Js, compactWindow))
        assertEquals(PrimaryNavLayout.BottomBar, selectPrimaryNavLayout(Platform.Desktop, compactWindow))
    }

    @Test
    fun `a wide-but-short window falls back to the bottom bar even on web`() {
        // showNavigationRail flips to false when HeightSizeClass.Compact (landscape phone,
        // narrow desktop strip), regardless of width — so neither rail nor top bar runs.
        val wideButShort = AppAdaptiveInfo(WidthSizeClass.ExtraLarge, HeightSizeClass.Compact)
        assertEquals(PrimaryNavLayout.BottomBar, selectPrimaryNavLayout(Platform.Js, wideButShort))
        assertEquals(PrimaryNavLayout.BottomBar, selectPrimaryNavLayout(Platform.Desktop, wideButShort))
    }
}
