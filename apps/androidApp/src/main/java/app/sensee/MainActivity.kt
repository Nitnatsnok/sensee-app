package app.sensee

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import app.sensee.appShell.AndroidPlatformEnvironment
import app.sensee.appShell.App
import app.sensee.appShell.root.createAppRoot
import app.sensee.database.DatabaseConfig
import app.sensee.ui.designSystem.theme.SenseeTheme
import com.arkivanov.decompose.defaultComponentContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val rootComponent =
            createAppRoot(
                componentContext = defaultComponentContext(),
                platformEnvironment =
                    AndroidPlatformEnvironment(
                        applicationContext = applicationContext,
                    ),
                databaseConfig = DatabaseConfig(resetOnSchemaMigration = isDebuggable),
            )

        setContent {
            App(
                rootComponent = rootComponent,
                contentFrame = { content ->
                    SenseeSystemBars(window = window)
                    content()
                },
            )
        }
    }
}

@Composable
private fun SenseeSystemBars(window: Window) {
    val backgroundLuminance = SenseeTheme.colors.background.luminance()
    val useDarkSystemBarIcons = backgroundLuminance > LIGHT_BACKGROUND_LUMINANCE_THRESHOLD
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).run {
            isAppearanceLightStatusBars = useDarkSystemBarIcons
            isAppearanceLightNavigationBars = useDarkSystemBarIcons
        }
    }
}

private const val LIGHT_BACKGROUND_LUMINANCE_THRESHOLD = 0.5f
