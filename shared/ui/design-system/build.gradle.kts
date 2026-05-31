plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.core.compose)
                api(libs.kotlinx.collections.immutable)
                api(libs.compose.ui)
                api(libs.compose.unstyled.primitives)
                api(libs.compose.unstyled.theming)
                api(libs.compose.unstyled.platformTheme)
                api(libs.compose.unstyled.button)
                api(libs.compose.unstyled.toggleSwitch)
                api(libs.compose.unstyled.coloredIndication)
                api(libs.compose.unstyled.icon)
                api(libs.compose.unstyled.textField)
                api(libs.compose.unstyled.modalBottomSheet)
                api(libs.compose.uiToolingPreview)

                implementation(libs.compose.components.resources)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
    android {
        namespace = "app.sensee.ui.designSystem"
        androidResources.enable = true
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
