package app.sensee

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.sensee.appShell.AndroidPlatformEnvironment
import app.sensee.appShell.App
import app.sensee.appShell.root.createAppRoot
import app.sensee.database.DatabaseConfig
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
            App(rootComponent = rootComponent)
        }
    }
}
