package app.sensee.gradle.dsl

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KaptExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper

/** Configure the Android application DSL. */
internal fun Project.androidApplication(action: ApplicationExtension.() -> Unit) = extensions.configure(action)

/** Configure the Android library DSL. */
internal fun Project.androidLibrary(action: LibraryExtension.() -> Unit) = extensions.configure(action)

/** Configure the generic Android `android {}` extension when present. */
internal fun Project.androidCommon(action: CommonExtension.() -> Unit) {
    val rawExtension =
        extensions.findByName("android")
            ?: throw GradleException("Extension 'android' was not found.")
    val extension =
        rawExtension as? CommonExtension
            ?: throw GradleException("Extension 'android' has an unsupported type: ${rawExtension::class.java.name}.")
    extension.configure(action)
}

/** Configure the KMP Android library target DSL. */
internal fun Project.androidKmp(action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) =
    extensions
        .getByType<KotlinMultiplatformExtension>()
        .extensions
        .configure<KotlinMultiplatformAndroidLibraryTarget>(action)

/** Configure the Kotlin multiplatform DSL. */
internal fun Project.kotlinMultiplatform(action: KotlinMultiplatformExtension.() -> Unit) = extensions.configure(action)

/** Configure Android application components (`androidComponents {}`). */
internal fun Project.androidApplicationComponents(action: ApplicationAndroidComponentsExtension.() -> Unit) =
    extensions.configure(action)

/** Configure Android library components (`androidComponents {}`). */
internal fun Project.androidLibraryComponents(action: LibraryAndroidComponentsExtension.() -> Unit) =
    extensions.configure(action)

/** Configure Android KMP library components (`androidComponents {}`). */
internal fun Project.androidKmpLibraryComponents(action: KotlinMultiplatformAndroidComponentsExtension.() -> Unit) =
    extensions.configure(action)

/** Configure kapt extension options. */
internal fun Project.kapt(action: KaptExtension.() -> Unit) = extensions.configure(action)

/**
 * Run [action] after the Android DSL is finalized (via `finalizeDsl`) in Android modules.
 */
internal inline fun Project.onAndroidFinalizeDsl(crossinline action: (CommonExtension) -> Unit) {
    val componentExtension =
        extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
            ?: extensions.findByType(LibraryAndroidComponentsExtension::class.java)
    componentExtension?.finalizeDsl {
        action(it)
    }
}

/**
 * Run [action] after the Android KMP library DSL is finalized (via `finalizeDsl`).
 */
internal inline fun Project.onAndroidKmpLibraryFinalizeDsl(
    crossinline action: (KotlinMultiplatformAndroidLibraryExtension) -> Unit,
) {
    val componentExtension =
        extensions.findByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
    componentExtension?.finalizeDsl {
        action(it)
    }
}

/** Run [action] when the Kotlin plugin is applied. */
internal inline fun Project.onKotlinPluginApplied(crossinline action: () -> Unit) {
    whenPlugin<KotlinBasePlugin> {
        action()
    }
}

/**
 * Create a nested extension named [name] under parent extension [parent].
 */
internal inline fun <reified T : Any> Project.createNestedExtension(
    parent: String,
    name: String,
    constructionArguments: Array<out Any> = emptyArray(),
): T {
    val parentExtension =
        extensions.findByName(parent)
            ?: throw GradleException("Parent extension '$parent' was not found.")
    val extensionAware =
        parentExtension as? ExtensionAware
            ?: throw GradleException("Parent extension '$parent' does not support nested extensions.")
    return extensionAware.extensions.create<T>(name, *constructionArguments)
}

/**
 * Create a nested extension named [name] under already resolved [parent].
 */
internal inline fun <reified T : Any> Project.createNestedExtension(
    parent: ExtensionAware,
    name: String,
    constructionArguments: Array<out Any> = emptyArray(),
): T = parent.extensions.create<T>(name, *constructionArguments)
