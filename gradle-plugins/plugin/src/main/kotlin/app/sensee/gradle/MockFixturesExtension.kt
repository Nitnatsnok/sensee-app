package app.sensee.gradle

import org.gradle.api.provider.Property

/**
 * Configures the generated `MockFixtureSet` for a module:
 *  - [packageName] / [className] place the generated class.
 *  - [classKdoc] is optional documentation copied into a `/** … */` block above
 *    the class — keep the rationale alongside the generated code, since the
 *    original Kotlin source is deleted by the migration.
 *
 * Source JSON lives under `src/commonMain/mockFixtures/` and the relative path
 * (sans `.json`) becomes the mock backend key.
 */
abstract class MockFixturesExtension {
    abstract val packageName: Property<String>
    abstract val className: Property<String>
    abstract val classKdoc: Property<String>
}
