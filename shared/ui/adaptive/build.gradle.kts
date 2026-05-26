plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.adaptive)
                implementation(libs.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(projects.shared.ui.designSystem)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.compose.uiTest)
                implementation(libs.compose.uiTest.junit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
    android {
        namespace = "app.sensee.ui.adaptive"
    }
}
