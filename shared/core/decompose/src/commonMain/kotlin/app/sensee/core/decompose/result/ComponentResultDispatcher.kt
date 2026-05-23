package app.sensee.core.decompose.result

public fun interface ComponentResultDispatcher {
    public fun dispatch(result: ComponentResult): Boolean
}
