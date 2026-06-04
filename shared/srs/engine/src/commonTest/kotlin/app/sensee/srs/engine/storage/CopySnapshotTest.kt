package app.sensee.srs.engine.storage

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.testKit.InMemorySrsStorage
import app.sensee.srs.testKit.SrsTestCards
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CopySnapshotTest {
    private fun store() = InMemorySrsStorage(initialParameters = FsrsParameters.defaultV6())

    @Test
    fun `copySnapshot clones onto the new id and leaves the source untouched`() =
        runTest {
            val store = store()
            store.saveCard(SrsTestCards.newCard("from").copy(reviewCount = 5))

            store.copySnapshot(SrsCardId("from"), SrsCardId("to"))

            val copied = store.getCard(SrsCardId("to"))
            assertEquals(SrsCardId("to"), copied?.id)
            assertEquals(5, copied?.reviewCount)
            assertEquals(5, store.getCard(SrsCardId("from"))?.reviewCount, "the source snapshot is untouched")
        }

    @Test
    fun `copySnapshot is a no-op when the source has no snapshot`() =
        runTest {
            val store = store()

            store.copySnapshot(SrsCardId("missing"), SrsCardId("to"))

            assertNull(store.getCard(SrsCardId("to")))
        }
}
