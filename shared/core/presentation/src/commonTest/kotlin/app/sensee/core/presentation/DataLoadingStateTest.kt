package app.sensee.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataLoadingStateTest {
    @Test
    fun `merge returns idle for empty input`() {
        assertEquals(DataLoadingState.Idle, merge())
    }

    @Test
    fun `merge prefers loading over idle`() {
        assertEquals(
            DataLoadingState.Loading,
            merge(DataLoadingState.Idle, DataLoadingState.Loading),
        )
    }

    @Test
    fun `merge prefers idle over success`() {
        assertEquals(
            DataLoadingState.Idle,
            merge(DataLoadingState.Success, DataLoadingState.Idle),
        )
    }

    @Test
    fun `merge prefers first error`() {
        val firstError = DataLoadingState.Error(IllegalStateException("first"))
        val secondError = DataLoadingState.Error(IllegalArgumentException("second"))

        assertEquals(
            firstError,
            merge(firstError, DataLoadingState.Loading, secondError),
        )
    }

    @Test
    fun `isIdle returns true only for idle`() {
        assertTrue(DataLoadingState.Idle.isIdle())
        assertFalse(DataLoadingState.Loading.isIdle())
    }
}
