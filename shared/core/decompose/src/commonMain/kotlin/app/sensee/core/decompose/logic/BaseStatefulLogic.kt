package app.sensee.core.decompose.logic

import app.sensee.core.coroutines.AppDispatchers
import app.sensee.core.observability.diagnostics.AppDiagnostics

public abstract class BaseStatefulLogic<State : Any>(
    protected var state: State,
    appDispatchers: AppDispatchers,
    appDiagnostics: AppDiagnostics,
) : BaseLogic(appDispatchers, appDiagnostics),
    StatefulLogic<State> {
    override fun saveState(): State = state
}
