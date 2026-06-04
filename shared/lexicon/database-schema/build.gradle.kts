plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "app.sensee.lexicon.databaseSchema"
    }
}

sqldelight {
    databases {
        register("SenseeDatabase") {
            packageName.set("app.sensee.lexicon.databaseSchema")
            generateAsync.set(true)
        }
    }
}
