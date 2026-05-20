plugins {
    alias(libs.plugins.sensee.kmpLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "app.sensee.feature.vocabularyEditor.databaseSchema"
    }
}

sqldelight {
    databases {
        register("SenseeDatabase") {
            packageName.set("app.sensee.feature.vocabularyEditor.databaseSchema")
            generateAsync.set(true)
        }
    }
}
