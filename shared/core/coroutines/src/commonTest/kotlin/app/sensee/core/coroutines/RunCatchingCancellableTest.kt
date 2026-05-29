package app.sensee.core.coroutines

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RunCatchingCancellableTest {
    @Test
    fun `returns success when block completes`() {
        val result = runCatchingCancellable { "ok" }

        assertEquals("ok", result.getOrThrow())
    }

    @Test
    fun `captures non cancellation failure`() {
        val failure = IllegalStateException("boom")

        val result = runCatchingCancellable { throw failure }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `rethrows cancellation failure`() {
        val cancellation = CancellationException("cancelled")

        val thrown =
            assertFailsWith<CancellationException> {
                runCatchingCancellable { throw cancellation }
            }

        assertSame(cancellation, thrown)
    }
}
