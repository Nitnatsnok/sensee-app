plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.settings.domain)
                implementation(projects.shared.ai.core)
                implementation(projects.shared.tts.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.metro.runtime)
                implementation(projects.shared.feature.profile.presentation.api)
                implementation(projects.shared.feature.profile.presentation.navigationApi)
                implementation(projects.shared.ui.designSystem)
                implementation(projects.shared.ui.adaptive)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.shared.core.testKit)
                implementation(projects.shared.ai.core)
                implementation(projects.shared.tts.core)
            }
        }
        jvmTest {
            dependencies {
                // UI tests live only here — the Compose UI test runtime is
                // JVM-only (Skiko + JUnit4). ui-test-junit4 alone fails at
                // runtime with LibraryLoadException without skiko-awt-runtime,
                // which currentOs pulls for the host platform.
                implementation(libs.compose.uiTest)
                implementation(libs.compose.uiTest.junit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.profile.presentation.impl"
    }
}
