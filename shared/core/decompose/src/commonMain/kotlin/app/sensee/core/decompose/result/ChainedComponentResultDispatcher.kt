package app.sensee.core.decompose.result

public class ChainedComponentResultDispatcher(
    private val local: ComponentResultDispatcher,
    private val parent: ComponentResultDispatcher?,
) : ComponentResultDispatcher {
    override fun dispatch(result: ComponentResult): Boolean =
        local.dispatch(result) ||
            parent?.dispatch(result) == true
}
