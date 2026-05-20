import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    base
    alias(libs.plugins.sensee.quality)
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.dependencyAnalysis)
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.versionCatalogUpdate)
}

// Reproducible version-currency audit. Always run with `--interactive`
// (`./gradlew versionCatalogUpdate --interactive`): that stages proposed bumps
// in libs.versions.updates.toml for review instead of rewriting the catalog in
// place. Many catalog entries are consumed only by the gradle-plugins included
// build (classpath libraries) and are invisible to this build, so a non-staged
// run could wrongly prune live entries. keepUnusedVersions additionally protects
// version keys that have no library/plugin reference here.
versionCatalogUpdate {
    sortByKey = false
    keep {
        keepUnusedVersions = true
    }
}

// Only stable releases are proposed as updates; pre-release pins (Compose
// material3/adaptive, detekt 2) are deliberate and must not be auto-bumped to
// the next alpha.
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        val stable =
            listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) } ||
                "^[0-9,.v-]+(-r)?$".toRegex().matches(candidate.version)
        !stable
    }
}

val composeStabilityRefreshRequested =
    gradle.startParameter.taskNames.any { taskName ->
        taskName == "composeStabilityReport" ||
            taskName.endsWith(":composeStabilityReport") ||
            taskName == "collectComposeStabilityReports" ||
            taskName.endsWith(":collectComposeStabilityReports")
    }

tasks.register("konsistCheck") {
    group = "verification"
    description = "Runs Konsist architecture checks."

    dependsOn(":quality:konsist:test")
}

tasks.named("check") {
    dependsOn("konsistCheck")
}

// Dependency-analysis noise suppression. Three classes of false positives on this stack:
//  - Metro is a compiler-plugin DI: it generates graph code into every module, so its
//    runtime appears in the generated ABI and DAGP wrongly wants it on `api`. It must
//    stay `implementation` — an `api` Metro runtime would leak DI into all consumers.
//  - KMP multi-target + the kmp-library convention inject artifacts the module author
//    never declared (JS dom-api-compat, Compose desktop/hot-reload variants, the
//    commonTest kotlin-test); flagging those "unused" per module is inherent noise.
//  - Umbrella multiplatform libraries (Compose, Decompose, Compose Unstyled) explode
//    into per-platform/internal artifacts. Declaring the umbrella is correct, so these
//    are modelled as DAGP bundles instead of demanding the granular children directly.
dependencyAnalysis {
    structure {
        bundle("compose-multiplatform") {
            includeGroup("org.jetbrains.compose")
            include("^org\\.jetbrains\\.compose\\..*")
            include("^androidx\\.compose\\..*")
        }
        bundle("decompose") {
            primary("com.arkivanov.decompose:decompose")
            includeGroup("com.arkivanov.decompose")
            includeGroup("com.arkivanov.essenty")
        }
        bundle("compose-unstyled") {
            includeGroup("com.composables")
        }
    }
    issues {
        all {
            onIncorrectConfiguration {
                exclude("dev.zacsweers.metro:runtime")
            }
            onUnusedDependencies {
                exclude(
                    "dev.zacsweers.metro:runtime",
                    "org.jetbrains.kotlin:kotlin-test",
                    "org.jetbrains.kotlin:kotlin-dom-api-compat",
                    "org.jetbrains.compose.desktop:desktop-jvm-windows-x64",
                    "org.jetbrains.compose.hot-reload:hot-reload-runtime-api",
                    // androidDeviceTest runtime injected into every KMP android module
                    // by the kmp-library convention; the module author never declared it.
                    "androidx.test.ext:junit",
                    "androidx.test:runner",
                    "androidx.test:core",
                    // Service-loaded at runtime, never referenced from code: the Ktor
                    // JS engine and the SQLDelight web-worker driver are wired by the
                    // platform on web targets, so ABI analysis always sees them unused.
                    "io.ktor:ktor-client-js",
                    "app.cash.sqldelight:web-worker-driver",
                    // JUnit 5 is the test execution platform for the Konsist module;
                    // its API is invoked reflectively, not via compiled references.
                    "org.junit.jupiter:junit-jupiter",
                )
            }
            onUsedTransitiveDependencies {
                exclude(
                    "dev.zacsweers.metro:runtime",
                    // Internal transitives of declared umbrella artifacts: Ktor client
                    // core, SQLDelight runtime, the coroutines core platform variant,
                    // and Kermit. Declaring the umbrella is the correct surface.
                    "io.ktor:ktor-http",
                    "io.ktor:ktor-utils",
                    "io.ktor:ktor-io",
                    "io.ktor:ktor-events",
                    "io.ktor:ktor-serialization",
                    "io.ktor:ktor-network",
                    "io.ktor:ktor-network-tls",
                    "app.cash.sqldelight:async-extensions",
                    "org.jetbrains.kotlinx:kotlinx-coroutines-android",
                    "co.touchlab:kermit-core",
                    "androidx.test:runner",
                )
            }
        }
    }
}

val collectComposeStabilityReports by tasks.registering(Sync::class) {
    group = "verification"
    description = "Collects Compose compiler reports and metrics from all Compose modules."
    into(layout.buildDirectory.dir("reports/compose_compiler"))

    if (composeStabilityRefreshRequested) {
        doNotTrackState("Compose stability report aggregation must always refresh when report tasks are requested.")
    }
}

tasks.register("composeStabilityReport") {
    group = "verification"
    description = "Runs Compose compiler stability reports for all Compose modules. " +
        "Collects reports and metrics in the root build directory and refreshes them even when compilation outputs are otherwise up to date."
    dependsOn(collectComposeStabilityReports)
}

plugins.withType(NodeJsPlugin::class.java) {
    extensions.configure<NodeJsEnvSpec>(NodeJsEnvSpec.EXTENSION_NAME) {
        downloadBaseUrl.set(null as String?)
    }
}

plugins.withType(WasmNodeJsPlugin::class.java) {
    extensions.configure<WasmNodeJsEnvSpec>(WasmNodeJsEnvSpec.EXTENSION_NAME) {
        downloadBaseUrl.set(null as String?)
    }
}

plugins.withType(YarnPlugin::class.java) {
    extensions.configure<YarnRootEnvSpec>(YarnRootEnvSpec.YARN) {
        downloadBaseUrl.set(null as String?)
    }
}

plugins.withType(WasmYarnPlugin::class.java) {
    extensions.configure<WasmYarnRootEnvSpec>(WasmYarnRootEnvSpec.YARN) {
        downloadBaseUrl.set(null as String?)
    }
}
