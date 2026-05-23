import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.sensee.composeMultiplatform)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
}

dependencies {
    implementation(projects.shared.appShell)
    implementation(projects.shared.core.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(projects.shared.core.platform)

    implementation(libs.compose.ui)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)

    // Win32 calls for the custom window chrome (native min/max animations).
    // No-op at runtime on macOS/Linux — see WindowsWindowDecoration.
    implementation(libs.jna)
    implementation(libs.jna.platform)
}

compose.desktop {
    application {
        mainClass = "app.sensee.MainKt"

        // ProGuard runs for release packaging; JNA resolves Win32 bindings
        // reflectively, so its classes must survive shrinking.
        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sensee"
            packageVersion = providers.gradleProperty("sensee.version").get()
            // jpackage runtime image strips unused JPMS modules; sqlite-jdbc
            // needs java.sql at runtime via JdbcSqliteDriver → DriverManager.
            modules("java.sql")

            description = "Language learning desktop app"
            vendor = "Sensee"
            copyright = "© 2026 Sensee"

            windows {
                shortcut = true
                menuGroup = "Sensee"
                upgradeUuid = "0C8ABF27-4750-4516-B785-270FD06CA3E0"
                perUserInstall = true
                iconFile.set(project.file("icons/icon.ico"))
            }

            linux {
                packageName = "sensee"
                menuGroup = "Education"
                appCategory = "education"
                iconFile.set(project.file("icons/icon.png"))
            }

            macOS {
                bundleID = "app.sensee"
                dockName = "Sensee"
                appCategory = "education"
                iconFile.set(project.file("icons/icon.icns"))
            }
        }
    }
}
