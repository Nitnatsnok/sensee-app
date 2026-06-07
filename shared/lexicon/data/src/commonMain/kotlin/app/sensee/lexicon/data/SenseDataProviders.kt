package app.sensee.lexicon.data

import app.sensee.lexicon.domain.EmbeddingPort
import app.sensee.lexicon.domain.LexicalGraphReader
import app.sensee.lexicon.domain.SearchPort
import app.sensee.lexicon.domain.SenseReadRepository
import app.sensee.lexicon.domain.SenseWriteRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/**
 * Binds the lexicon data singletons to their domain contracts: the single
 * [DefaultSenseRepository] to the read, write, and search contracts (projections,
 * the editor/ingest, and search all share one instance), the separate
 * derive-on-read [DefaultLexicalGraphReader] to [LexicalGraphReader], and the
 * separate [DefaultEmbeddingPort] to [EmbeddingPort] (it carries the embedding
 * table and an [app.sensee.ai.core.contract.AiEmbeddingClient] of its own).
 */
@ContributesTo(AppScope::class)
public interface SenseDataProviders {
    @Provides
    public fun provideSenseReadRepository(repository: DefaultSenseRepository): SenseReadRepository = repository

    @Provides
    public fun provideSenseWriteRepository(repository: DefaultSenseRepository): SenseWriteRepository = repository

    @Provides
    public fun provideSearchPort(search: DefaultSenseSearch): SearchPort = search

    @Provides
    public fun provideLexicalGraphReader(reader: DefaultLexicalGraphReader): LexicalGraphReader = reader

    @Provides
    public fun provideEmbeddingPort(port: DefaultEmbeddingPort): EmbeddingPort = port
}
