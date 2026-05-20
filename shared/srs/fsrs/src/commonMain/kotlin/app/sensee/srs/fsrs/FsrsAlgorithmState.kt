package app.sensee.srs.fsrs

import app.sensee.srs.core.model.SrsAlgorithmInfo
import app.sensee.srs.core.model.SrsAlgorithmState

public data class FsrsAlgorithmState(
    override val algorithm: SrsAlgorithmInfo = FsrsAlgorithm.V6,
    val difficulty: Double,
    val stability: Double,
) : SrsAlgorithmState {
    init {
        require(difficulty > 0.0) {
            "FSRS difficulty must be positive"
        }

        require(stability > 0.0) {
            "FSRS stability must be positive"
        }
    }
}
