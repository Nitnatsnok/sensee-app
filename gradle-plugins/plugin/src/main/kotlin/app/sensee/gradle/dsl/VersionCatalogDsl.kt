package app.sensee.gradle.dsl

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.plugin.use.PluginDependency

internal fun Project.requiredVersion(
    alias: String,
    catalogName: String = "libs",
): VersionConstraint = getCatalog(catalogName).requiredVersion(alias)

internal fun Project.requiredLibrary(
    alias: String,
    catalogName: String = "libs",
): Provider<MinimalExternalModuleDependency> = getCatalog(catalogName).requiredLibrary(alias)

internal fun Project.requiredPlugin(
    alias: String,
    catalogName: String = "libs",
): Provider<PluginDependency> = getCatalog(catalogName).requiredPlugin(alias)

internal fun Project.requiredBundle(
    alias: String,
    catalogName: String = "libs",
): Provider<ExternalModuleDependencyBundle> = getCatalog(catalogName).requiredBundle(alias)

internal fun Project.getCatalog(name: String): VersionCatalog =
    extensions.getByType<VersionCatalogsExtension>().named(name)

internal fun VersionCatalog.requiredVersion(alias: String): VersionConstraint =
    findVersion(alias).orElseThrow {
        IllegalArgumentException("Required version catalog alias '$alias' was not found in libs.versions.toml")
    }

internal fun VersionCatalog.requiredLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalArgumentException("Required version catalog alias '$alias' was not found in libs.versions.toml")
    }

internal fun VersionCatalog.requiredPlugin(alias: String): Provider<PluginDependency> =
    findPlugin(alias).orElseThrow {
        IllegalArgumentException("Required version catalog alias '$alias' was not found in libs.versions.toml")
    }

internal fun VersionCatalog.requiredBundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    findBundle(alias).orElseThrow {
        IllegalArgumentException("Required version catalog alias '$alias' was not found in libs.versions.toml")
    }
