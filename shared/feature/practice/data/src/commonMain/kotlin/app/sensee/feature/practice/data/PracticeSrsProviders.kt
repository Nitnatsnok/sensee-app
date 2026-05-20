package app.sensee.feature.practice.data

import app.sensee.feature.practice.data.local.PracticeSrsStorage
import app.sensee.srs.engine.SrsEngine
import app.sensee.srs.engine.clock.SrsClock
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.fsrsEngine.FsrsSrsEngineFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
public interface PracticeSrsProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideSrsClock(clock: Clock): SrsClock = SrsClock { clock.now() }

    @SingleIn(AppScope::class)
    @Provides
    public fun providePracticeSrsEngine(
        storage: PracticeSrsStorage,
        clock: SrsClock,
        reviewLogIdGenerator: SrsReviewLogIdGenerator,
    ): SrsEngine<FsrsParameters> =
        FsrsSrsEngineFactory.create(
            storage = storage,
            clock = clock,
            reviewLogIdGenerator = reviewLogIdGenerator,
        )
}
