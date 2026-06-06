package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.Sense
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.StoredSense
import dev.zacsweers.metro.Inject

/**
 * Confirms captured senses, then indexes each for similarity. [SenseWriteRepository.confirmAll]
 * makes the durable batch in one transaction; embedding runs *after* it has
 * committed — outside the write transaction — and is best-effort: a provider miss
 * or a storage error is logged and dropped, leaving the sense unembedded for a
 * later backfill, and never fails the confirm. The vector is a soft similar-sense
 * signal, never a save gate.
 */
@Inject
public class ConfirmSensesUseCase(
    private val senseWriteRepository: SenseWriteRepository,
    private val embeddingPort: EmbeddingPort,
    appDiagnostics: AppDiagnostics,
) {
    private val logger = appDiagnostics.logger.tag("ConfirmSenses")

    /** Confirm [senses] as a batch, embed each best-effort, and return the stored rows. */
    public suspend operator fun invoke(senses: List<Sense>): List<StoredSense> {
        val confirmed = senseWriteRepository.confirmAll(senses)
        confirmed.forEach { stored ->
            runCatchingCancellable { embeddingPort.embed(stored) }
                .onFailure { failure ->
                    logger.warn(failure) { "Embedding sense ${stored.id} failed; leaving it for a later backfill." }
                }
        }
        return confirmed
    }
}
