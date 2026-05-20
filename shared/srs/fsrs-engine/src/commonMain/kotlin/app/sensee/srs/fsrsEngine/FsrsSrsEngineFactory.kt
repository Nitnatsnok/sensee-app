package app.sensee.srs.fsrsEngine

import app.sensee.srs.engine.SrsEngine
import app.sensee.srs.engine.SrsEngineFactory
import app.sensee.srs.engine.clock.SrsClock
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.engine.storage.SrsStorage
import app.sensee.srs.fsrs.FsrsParameters
import app.sensee.srs.fsrs.FsrsScheduler

public object FsrsSrsEngineFactory {
    public fun create(
        storage: SrsStorage<FsrsParameters>,
        clock: SrsClock,
        reviewLogIdGenerator: SrsReviewLogIdGenerator,
        scheduler: FsrsScheduler = FsrsScheduler(),
    ): SrsEngine<FsrsParameters> =
        SrsEngineFactory.create(
            scheduler = scheduler,
            storage = storage,
            clock = clock,
            reviewLogIdGenerator = reviewLogIdGenerator,
        )
}
