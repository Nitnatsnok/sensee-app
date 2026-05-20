import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.sensee.kmpWebToolchain)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.appShell)
            implementation(projects.shared.core.decompose)
            implementation(projects.shared.core.platform)

            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
