package app.sensee.lexicon.data

import app.sensee.lexicon.domain.LexicalEdge
import app.sensee.lexicon.domain.LexicalGraphReader
import app.sensee.lexicon.domain.LexicalNode
import app.sensee.lexicon.domain.LexicalRelation
import app.sensee.lexicon.domain.SenseId
import app.sensee.lexicon.domain.SenseReadRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Derives the lexical relation graph at read time from the canonical sense store
 * (ADR-001): no edge table. Lemma siblings come from the lemma-key index; the
 * word-family / synonym / antonym terms come from a sense's content and resolve to
 * a stored sense that shares their lemma key, else surface as a bare term.
 *
 * Reverse participation (which senses point *at* this one) is a corpus scan left
 * for a backend graph; this reader only walks a sense's own outgoing relations.
 */
@SingleIn(AppScope::class)
@Inject
public class DefaultLexicalGraphReader(
    private val senseReadRepository: SenseReadRepository,
) : LexicalGraphReader {
    override suspend fun edgesFrom(senseId: SenseId): List<LexicalEdge> {
        val source = senseReadRepository.getById(senseId) ?: return emptyList()
        val siblings =
            senseReadRepository
                .listByLemmaKey(source.lemmaKey)
                .filter { it.id != senseId }
                .map {
                    LexicalEdge(senseId, LexicalNode.SenseRef(it.id), LexicalRelation.LemmaSibling, RANK_SIBLING)
                }
        val family =
            source.sense.wordFamily.mapNotNull {
                termEdge(senseId, it.lemma, LexicalRelation.WordFamily, RANK_FAMILY)
            }
        val synonyms =
            source.sense.synonyms.mapNotNull {
                termEdge(senseId, it, LexicalRelation.Synonym, RANK_TERM)
            }
        val antonyms =
            source.sense.antonyms.mapNotNull {
                termEdge(senseId, it, LexicalRelation.Antonym, RANK_TERM)
            }
        // One edge per target — closest relation kept (rank-sorted, then deduped by
        // target) — so a node reachable as both a sibling and a word-family term is
        // emitted once, as the closer relation.
        return (siblings + family + synonyms + antonyms)
            .sortedBy { it.rank }
            .distinctBy { it.to }
    }

    // Resolve a related term to a stored sense under its lemma key — a stable
    // representative (lowest sense_id), so the edge does not move when content is
    // edited — else surface it as a bare, trimmed term. The derive-on-read scan a
    // backend graph would replace. Null for a blank term.
    private suspend fun termEdge(
        from: SenseId,
        term: String,
        relation: LexicalRelation,
        rank: Int,
    ): LexicalEdge? {
        val candidate = term.trim()
        if (candidate.isEmpty()) return null
        val resolved =
            senseReadRepository
                .listByLemmaKey(candidate.lowercase())
                .filter { it.id != from }
                .minByOrNull { it.id.value }
        val node = resolved?.let { LexicalNode.SenseRef(it.id) } ?: LexicalNode.Term(candidate)
        return LexicalEdge(from, node, relation, rank)
    }

    private companion object {
        const val RANK_SIBLING = 0
        const val RANK_FAMILY = 1
        const val RANK_TERM = 2
    }
}
