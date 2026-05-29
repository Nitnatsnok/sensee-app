plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "app.sensee.verification.databaseSchema"
    }
}

sqldelight {
    databases {
        register("SenseeDatabase") {
            packageName.set("app.sensee.verification.databaseSchema")
            generateAsync.set(true)
        }
    }
}
