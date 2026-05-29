package app.sensee.feature.vocabularyEditor.data

import app.sensee.feature.vocabularyEditor.domain.VocabularyRepository
import app.sensee.lexicon.domain.LexiconRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
public interface VocabularyDataProviders {
    @Provides
    public fun provideLexiconRepository(repository: VocabularyRepository): LexiconRepository = repository
}
