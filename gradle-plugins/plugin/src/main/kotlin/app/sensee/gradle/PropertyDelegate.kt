package app.sensee.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Write-only property delegate backed by a Gradle [Property]. Reading throws: build logic must
 * read the value through the [Property] itself.
 */
class PropertyDelegate<T : Any>(
    private val property: Property<T>,
) : ReadWriteProperty<Any?, T> {
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): T = throw IllegalStateException("Read DSL value via Property<T> in build logic; delegate is for scripts only")

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T,
    ) {
        this.property.set(value)
    }

    fun property(): Property<T> = property
}

/**
 * Write-only property delegate backed by a Gradle [ListProperty]. Reading throws: build logic
 * must read the value through the [ListProperty] itself.
 */
class ListPropertyDelegate<T : Any>(
    private val property: ListProperty<T>,
) : ReadWriteProperty<Any?, List<T>> {
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ): List<T> =
        throw IllegalStateException("Read DSL value via ListProperty<T> in build logic; delegate is for scripts only")

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: List<T>,
    ) {
        this.property.set(value)
    }

    fun property(): ListProperty<T> = property
}

inline fun <reified T : Any> ObjectFactory.propertyDelegate(): PropertyDelegate<T> {
    val property: Property<T> = property()
    return PropertyDelegate(property)
}

/**
 * Creates a [PropertyDelegate] backed by a [Property] with the given [default] value.
 */
inline fun <reified T : Any> ObjectFactory.propertyDelegate(default: T): PropertyDelegate<T> {
    val property: Property<T> = property(default)
    return PropertyDelegate(property)
}

inline fun <reified T : Any> ObjectFactory.listPropertyDelegate(): ListPropertyDelegate<T> {
    val property: ListProperty<T> = listProperty<T>()
    return ListPropertyDelegate(property)
}

/**
 * Creates a [ListPropertyDelegate] backed by a [ListProperty] with the given [default] convention.
 */
inline fun <reified T : Any> ObjectFactory.listPropertyDelegate(default: List<T>): ListPropertyDelegate<T> {
    val property: ListProperty<T> = listProperty<T>().convention(default)
    return ListPropertyDelegate(property)
}

/**
 * Creates a [Property] with the given [default] value as its convention.
 */
inline fun <reified T : Any> ObjectFactory.property(default: T): Property<T> = property<T>().convention(default)
