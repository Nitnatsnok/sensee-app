package app.sensee.srs.engine

import app.sensee.srs.core.algorithm.SrsReviewOptionsPreview
import app.sensee.srs.core.algorithm.SrsScheduler
import app.sensee.srs.core.algorithm.SrsSchedulingInput
import app.sensee.srs.core.algorithm.SrsSchedulingPreview
import app.sensee.srs.core.algorithm.SrsSchedulingPreviewInput
import app.sensee.srs.core.algorithm.SrsSchedulingResult
import app.sensee.srs.core.algorithm.toReviewOptionsPreview
import app.sensee.srs.core.model.SrsAlgorithmParameters
import app.sensee.srs.engine.clock.SrsClock
import app.sensee.srs.engine.id.SrsReviewLogIdGenerator
import app.sensee.srs.engine.storage.SrsStorage

public class SrsEngine<Parameters : SrsAlgorithmParameters>(
    private val scheduler: SrsScheduler<Parameters>,
    private val storage: SrsStorage<Parameters>,
    private val clock: SrsClock,
    private val reviewLogIdGenerator: SrsReviewLogIdGenerator,
) {
    public suspend fun submitReview(request: SrsReviewRequest): SrsSchedulingResult =
        storage.transaction {
            val card =
                storage.getCard(request.cardId)
                    ?: throw SrsCardNotFoundException(request.cardId)

            val parameters = storage.getActiveParameters(request.scope)

            val reviewedAt = request.reviewedAt ?: clock.now()

            val result =
                scheduler.schedule(
                    input =
                        SrsSchedulingInput(
                            card = card,
                            rating = request.rating,
                            reviewedAt = reviewedAt,
                            parameters = parameters,
                            reviewLogId = reviewLogIdGenerator.nextId(),
                        ),
                )

            storage.saveCard(result.updatedCard)
            storage.appendReviewLog(result.reviewLog)

            result
        }

    public suspend fun preview(request: SrsPreviewRequest): SrsSchedulingPreview =
        storage.transaction {
            val card =
                storage.getCard(request.cardId)
                    ?: throw SrsCardNotFoundException(request.cardId)

            val parameters = storage.getActiveParameters(request.scope)

            val reviewedAt = request.reviewedAt ?: clock.now()

            scheduler.preview(
                input =
                    SrsSchedulingPreviewInput(
                        card = card,
                        reviewedAt = reviewedAt,
                        parameters = parameters,
                    ),
            )
        }

    public suspend fun previewOptions(request: SrsPreviewRequest): SrsReviewOptionsPreview =
        preview(request)
            .toReviewOptionsPreview()
}
