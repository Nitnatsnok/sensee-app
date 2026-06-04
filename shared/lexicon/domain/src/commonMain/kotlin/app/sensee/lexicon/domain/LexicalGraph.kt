package app.sensee.lexicon.domain

/**
 * Read-side reader of the lexical relation graph, derived at read time from each
 * sense's content and lemma key — there is no edge table (ADR-001). It surfaces a
 * sense's forward relations only (lemma siblings, word family, synonyms, antonyms);
 * reverse participation and finer relations (sub-lemma, collocations) are
 * derive-on-read follow-ups, and a backend graph swaps in behind this same port.
 */
public interface LexicalGraphReader {
    /**
     * The edges radiating from [senseId]: its lemma siblings (other senses under
     * the same lemma key) and the word-family / synonym / antonym terms carried on
     * its content, each resolved to a stored sense when one shares its lemma key.
     * At most one edge per target — the closest relation — ordered by
     * [LexicalEdge.rank]. Empty when the sense is unknown.
     */
    public suspend fun edgesFrom(senseId: SenseId): List<LexicalEdge>
}

/**
 * A directed lexical relation from a source sense to a [LexicalNode], [rank]ed so
 * the closest relations come first (lemma siblings before word family before
 * synonyms/antonyms).
 */
public data class LexicalEdge(
    val from: SenseId,
    val to: LexicalNode,
    val relation: LexicalRelation,
    val rank: Int,
)

/** The target of a [LexicalEdge]: a stored sense to navigate to, or a bare term. */
public sealed interface LexicalNode {
    public data class SenseRef(
        val id: SenseId,
    ) : LexicalNode

    public data class Term(
        val text: String,
    ) : LexicalNode
}

public enum class LexicalRelation { LemmaSibling, WordFamily, Synonym, Antonym }
