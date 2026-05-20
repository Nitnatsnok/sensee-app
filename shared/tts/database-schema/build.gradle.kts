plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "app.sensee.tts.databaseSchema"
    }
}

sqldelight {
    databases {
        register("SenseeDatabase") {
            packageName.set("app.sensee.tts.databaseSchema")
            generateAsync.set(true)
        }
    }
}
