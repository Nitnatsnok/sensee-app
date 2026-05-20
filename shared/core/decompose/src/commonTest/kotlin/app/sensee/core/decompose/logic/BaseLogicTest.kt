package app.sensee.core.decompose.logic

import app.sensee.core.testKit.ImmediateMainDispatcher
import app.sensee.core.testKit.immediateAppDispatchers
import app.sensee.core.testKit.noOpAppDiagnostics
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertSame

class BaseLogicTest {
    @Test
    fun `logic scope uses the main-immediate dispatcher from AppDispatchers`() {
        val dispatcher = ImmediateMainDispatcher()
        val logic = TestLogic(dispatcher)

        assertSame(
            expected = dispatcher,
            actual = logic.scopeDispatcher(),
        )

        logic.onDestroy()
    }

    private class TestLogic(
        dispatcher: MainCoroutineDispatcher,
    ) : BaseLogic(immediateAppDispatchers(dispatcher), noOpAppDiagnostics()) {
        fun scopeDispatcher(): CoroutineDispatcher? =
            logicScope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
    }
}
