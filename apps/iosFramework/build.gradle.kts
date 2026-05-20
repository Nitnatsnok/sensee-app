plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SenseeKit"
            // Static (matches the JetBrains wizard, proven to link Compose
            // Multiplatform at these versions: Kotlin/Native never ld-links a
            // static framework, so Compose's UIKit symbols resolve at the Xcode
            // app link at the app's deployment target — going dynamic instead
            // ld-links here at Kotlin/Native's default min-iOS and breaks on
            // newer Compose UIKit symbols). The one consumer-side cost is
            // linking system sqlite3 for SQLDelight: apps/iosApp adds
            // OTHER_LDFLAGS = -lsqlite3.
            isStatic = true
            // IosRootHolder lives in :shared:app-shell; MainViewController's
            // public signature references it. A KMP framework only exposes its
            // own API plus api-deps that are explicitly exported, so app-shell
            // must be api + export or the Swift host cannot see IosRootHolder.
            export(projects.shared.appShell)
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.appShell)
            implementation(projects.shared.core.decompose)
            implementation(projects.shared.core.platform)

            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
