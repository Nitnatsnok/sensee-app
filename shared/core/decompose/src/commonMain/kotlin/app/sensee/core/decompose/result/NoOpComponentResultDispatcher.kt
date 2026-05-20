package app.sensee.core.decompose.result

public object NoOpComponentResultDispatcher : ComponentResultDispatcher {
    override fun dispatch(result: ComponentResult): Boolean = false
}
