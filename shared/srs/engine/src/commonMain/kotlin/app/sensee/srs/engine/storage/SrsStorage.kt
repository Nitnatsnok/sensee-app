package app.sensee.srs.engine.storage

import app.sensee.srs.core.model.SrsAlgorithmParameters

public interface SrsStorage<Parameters : SrsAlgorithmParameters> :
    SrsCardStore,
    SrsReviewLogStore,
    SrsParametersStore<Parameters>,
    SrsTransactionRunner
