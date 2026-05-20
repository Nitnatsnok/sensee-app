rootProject.name = "Sensee"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("gradle-plugins")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // Kotlin JS/Wasm managed toolchain distributions. Keep these exclusive so
        // normal application dependencies cannot resolve from distribution hosts.
        exclusiveContent {
            forRepository {
                ivy {
                    name = "nodeJsDistributions"
                    url = uri("https://nodejs.org/dist")
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeModule("org.nodejs", "node")
            }
        }

        exclusiveContent {
            forRepository {
                ivy {
                    name = "yarnDistributions"
                    url = uri("https://github.com/yarnpkg/yarn/releases/download")
                    patternLayout {
                        artifact("v[revision]/[artifact](-v[revision]).[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeModule("com.yarnpkg", "yarn")
            }
        }

        exclusiveContent {
            forRepository {
                ivy {
                    name = "binaryenDistributions"
                    url = uri("https://github.com/WebAssembly/binaryen/releases/download")
                    patternLayout {
                        artifact("version_[revision]/[artifact]-version_[revision]-[classifier].[ext]")
                    }
                    metadataSources {
                        artifact()
                    }
                }
            }
            filter {
                includeModule("com.github.webassembly", "binaryen")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val desktopAppModule = ":desktopApp"
val webAppModule = ":webApp"
val androidAppModule = ":androidApp"
val appsDirectory = "apps"
// Short task paths (":desktopApp", …) for the runnable apps even though they
// physically live under apps/. iosFramework is intentionally NOT here: it is
// the iOS framework-export module (a leftover of the composeApp split), not a
// runnable app, so it keeps its natural path ":apps:iosFramework" below.
val appModules = listOf(desktopAppModule, webAppModule, androidAppModule)
val appDirectories =
    appModules.associateWith {
        layout.rootDirectory
            .dir(appsDirectory)
            .dir(it.removePrefix(":"))
    }

include(desktopAppModule)
include(webAppModule)
include(androidAppModule)
include(":apps:iosFramework")

appModules.forEach {
    val directory =
        appDirectories[it]?.asFile
            ?: throw GradleException("Directory for module $it does not exist")
    project(it).projectDir = directory
}

include(":quality:detekt-rules")
include(":quality:konsist")
include(":shared:app-shell")
include(":shared:core:presentation")
include(":shared:core:platform")
include(":shared:core:decompose")
include(":shared:core:coroutines")
include(":shared:core:network")
include(":shared:core:mock-backend")
include(":shared:core:observability")
include(":shared:core:secure-storage")
include(":shared:core:tracing")
include(":shared:core:test-kit")
include(":shared:database")
include(":shared:settings:domain")
include(":shared:settings:data")
include(":shared:settings:database-schema")
include(":shared:ui:design-system")
include(":shared:ui:adaptive")
include(":shared:ui:learning-deck")
include(":shared:srs:core")
include(":shared:srs:fsrs")
include(":shared:srs:engine")
include(":shared:srs:test-kit")
include(":shared:srs:fsrs-engine")
include(":shared:ai:core")
include(":shared:ai:fixture")
include(":shared:ai:llm")
include(":shared:ai:integration")
include(":shared:grammar:domain")
include(":shared:grammar:data")
include(":shared:tts:core")
include(":shared:tts:playback")
include(":shared:tts:system")
include(":shared:tts:elevenlabs")
include(":shared:tts:openai")
include(":shared:tts:integration")
include(":shared:tts:test-kit")
include(":shared:tts:database-schema")
include(":shared:tts:cache")
include(":shared:feature:home:presentation:api")
include(":shared:feature:home:presentation:navigation-api")
include(":shared:feature:home:presentation:impl")
include(":shared:feature:profile:presentation:api")
include(":shared:feature:profile:presentation:navigation-api")
include(":shared:feature:profile:presentation:impl")
include(":shared:feature:practice:presentation:api")
include(":shared:feature:practice:domain")
include(":shared:feature:practice:data")
include(":shared:feature:practice:database-schema")
include(":shared:feature:practice:presentation:impl")
include(":shared:feature:startup:presentation:api")
include(":shared:feature:startup:presentation:impl")
include(":shared:feature:library:domain")
include(":shared:feature:library:data")
include(":shared:feature:library:database-schema")
include(":shared:feature:library:presentation:api")
include(":shared:feature:library:presentation:navigation-api")
include(":shared:feature:library:presentation:impl")
include(":shared:feature:vocabulary-editor:domain")
include(":shared:feature:vocabulary-editor:database-schema")
include(":shared:feature:vocabulary-editor:data")
include(":shared:feature:vocabulary-editor:presentation:api")
include(":shared:feature:vocabulary-editor:presentation:navigation-api")
include(":shared:feature:vocabulary-editor:presentation:impl")
include(":shared:feature:practice:presentation:navigation-api")
include(":shared:core:compose")
