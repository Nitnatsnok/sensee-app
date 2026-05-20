package app.sensee.srs.core.model

import app.sensee.srs.core.id.SrsParametersId

public interface SrsAlgorithmParameters {
    public val id: SrsParametersId
    public val algorithm: SrsAlgorithmInfo
}
