plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.srs.core)
                api(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(projects.shared.srs.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.feature.practice.domain"
    }
}
