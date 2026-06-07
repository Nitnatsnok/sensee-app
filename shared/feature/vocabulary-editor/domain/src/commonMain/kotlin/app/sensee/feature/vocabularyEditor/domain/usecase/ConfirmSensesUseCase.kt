package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Confirms captured senses, then indexes each for similarity. [SenseWriteRepository.confirmAll]
 * makes the durable batch in one transaction; embedding runs *after* it has
 * committed — outside the write transaction, within a bounded eager budget
 * ([EmbeddingPort.EAGER_EMBED_BUDGET_MS]) — and is best-effort: a provider miss,
 * storage error, or budget overrun is dropped, leaving the sense unembedded for a
 * later backfill, and never fails or blocks the confirm. The vector is a soft
 * similar-sense signal, never a save gate.
 */
@Inject
public class ConfirmSensesUseCase(
    private val senseWriteRepository: SenseWriteRepository,
    private val embeddingPort: EmbeddingPort,
    appDiagnostics: AppDiagnostics,
) {
    private val logger = appDiagnostics.logger.tag("ConfirmSenses")

    /** Confirm [senses] as a batch, embed each best-effort within the eager budget, and return the stored rows. */
    public suspend operator fun invoke(senses: List<Sense>): List<StoredSense> {
        val confirmed = senseWriteRepository.confirmAll(senses)
        // The durable batch is already committed; embed within a bounded budget so a
        // slow provider leaves senses for a later backfill instead of blocking confirm.
        // Embed concurrently: a batch is N network calls, and one round of parallel
        // calls fits the budget where a sequential loop would time out on the first
        // cold request and abandon the rest.
        withTimeoutOrNull(EmbeddingPort.EAGER_EMBED_BUDGET_MS) {
            coroutineScope {
                confirmed.forEach { stored ->
                    launch {
                        runCatchingCancellable { embeddingPort.embed(stored) }
                            .onFailure { failure ->
                                logger.warn(failure) {
                                    "Embedding sense ${stored.id} failed; leaving it for a later backfill."
                                }
                            }
                    }
                }
            }
        }
        return confirmed
    }
}
