package app.sensee.srs.core.algorithm

import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsAlgorithmParameters

public interface SrsScheduler<Parameters : SrsAlgorithmParameters> {
    public val algorithm: SrsAlgorithmInfo

    public fun schedule(input: SrsSchedulingInput<Parameters>): SrsSchedulingResult

    public fun preview(input: SrsSchedulingPreviewInput<Parameters>): SrsSchedulingPreview
}
