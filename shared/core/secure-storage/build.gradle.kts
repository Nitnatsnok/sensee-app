plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                implementation(projects.shared.core.coroutines)
                implementation(projects.shared.core.platform)
                implementation(libs.metro.runtime)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.java.keyring)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
    android {
        namespace = "app.sensee.core.secureStorage"
    }
}
