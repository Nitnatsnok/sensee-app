plugins {
    alias(libs.plugins.sensee.quality)
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
}

dependencies {
    testImplementation(libs.quality.konsist)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()

    systemProperty(
        "projectRoot",
        rootProject.layout.projectDirectory.asFile.absolutePath,
    )

    inputs
        .files(
            rootProject.fileTree(rootProject.layout.projectDirectory) {
                include("**/src/**/*.kt")
                include("**/src/**/*.kts")
                include("**/build.gradle.kts")
                include("settings.gradle.kts")

                exclude("**/build/**")
                exclude("**/.gradle/**")
                exclude("**/.idea/**")
                exclude("**/generated/**")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
}
