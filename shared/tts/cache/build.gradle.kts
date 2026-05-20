plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.tts.core)
                implementation(projects.shared.database)
                implementation(projects.shared.core.observability)
                implementation(projects.shared.core.coroutines)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(projects.shared.tts.testKit)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.sqldelight.driver.sqlite)
                implementation(projects.shared.core.testKit)
            }
        }
    }
    android {
        namespace = "app.sensee.tts.cache"
    }
}
