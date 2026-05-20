plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.sensee.quality)
}

kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    compileOnly(libs.quality.detekt.api)
    compileOnly(libs.quality.detekt.kotlin.analysis.api)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.quality.detekt.test)
    testImplementation(libs.quality.detekt.test.utils)
    testImplementation(libs.kotlin.testJunit)
}
