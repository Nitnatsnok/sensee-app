package app.sensee.gradle

import app.sensee.gradle.dsl.androidApplication
import app.sensee.gradle.dsl.androidApplicationComponents
import app.sensee.gradle.dsl.androidCommon
import app.sensee.gradle.dsl.androidKmp
import app.sensee.gradle.dsl.androidKmpLibraryComponents
import app.sensee.gradle.dsl.androidLibrary
import app.sensee.gradle.dsl.androidLibraryComponents
import app.sensee.gradle.dsl.createNestedExtension
import app.sensee.gradle.dsl.kapt
import app.sensee.gradle.dsl.onAndroidFinalizeDsl
import app.sensee.gradle.dsl.onAndroidKmpLibraryFinalizeDsl
import app.sensee.gradle.dsl.onKotlinPluginApplied
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExtensionsDslTest {
    @Test
    fun `androidApplication configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("application", proxy<ApplicationExtension>())

        var called = false
        project.androidApplication {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidLibrary configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("library", proxy<LibraryExtension>())

        var called = false
        project.androidLibrary {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidCommon configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add(
            "android",
            proxy<CommonExtension> { methodName, _ ->
                if (methodName == "getExtensions") project.extensions else null
            },
        )

        var called = false
        project.androidCommon {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidCommon fails when android extension is missing`() {
        val project = ProjectBuilder.builder().build()

        val error =
            assertFailsWith<GradleException> {
                project.androidCommon {}
            }

        assertContains(error.message ?: "", "Extension 'android' was not found")
    }

    @Test
    fun `androidCommon fails when android extension has unsupported type`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("android", Any())

        val error =
            assertFailsWith<GradleException> {
                project.androidCommon {}
            }

        assertContains(error.message ?: "", "unsupported type")
    }

    @Test
    fun `androidKmp configures android target`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.android.kotlin.multiplatform.library")

        var called = false
        project.androidKmp {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidApplicationComponents configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", proxy<ApplicationAndroidComponentsExtension>())

        var called = false
        project.androidApplicationComponents {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidLibraryComponents configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", proxy<LibraryAndroidComponentsExtension>())

        var called = false
        project.androidLibraryComponents {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `androidKmpLibraryComponents configures extension when present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", proxy<KotlinMultiplatformAndroidComponentsExtension>())

        var called = false
        project.androidKmpLibraryComponents {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `createNestedExtension creates child under extension-aware parent`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("parent", ExtensionAwareHolder(project.extensions))

        val child =
            project.createNestedExtension<NestedExtension>(
                parent = "parent",
                name = "nested",
                constructionArguments = arrayOf("ok"),
            )

        assertEquals("ok", child.value)
        assertSame(project.extensions.findByName("nested"), child)
    }

    @Test
    fun `createNestedExtension fails when parent is missing`() {
        val project = ProjectBuilder.builder().build()

        val error =
            assertFailsWith<GradleException> {
                project.createNestedExtension<NestedExtension>(parent = "missing", name = "nested")
            }

        assertContains(error.message ?: "", "Parent extension 'missing' was not found")
    }

    @Test
    fun `createNestedExtension fails when parent is not extension-aware`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("parent", PlainParentExtension())

        val error =
            assertFailsWith<GradleException> {
                project.createNestedExtension<NestedExtension>(parent = "parent", name = "nested")
            }

        assertContains(error.message ?: "", "does not support nested extensions")
    }

    @Test
    fun `createNestedExtension creates child under resolved extension-aware parent`() {
        val project = ProjectBuilder.builder().build()
        val parent = ExtensionAwareHolder(project.extensions)

        val child =
            project.createNestedExtension<NestedExtension>(
                parent = parent,
                name = "nested",
                constructionArguments = arrayOf("ok"),
            )

        assertEquals("ok", child.value)
        assertSame(parent.extensions.findByName("nested"), child)
    }

    @Test
    fun `createKmpIosTargetsExtension creates iosTargets child under kotlin`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        val kotlinExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java) as ExtensionAware

        val sensee = project.createSenseeKmpExtension()

        assertSame(kotlinExtension.extensions.findByName("sensee"), sensee)
        assertEquals(null, project.extensions.findByName("sensee"))
    }

    @Test
    fun `kapt configures extension when plugin is applied`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        project.pluginManager.apply("org.jetbrains.kotlin.kapt")

        var called = false
        project.kapt {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `onKotlinPluginApplied triggers action for kotlin jvm`() {
        assertOnKotlinPluginAppliedCalledExactlyOnce("org.jetbrains.kotlin.jvm")
    }

    @Test
    fun `onKotlinPluginApplied triggers action for kotlin android`() {
        assertOnKotlinPluginAppliedCalledExactlyOnce("org.jetbrains.kotlin.android")
    }

    @Test
    fun `onKotlinPluginApplied triggers action for kotlin multiplatform`() {
        assertOnKotlinPluginAppliedCalledExactlyOnce("org.jetbrains.kotlin.multiplatform")
    }

    @Test
    fun `onKotlinPluginApplied triggers action for kotlin multiplatform library`() {
        assertOnKotlinPluginAppliedCalledExactlyOnce(
            "org.jetbrains.kotlin.multiplatform",
            "com.android.kotlin.multiplatform.library",
        )
    }

    @Test
    fun `onKotlinPluginApplied triggers action when kotlin jvm already applied`() {
        assertOnKotlinPluginAppliedCalledExactlyOnceWhenAlreadyApplied("org.jetbrains.kotlin.jvm")
    }

    @Test
    fun `onKotlinPluginApplied triggers action when kotlin android already applied`() {
        assertOnKotlinPluginAppliedCalledExactlyOnceWhenAlreadyApplied("org.jetbrains.kotlin.android")
    }

    @Test
    fun `onKotlinPluginApplied triggers action when kotlin multiplatform already applied`() {
        assertOnKotlinPluginAppliedCalledExactlyOnceWhenAlreadyApplied("org.jetbrains.kotlin.multiplatform")
    }

    @Test
    fun `onKotlinPluginApplied triggers action when kotlin multiplatform library already applied`() {
        assertOnKotlinPluginAppliedCalledExactlyOnceWhenAlreadyApplied(
            "org.jetbrains.kotlin.multiplatform",
            "com.android.kotlin.multiplatform.library",
        )
    }

    @Test
    fun `onAndroidFinalizeDsl invokes action for application components`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", applicationComponentsProxy())

        var called = false
        project.onAndroidFinalizeDsl {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `onAndroidFinalizeDsl invokes action for library components`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", libraryComponentsProxy())

        var called = false
        project.onAndroidFinalizeDsl {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `onAndroidFinalizeDsl prefers application components when both are present`() {
        val project = ProjectBuilder.builder().build()

        var appFinalizeCalls = 0
        var libFinalizeCalls = 0

        project.extensions.add(
            "applicationAndroidComponents",
            applicationComponentsProxy {
                appFinalizeCalls += 1
            },
        )
        project.extensions.add(
            "libraryAndroidComponents",
            libraryComponentsProxy {
                libFinalizeCalls += 1
            },
        )

        project.onAndroidFinalizeDsl {}

        assertEquals(1, appFinalizeCalls)
        assertEquals(0, libFinalizeCalls)
    }

    @Test
    fun `onAndroidFinalizeDsl is no-op when Android Components extension is absent`() {
        val project = ProjectBuilder.builder().build()

        project.onAndroidFinalizeDsl {
            error("Action should not be invoked without Android Components extension")
        }
    }

    @Test
    fun `onAndroidKmpLibraryFinalizeDsl invokes action when components extension is present`() {
        val project = ProjectBuilder.builder().build()
        project.extensions.add("androidComponents", kmpLibraryComponentsProxy())

        var called = false
        project.onAndroidKmpLibraryFinalizeDsl {
            called = true
        }

        assertTrue(called)
    }

    @Test
    fun `onAndroidKmpLibraryFinalizeDsl is no-op when components extension is absent`() {
        val project = ProjectBuilder.builder().build()

        project.onAndroidKmpLibraryFinalizeDsl {
            error("Action should not be invoked without KMP Android Components extension")
        }
    }

    private class ExtensionAwareHolder(
        private val extensionContainer: ExtensionContainer,
    ) : ExtensionAware {
        override fun getExtensions(): ExtensionContainer = extensionContainer
    }

    private class PlainParentExtension

    open class NestedExtension(
        val value: String,
    )

    private fun assertOnKotlinPluginAppliedCalledExactlyOnce(vararg pluginIds: String) {
        val project = ProjectBuilder.builder().build()

        var callCount = 0
        project.onKotlinPluginApplied {
            callCount += 1
        }

        pluginIds.forEach(project.pluginManager::apply)

        assertEquals(1, callCount)
    }

    private fun assertOnKotlinPluginAppliedCalledExactlyOnceWhenAlreadyApplied(vararg pluginIds: String) {
        val project = ProjectBuilder.builder().build()
        pluginIds.forEach(project.pluginManager::apply)

        var callCount = 0
        project.onKotlinPluginApplied {
            callCount += 1
        }

        assertEquals(1, callCount)
    }

    private fun applicationComponentsProxy(onFinalizeDsl: () -> Unit = {}): ApplicationAndroidComponentsExtension =
        proxy { methodName, args ->
            if (methodName == "finalizeDsl") {
                onFinalizeDsl()
                invokeCallback(args.firstOrNull(), proxy<ApplicationExtension>())
            }
            null
        }

    private fun libraryComponentsProxy(onFinalizeDsl: () -> Unit = {}): LibraryAndroidComponentsExtension =
        proxy { methodName, args ->
            if (methodName == "finalizeDsl") {
                onFinalizeDsl()
                invokeCallback(args.firstOrNull(), proxy<LibraryExtension>())
            }
            null
        }

    private fun kmpLibraryComponentsProxy(
        onFinalizeDsl: () -> Unit = {},
    ): KotlinMultiplatformAndroidComponentsExtension =
        proxy { methodName, args ->
            if (methodName == "finalizeDsl") {
                onFinalizeDsl()
                invokeCallback(args.firstOrNull(), proxy<KotlinMultiplatformAndroidLibraryExtension>())
            }
            null
        }

    @Suppress("UNCHECKED_CAST")
    private fun invokeCallback(
        callback: Any?,
        value: Any,
    ) {
        when (callback) {
            is Action<*> -> (callback as Action<Any>).execute(value)
            is Function1<*, *> -> (callback as (Any) -> Any?).invoke(value)
            else -> error("Unsupported callback type: ${callback?.javaClass?.name}")
        }
    }

    /**
     * Reflection-based test double for any interface [T]. The [handler] is invoked for every
     * non-Object method on the proxy with the called method's name and its arguments, and may
     * return any value (or `null` to fall back to a type-appropriate default — see
     * [defaultValue]). `toString`/`hashCode`/`equals` are handled internally so the proxy
     * behaves sanely in collections and assertions.
     */
    private inline fun <reified T : Any> proxy(
        crossinline handler: (methodName: String, args: Array<out Any?>) -> Any? = { _, _ -> null },
    ): T =
        Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { self, method, args ->
            when (method.name) {
                "toString" -> "${T::class.java.simpleName}Proxy"
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args?.firstOrNull()
                else -> handler(method.name, args ?: emptyArray()) ?: defaultValue(method.returnType)
            }
        } as T

    private fun defaultValue(returnType: Class<*>): Any? =
        when (returnType) {
            Boolean::class.javaPrimitiveType -> false
            Char::class.javaPrimitiveType -> 0.toChar()
            Byte::class.javaPrimitiveType -> 0.toByte()
            Short::class.javaPrimitiveType -> 0.toShort()
            Int::class.javaPrimitiveType -> 0
            Long::class.javaPrimitiveType -> 0L
            Float::class.javaPrimitiveType -> 0f
            Double::class.javaPrimitiveType -> 0.0
            Void.TYPE -> null
            else -> null
        }
}
