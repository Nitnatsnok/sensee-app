package app.sensee.lexicon.domain

import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmInline

@JvmInline
public value class EntryId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "EntryId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * A lexical item stored for the user or supplied by Sensee service material.
 *
 * [EntryStatus.Draft] entries are still owned by the vocabulary-editor
 * workflow. [EntryStatus.Confirmed] entries carry accepted [Sense] values and
 * can be projected by Library/Practice without depending on the editor feature.
 */
public data class LexicalEntry(
    val id: EntryId,
    val term: String,
    val status: EntryStatus,
    val senses: List<Sense> = emptyList(),
)

public enum class EntryStatus {
    Draft,
    Confirmed,
}

/**
 * Read-only access to lexical material. Write/edit lifecycle belongs to the
 * feature that owns authoring; readers such as Library consume this boundary.
 */
public interface LexiconRepository {
    public suspend fun getEntry(id: EntryId): LexicalEntry?

    public suspend fun listEntries(): List<LexicalEntry>

    public fun observeEntries(): Flow<List<LexicalEntry>>
}
