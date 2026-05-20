package app.sensee.srs.engine.storage

import app.sensee.srs.core.id.SrsScope
import app.sensee.srs.core.model.SrsAlgorithmParameters

public interface SrsParametersStore<Parameters : SrsAlgorithmParameters> {
    public suspend fun getActiveParameters(scope: SrsScope): Parameters

    public suspend fun saveParameters(
        scope: SrsScope,
        parameters: Parameters,
    )
}
