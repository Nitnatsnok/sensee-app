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
}

compose.desktop {
    application {
        mainClass = "app.sensee.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Sensee"
            packageVersion = providers.gradleProperty("sensee.version").get()
            // jpackage runtime image strips unused JPMS modules; sqlite-jdbc
            // needs java.sql at runtime via JdbcSqliteDriver → DriverManager.
            modules("java.sql")

            windows {
                shortcut = true
                menuGroup = "Sensee"
                upgradeUuid = "0C8ABF27-4750-4516-B785-270FD06CA3E0"
                perUserInstall = true
            }

            linux {
                packageName = "sensee"
            }

            macOS {
                bundleID = "app.sensee"
            }
        }
    }
}
