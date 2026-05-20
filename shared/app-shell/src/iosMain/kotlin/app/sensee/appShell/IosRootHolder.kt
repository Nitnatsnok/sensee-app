package app.sensee.appShell

import app.sensee.appShell.root.RootComponent
import app.sensee.appShell.root.createAppRoot
import app.sensee.database.DatabaseConfig
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import kotlin.experimental.ExperimentalNativeApi

public class IosRootHolder {
    private val lifecycle = LifecycleRegistry()

    @OptIn(ExperimentalNativeApi::class)
    public val rootComponent: RootComponent =
        createAppRoot(
            componentContext =
                DefaultComponentContext(
                    lifecycle = lifecycle,
                ),
            platformEnvironment = IosPlatformEnvironment(),
            databaseConfig =
                DatabaseConfig(
                    resetOnSchemaMigration = Platform.isDebugBinary,
                ),
        )

    private var destroyed: Boolean = false

    init {
        lifecycle.create()
    }

    public fun resume() {
        if (!destroyed) {
            lifecycle.resume()
        }
    }

    public fun stop() {
        if (!destroyed) {
            lifecycle.stop()
        }
    }

    public fun destroy() {
        if (!destroyed) {
            destroyed = true
            lifecycle.destroy()
        }
    }
}
