plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.sensee.composeMultiplatform)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
}

android {
    namespace = "app.sensee"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = "app.sensee"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = providers.gradleProperty("sensee.versionCode").get().toInt()
        versionName = providers.gradleProperty("sensee.version").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        getByName("debug") {
            // Committed public debug keystore: stable APK signature across CI
            // builds so reviewers can update in place. Debug creds are the
            // well-known Android defaults — intentionally non-secret.
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            manifestPlaceholders["firebaseCrashlyticsCollectionEnabled"] = false
            manifestPlaceholders["firebaseAnalyticsCollectionEnabled"] = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["firebaseCrashlyticsCollectionEnabled"] = true
            manifestPlaceholders["firebaseAnalyticsCollectionEnabled"] = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("review") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(projects.shared.appShell)
    implementation(projects.shared.core.decompose)
    implementation(projects.shared.core.platform)
    implementation(projects.shared.ui.designSystem)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    debugImplementation(libs.compose.uiTooling)
}
