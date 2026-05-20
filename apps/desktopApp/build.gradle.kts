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
            packageName = "app.sensee"
            packageVersion = providers.gradleProperty("sensee.version").get()
        }
    }
}
