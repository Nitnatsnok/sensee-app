package app.sensee.srs.testKit

import app.sensee.srs.core.id.SrsCardId
import app.sensee.srs.core.id.SrsScope
import app.sensee.srs.core.log.SrsReviewLog
import app.sensee.srs.core.model.SrsAlgorithmParameters
import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import app.sensee.srs.engine.storage.SrsStorage
import kotlin.time.Instant

public class InMemorySrsStorage<Parameters : SrsAlgorithmParameters>(
    initialParameters: Parameters,
    initialScope: SrsScope = SrsScope.Default,
) : SrsStorage<Parameters> {
    private val cards = LinkedHashMap<SrsCardId, SrsCardSnapshot>()
    private val logs = mutableListOf<SrsReviewLog>()
    private val parametersByScope = LinkedHashMap<SrsScope, Parameters>()

    init {
        parametersByScope[initialScope] = initialParameters

        if (initialScope != SrsScope.Default) {
            parametersByScope[SrsScope.Default] = initialParameters
        }
    }

    override suspend fun getCard(cardId: SrsCardId): SrsCardSnapshot? = cards[cardId]

    override suspend fun saveCard(card: SrsCardSnapshot) {
        cards[card.id] = card
    }

    override suspend fun getDueCards(
        now: Instant,
        limit: Int,
    ): List<SrsCardSnapshot> {
        require(limit > 0) {
            "limit must be positive"
        }

        return cards.values
            .asSequence()
            .filter { card ->
                val dueAt = card.dueAt
                card.state != SrsCardState.Suspended && dueAt != null && dueAt <= now
            }.sortedBy { it.dueAt }
            .take(limit)
            .toList()
    }

    override suspend fun appendReviewLog(log: SrsReviewLog) {
        logs += log
    }

    override suspend fun getReviewLogs(
        cardId: SrsCardId,
        limit: Int?,
    ): List<SrsReviewLog> {
        val allLogs =
            logs
                .asSequence()
                .filter { it.cardId == cardId }
                .sortedBy { it.reviewedAt }
                .toList()

        return if (limit == null) {
            allLogs
        } else {
            require(limit > 0) {
                "limit must be positive"
            }

            allLogs.takeLast(limit)
        }
    }

    override suspend fun getActiveParameters(scope: SrsScope): Parameters =
        parametersByScope[scope]
            ?: parametersByScope.getValue(SrsScope.Default)

    override suspend fun saveParameters(
        scope: SrsScope,
        parameters: Parameters,
    ) {
        parametersByScope[scope] = parameters
    }

    override suspend fun <T> transaction(block: suspend () -> T): T {
        val cardsSnapshot = LinkedHashMap(cards)
        val logsSnapshot = logs.toMutableList()
        val parametersSnapshot = LinkedHashMap(parametersByScope)
        var committed = false

        try {
            val result = block()
            committed = true
            return result
        } finally {
            if (!committed) {
                cards.clear()
                cards.putAll(cardsSnapshot)

                logs.clear()
                logs.addAll(logsSnapshot)

                parametersByScope.clear()
                parametersByScope.putAll(parametersSnapshot)
            }
        }
    }

    public suspend fun saveCards(cards: Iterable<SrsCardSnapshot>) {
        cards.forEach { saveCard(it) }
    }

    public fun getAllCards(): List<SrsCardSnapshot> = cards.values.toList()

    public fun getAllReviewLogs(): List<SrsReviewLog> = logs.toList()

    public fun clear() {
        cards.clear()
        logs.clear()
    }
}
