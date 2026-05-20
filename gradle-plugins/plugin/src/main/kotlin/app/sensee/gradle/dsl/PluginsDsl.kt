package app.sensee.gradle.dsl

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Runs [action] on this project once a plugin with the given [id] is applied.
 */
inline fun Project.whenPlugin(
    id: String,
    crossinline action: Project.() -> Unit,
) {
    plugins.withId(id) { action(this@whenPlugin) }
}

/**
 * Runs [action] on this project once a plugin of type [T] is applied.
 */
inline fun <reified T : Plugin<*>> Project.whenPlugin(crossinline action: Project.() -> Unit) {
    plugins.withType(T::class.java).all {
        action(this@whenPlugin)
    }
}

/**
 * Applies the plugin with the given [id] unless it is already applied.
 */
fun Project.ensurePlugin(id: String) {
    if (!plugins.hasPlugin(id)) {
        pluginManager.apply(id)
    }
}
