plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.decompose.extensions.compose)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.decompose)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.core.presentation)
                implementation(projects.shared.feature.home.presentation.api)
                implementation(projects.shared.feature.home.presentation.navigationApi)
                implementation(projects.shared.feature.practice.domain)
                implementation(projects.shared.feature.practice.presentation.navigationApi)
                implementation(projects.shared.settings.domain)
                implementation(projects.shared.ui.designSystem)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.shared.core.testKit)
            }
        }
        jvmTest {
            dependencies {
                // UI tests live only here — the Compose UI test runtime is JVM-only
                // (Skiko + JUnit4); compose.desktop.currentOs pulls skiko for the host.
                implementation(libs.compose.uiTest)
                implementation(libs.compose.uiTest.junit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.home.presentation.impl"
    }
}
