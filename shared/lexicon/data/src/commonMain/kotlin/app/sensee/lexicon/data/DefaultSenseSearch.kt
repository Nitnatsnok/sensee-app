package app.sensee.lexicon.data

import app.sensee.grammar.domain.SurfaceForm
import app.sensee.grammar.domain.SurfaceToken
import app.sensee.lexicon.domain.SearchPort
import app.sensee.lexicon.domain.SenseQuery
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.StoredSense
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first

/**
 * In-memory [SearchPort] over the canonical read model — a thin derived projection
 * kept out of [DefaultSenseRepository], so the store stays read+write only and the
 * search concern owns its own ranking. Matches a query exact → prefix → substring
 * across the surface form, translation and lemma key. The authoritative FTS /
 * semantic backend swaps in behind this same port.
 */
@SingleIn(AppScope::class)
@Inject
public class DefaultSenseSearch(
    private val senseReadRepository: SenseReadRepository,
) : SearchPort {
    override suspend fun search(query: SenseQuery): List<StoredSense> {
        val term = query.text.trim().lowercase()
        if (term.isEmpty()) return emptyList()
        val rows =
            query.status?.let { senseReadRepository.listByStatus(it) }
                ?: senseReadRepository.observe().first()
        return rows
            .asSequence()
            .mapNotNull { stored -> stored.matchRank(term)?.let { RankedSense(stored, it) } }
            .sortedWith(compareBy<RankedSense> { it.rank }.thenByDescending { it.sense.updatedAtEpochMs })
            .take(query.limit)
            .map { it.sense }
            .toList()
    }
}

private enum class SenseMatchRank { EXACT, PREFIX, SUBSTRING }

private class RankedSense(
    val sense: StoredSense,
    val rank: SenseMatchRank,
)

// Lower rank = better match; null = no match. Surface form (literal tokens only,
// not argument-slot placeholder names), translation, and lemma key are matched
// the same way — exact, then prefix, then any substring — so a query ranks
// consistently whether it hits the L2 form, the L1 translation, or the head lemma.
private fun StoredSense.matchRank(term: String): SenseMatchRank? {
    val haystacks =
        listOfNotNull(
            sense.surfaceForm?.searchableText()?.lowercase(),
            sense.translation.lowercase(),
            lemmaKey.lowercase(),
        )
    return when {
        haystacks.any { it == term } -> SenseMatchRank.EXACT
        haystacks.any { it.startsWith(term) } -> SenseMatchRank.PREFIX
        haystacks.any { it.contains(term) } -> SenseMatchRank.SUBSTRING
        else -> null
    }
}

// The lexical text to match against: literal and optional-particle tokens, not
// argument-slot names (`<something>` is a placeholder, not searchable material).
private fun SurfaceForm.searchableText(): String {
    val parts =
        tokens.mapNotNull { token ->
            when (token) {
                is SurfaceToken.Literal -> token.text
                is SurfaceToken.Optional -> token.text
                is SurfaceToken.Slot -> null
            }
        }
    return parts.joinToString(" ")
}
