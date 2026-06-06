package app.sensee.feature.library.data

import app.sensee.core.coroutines.runCatchingCancellable
import app.sensee.core.observability.diagnostics.AppDiagnostics
import app.sensee.database.DatabaseTransactionRunner
import app.sensee.feature.library.domain.CardId
import app.sensee.feature.library.domain.ClaimRepository
import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.SenseOrigin
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseStatus
import app.sensee.lexicon.domain.SenseWriteRepository
import app.sensee.lexicon.domain.WriteIntent
import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * The `claim` orchestrator (ADR-002). It spans three owners — a lexicon content
 * copy, an SRS clone, and (eventually) deck membership — so it lives in
 * library/data, NOT as a lexicon-write method (`lexicon` has no SRS/feature deps).
 * It composes `upsert(ForceMint)` + `copySnapshot` in ONE transaction over the
 * neutral [DatabaseTransactionRunner], so a failed clone rolls back the content
 * copy. Always [WriteIntent.ForceMint]: a detached id, so cloning SRS onto a fresh
 * sense_id can never clobber an existing Personal row's progress.
 *
 * The claimed Personal sense needs no deck membership in the core — the captured
 * deck surfaces it; claiming into a named Personal deck is editing-UX (EB-9).
 *
 * Claiming takes ownership, so the copy is embedded best-effort after the
 * transaction commits (ADR-008: embedding follows ownership — a subscribed Service
 * mirror is not embedded). A provider miss never fails the claim.
 */
@SingleIn(AppScope::class)
@ContributesBinding(
    scope = AppScope::class,
    binding = binding<ClaimRepository>(),
)
@Inject
public class DefaultClaimRepository(
    private val senseReadRepository: SenseReadRepository,
    private val senseWriteRepository: SenseWriteRepository,
    private val embeddingPort: EmbeddingPort,
    private val srsStorage: SrsStorage<FsrsParameters>,
    private val transactionRunner: DatabaseTransactionRunner,
    appDiagnostics: AppDiagnostics,
) : ClaimRepository {
    private val logger = appDiagnostics.logger.tag("Claim")

    override suspend fun claim(cardId: CardId): CardId {
        val copy =
            transactionRunner.transaction {
                val sourceId = SenseCatalogProjection.senseIdOf(cardId)
                val source =
                    requireNotNull(senseReadRepository.getById(sourceId)) { "Claim source $sourceId not found" }
                val copy =
                    senseWriteRepository.upsert(
                        sense = source.sense,
                        status = SenseStatus.Confirmed,
                        origin = SenseOrigin.Personal,
                        intent = WriteIntent.ForceMint,
                    )
                // Clone SRS for the base sense and every form card onto the new sense_id,
                // so review progress carries over to the detached copy.
                SenseCatalogProjection.cardsFor(source).forEach { card ->
                    val target = copy.id.value + card.id.value.removePrefix(source.id.value)
                    srsStorage.copySnapshot(SrsCardId(card.id.value), SrsCardId(target))
                }
                copy
            }
        // Embed the detached copy after the transaction commits and outside it
        // (EmbeddingPort contract): coverage for the new Personal sense, best-effort
        // so a provider miss or storage error never fails the claim.
        runCatchingCancellable { embeddingPort.embed(copy) }
            .onFailure { failure ->
                logger.warn(failure) { "Embedding claimed sense ${copy.id} failed; leaving it for a later backfill." }
            }
        return CardId(copy.id.value)
    }
}
