plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)

    alias(libs.plugins.metro)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android {
        namespace = "app.sensee.appShell"
        androidResources.enable = true
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.ui)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                api(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.core)

                api(projects.shared.ui.designSystem)
                implementation(projects.shared.ui.adaptive)

                api(projects.shared.database)
                implementation(projects.shared.settings.data)
                implementation(projects.shared.core.compose)
                implementation(projects.shared.core.coroutines)
                implementation(libs.metro.runtime)
                implementation(projects.shared.core.decompose)
                implementation(libs.decompose.extensions.compose)
                implementation(projects.shared.core.platform)
                api(projects.shared.core.presentation)
                implementation(projects.shared.core.mockBackend)
                implementation(projects.shared.core.network)
                implementation(projects.shared.core.secureStorage)
                implementation(projects.shared.core.tracing)
                implementation(projects.shared.ai.integration)
                implementation(projects.shared.verification.integration)
                implementation(projects.shared.grammar.data)

                implementation(projects.shared.feature.home.presentation.api)
                implementation(projects.shared.feature.home.presentation.navigationApi)
                implementation(projects.shared.feature.home.presentation.impl)
                implementation(projects.shared.feature.practice.presentation.api)
                implementation(projects.shared.feature.practice.presentation.navigationApi)
                implementation(projects.shared.feature.practice.data)
                implementation(projects.shared.feature.practice.presentation.impl)
                implementation(projects.shared.srs.fsrsEngine)
                implementation(projects.shared.feature.vocabularyEditor.data)
                implementation(projects.shared.feature.vocabularyEditor.presentation.api)
                implementation(projects.shared.feature.vocabularyEditor.presentation.navigationApi)
                implementation(projects.shared.feature.vocabularyEditor.presentation.impl)
                implementation(projects.shared.feature.library.data)
                implementation(projects.shared.feature.library.presentation.api)
                implementation(projects.shared.feature.library.presentation.navigationApi)
                implementation(projects.shared.feature.library.presentation.impl)
                implementation(projects.shared.feature.profile.presentation.api)
                implementation(projects.shared.feature.profile.presentation.navigationApi)
                implementation(projects.shared.feature.profile.presentation.impl)
                implementation(projects.shared.feature.startup.domain)
                implementation(projects.shared.feature.startup.presentation.api)
                implementation(projects.shared.feature.startup.presentation.impl)

                implementation(projects.shared.tts.integration)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
