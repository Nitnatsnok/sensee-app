package app.sensee.srs.engine

import app.sensee.srs.core.algorithm.SrsScheduler
import app.sensee.srs.core.model.SrsAlgorithmParameters
import app.sensee.srs.engine.clock.SrsClock
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.engine.storage.SrsStorage

public object SrsEngineFactory {
    public fun <Parameters : SrsAlgorithmParameters> create(
        scheduler: SrsScheduler<Parameters>,
        storage: SrsStorage<Parameters>,
        clock: SrsClock,
        reviewLogIdGenerator: SrsReviewLogIdGenerator,
    ): SrsEngine<Parameters> =
        SrsEngine(
            scheduler = scheduler,
            storage = storage,
            clock = clock,
            reviewLogIdGenerator = reviewLogIdGenerator,
        )
}
