package app.sensee.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

class KmpWebToolchainPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            configureKotlinManagedWebToolchainRepositories()
        }
}

@OptIn(ExperimentalWasmDsl::class)
private fun Project.configureKotlinManagedWebToolchainRepositories() {
    plugins.withType(NodeJsPlugin::class.java) {
        extensions.configure<NodeJsEnvSpec>(NodeJsEnvSpec.EXTENSION_NAME) {
            downloadBaseUrl.set(null as String?)
        }
    }

    plugins.withType(WasmNodeJsPlugin::class.java) {
        extensions.configure<WasmNodeJsEnvSpec>(WasmNodeJsEnvSpec.EXTENSION_NAME) {
            downloadBaseUrl.set(null as String?)
        }
    }

    plugins.withType(BinaryenPlugin::class.java) {
        extensions.configure<BinaryenEnvSpec>(BinaryenEnvSpec.EXTENSION_NAME) {
            downloadBaseUrl.set(null as String?)
        }
    }
}
