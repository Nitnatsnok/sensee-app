package app.sensee.feature.vocabularyEditor.domain

import app.sensee.lexicon.domain.LexiconRepository
import app.sensee.lexicon.domain.Sense
import kotlinx.coroutines.flow.Flow

/**
 * Owns captured lexical entries. Capture is upstream of the catalog (ADR-001);
 * this contract never depends on Library/Practice.
 */
public interface VocabularyRepository : LexiconRepository {
    public suspend fun createDraft(term: String): LexicalEntry

    /**
     * Keeps the current capture session's draft aligned with edited input.
     * Returns `null` if the draft disappeared or is no longer editable.
     */
    public suspend fun updateDraftTerm(
        id: EntryId,
        term: String,
    ): LexicalEntry?

    public override suspend fun getEntry(id: EntryId): LexicalEntry?

    public override suspend fun listEntries(): List<LexicalEntry>

    /** Reactive stream of all entries; emissions fire whenever an entry is upserted or deleted. */
    public override fun observeEntries(): Flow<List<LexicalEntry>>

    /**
     * Adds the user-selected senses and moves the entry to
     * [EntryStatus.Confirmed]. The selection IS the confirmation (ADR-001):
     * there is no separate confirm stage. An empty list is a no-op.
     */
    public suspend fun confirmSenses(
        id: EntryId,
        senses: List<Sense>,
    ): LexicalEntry = error("VocabularyRepository must implement confirmSenses or confirmMeanings")

    public suspend fun confirmMeanings(
        id: EntryId,
        meanings: List<Meaning>,
    ): LexicalEntry = confirmSenses(id, meanings)

    /**
     * Replaces the confirmed content of an existing user entry. Returns `null`
     * when the entry disappeared, is still a draft, or the replacement is empty.
     */
    public suspend fun updateConfirmedEntry(
        id: EntryId,
        term: String,
        senses: List<Sense>,
    ): LexicalEntry? = null

    /**
     * Discards an entry (e.g. an abandoned [EntryStatus.Draft]) so the catalog
     * does not accumulate junk. Idempotent: deleting an absent id is a no-op.
     */
    public suspend fun deleteEntry(id: EntryId)
}
