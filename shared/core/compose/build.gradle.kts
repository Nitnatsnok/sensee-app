plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
                api(libs.kotlinx.collections.immutable)
                api(projects.shared.core.presentation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
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
        namespace = "app.sensee.core.compose"
    }
}
