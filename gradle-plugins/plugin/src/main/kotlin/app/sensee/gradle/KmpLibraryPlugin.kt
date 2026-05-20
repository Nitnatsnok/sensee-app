package app.sensee.gradle

import app.sensee.gradle.dsl.androidKmp
import app.sensee.gradle.dsl.ensurePlugin
import app.sensee.gradle.dsl.kotlinMultiplatform
import app.sensee.gradle.dsl.requiredLibrary
import app.sensee.gradle.dsl.requiredVersion
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

class KmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            ensurePlugin("org.jetbrains.kotlin.multiplatform")
            ensurePlugin("com.android.kotlin.multiplatform.library")
            ensurePlugin("com.android.lint")
            ensurePlugin("app.sensee.gradle.kmp-web-toolchain")
            ensurePlugin("app.sensee.gradle.quality")

            val extension = createSenseeKmpExtension()
            configureKmpLibraryDefaults()
            applyIosTargetsDefaults(extension)
        }
}

// Local inner-loop accelerator. By default every shared module builds all
// targets (Android, JVM, iOS, JS, Wasm). A developer can narrow this for a
// faster edit/compile cycle with e.g. `-Psensee.kmp.targets=jvm` or in their
// ~/.gradle/gradle.properties. The Android target is always configured (it is
// the primary platform and is wired separately). CI and release builds set no
// such property, so they keep the full target set unchanged.
private fun Project.requestedKmpTargets(): Set<String>? =
    providers
        .gradleProperty("sensee.kmp.targets")
        .orNull
        ?.split(',')
        ?.map { it.trim().lowercase() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?.takeIf { it.isNotEmpty() }

private fun Project.configureKmpLibraryDefaults() {
    configureAndroidTarget()
    val requested = requestedKmpTargets()

    fun enabled(target: String) = requested == null || target.lowercase() in requested
    kotlinMultiplatform {
        // Shared library modules expose a deliberate public surface across module
        // boundaries: every public declaration must have an explicit visibility
        // modifier and return type. KGP applies this to production compilations
        // only (test source sets are intentionally exempt). ABI dump validation
        // is a separate opt-in (public-api plugin).
        explicitApi()

        jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())

        if (enabled("ios")) {
            iosArm64()
            iosSimulatorArm64()
        }
        if (enabled("jvm")) {
            jvm()
        }
        if (enabled("js")) {
            js {
                browser()
            }
        }
        if (enabled("wasmjs")) {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }
        }

        sourceSets.getByName("commonTest").dependencies {
            implementation(requiredLibrary("kotlin-test"))
        }

        compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

private fun Project.configureAndroidTarget() {
    androidKmp {
        compileSdk = requiredVersion("android-compileSdk").requiredVersion.toInt()
        minSdk = requiredVersion("android-minSdk").requiredVersion.toInt()

        withHostTestBuilder {
        }
    }
}
