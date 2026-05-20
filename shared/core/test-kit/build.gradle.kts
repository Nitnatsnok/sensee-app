plugins {
    alias(libs.plugins.sensee.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.shared.core.coroutines)
                api(projects.shared.core.observability)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
    android {
        namespace = "app.sensee.core.testKit"
    }
}
