package app.sensee.core.decompose.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChainedNavigationDispatcherTest {
    private object SomeConfig : ScreenConfig

    private class FakeDispatcher(
        private val openStatus: NavigationRequestStatus,
        private val backStatus: NavigationRequestStatus,
    ) : NavigationDispatcher {
        var openCalls = 0
        var backCalls = 0

        override fun open(
            target: ScreenConfig,
            onComplete: (isSuccess: Boolean) -> Unit,
        ): NavigationRequestStatus {
            openCalls++
            return openStatus
        }

        override fun back(onResult: (NavigationRequestStatus) -> Unit) {
            backCalls++
            onResult(backStatus)
        }
    }

    @Test
    fun `a locally handled open does not escalate to the parent`() {
        val local = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)
        val parent = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)

        val status = ChainedNavigationDispatcher(local, parent).open(SomeConfig)

        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(1, local.openCalls)
        assertEquals(0, parent.openCalls, "parent must not be consulted once local handled it")
    }

    @Test
    fun `an unhandled open escalates to the parent`() {
        val local = FakeDispatcher(NavigationRequestStatus.Unhandled, NavigationRequestStatus.Handled)
        val parent = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)

        val status = ChainedNavigationDispatcher(local, parent).open(SomeConfig)

        assertEquals(NavigationRequestStatus.Handled, status)
        assertEquals(1, parent.openCalls, "local did not handle it, so parent is consulted")
    }

    @Test
    fun `an unhandled open with no parent stays unhandled`() {
        val local = FakeDispatcher(NavigationRequestStatus.Unhandled, NavigationRequestStatus.Unhandled)

        val status = ChainedNavigationDispatcher(local, parent = null).open(SomeConfig)

        assertEquals(NavigationRequestStatus.Unhandled, status)
    }

    @Test
    fun `back is handled locally without falling through to the parent`() {
        val local = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)
        val parent = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)

        var result: NavigationRequestStatus? = null
        ChainedNavigationDispatcher(local, parent).back { result = it }

        assertEquals(NavigationRequestStatus.Handled, result)
        assertEquals(0, parent.backCalls, "parent back must not fire when local handled back")
    }

    @Test
    fun `an unhandled back falls through to the parent`() {
        val local = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Unhandled)
        val parent = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Handled)

        var result: NavigationRequestStatus? = null
        ChainedNavigationDispatcher(local, parent).back { result = it }

        assertEquals(NavigationRequestStatus.Handled, result)
        assertTrue(parent.backCalls == 1, "local could not go back, so parent handles it")
    }

    @Test
    fun `an unhandled back with no parent reports unhandled`() {
        val local = FakeDispatcher(NavigationRequestStatus.Handled, NavigationRequestStatus.Unhandled)

        var result: NavigationRequestStatus? = null
        ChainedNavigationDispatcher(local, parent = null).back { result = it }

        assertEquals(NavigationRequestStatus.Unhandled, result)
        assertFalse(result == NavigationRequestStatus.Handled)
    }
}
