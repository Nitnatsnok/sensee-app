package app.sensee.quality.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FailureHandlingRulesTest {
    private val environment = createEnvironment()

    @Test
    fun `runCatching call is reported`() {
        val findings =
            ForbiddenRunCatching(Config.empty).lintWithContext(
                environment,
                """
                fun load(): String =
                    runCatching { "ok" }.getOrDefault("fallback")
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
        assertTrue(findings.single().message.contains("Use explicit try/catch"))
    }

    @Test
    fun `runCatching alias import is reported`() {
        val findings =
            ForbiddenRunCatching(Config.empty).lintWithContext(
                environment,
                """
                import kotlin.runCatching as catching

                fun load(): String =
                    catching { "ok" }.getOrDefault("fallback")
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `similarly named helper is not reported`() {
        val findings =
            ForbiddenRunCatching(Config.empty).lintWithContext(
                environment,
                """
                fun <T> runCatchingCancellable(block: () -> T): T = block()

                fun load(): String =
                    runCatchingCancellable { "ok" }
                """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `local runCatching helper is not reported`() {
        val findings =
            ForbiddenRunCatching(Config.empty).lintWithContext(
                environment,
                """
                fun <T> runCatching(block: () -> T): T = block()

                fun load(): String =
                    runCatching { "ok" }
                """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `broad catch inside suspend function is reported without cancellation guard`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                suspend fun load(): String =
                    try {
                        remote()
                    } catch (failure: Throwable) {
                        "fallback"
                    }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `broad catch inside coroutine builder lambda is reported without cancellation guard`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                import kotlinx.coroutines.CoroutineScope
                import kotlinx.coroutines.launch

                fun launchLoad(scope: CoroutineScope) {
                    scope.launch {
                        try {
                            remote()
                        } catch (failure: Exception) {
                            println(failure)
                        }
                    }
                }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `broad catch inside flow builder lambda is reported without cancellation guard`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                import kotlinx.coroutines.flow.flow

                fun values() =
                    flow {
                        try {
                            emit(remote())
                        } catch (failure: Throwable) {
                            emit("fallback")
                        }
                    }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `broad catch inside expected suspend lambda is reported without cancellation guard`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                fun launchLoad(block: suspend () -> Unit) {
                }

                fun startLoad() {
                    launchLoad {
                        try {
                            remote()
                        } catch (failure: Exception) {
                            println(failure)
                        }
                    }
                }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(1, findings.size)
    }

    @Test
    fun `broad catch preceded by cancellation catch is accepted`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                import kotlinx.coroutines.CancellationException

                suspend fun load(): String =
                    try {
                        remote()
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (failure: Throwable) {
                        "fallback"
                    }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `broad catch rethrowing caught value is accepted`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                suspend fun load(): String =
                    try {
                        remote()
                    } catch (failure: Throwable) {
                        throw failure
                    }

                suspend fun remote(): String = "ok"
                """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }

    @Test
    fun `broad catch inside plain non suspend lambda is ignored`() {
        val findings =
            BroadCatchCancellationGuard(Config.empty).lintWithContext(
                environment,
                """
                fun load(values: List<String>) {
                    values.forEach {
                        try {
                            println(it)
                        } catch (failure: Exception) {
                            println(failure)
                        }
                    }
                }
                """.trimIndent(),
            )

        assertEquals(0, findings.size)
    }
}
