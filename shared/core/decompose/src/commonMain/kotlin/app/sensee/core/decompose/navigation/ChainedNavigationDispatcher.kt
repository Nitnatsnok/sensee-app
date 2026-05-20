package app.sensee.core.decompose.navigation

public class ChainedNavigationDispatcher(
    private val local: NavigationDispatcher,
    private val parent: NavigationDispatcher?,
) : NavigationDispatcher {
    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus {
        val localStatus =
            local.open(
                target = target,
                onComplete = onComplete,
            )
        if (localStatus == NavigationRequestStatus.Handled) return localStatus

        return parent?.open(
            target = target,
            onComplete = onComplete,
        ) ?: NavigationRequestStatus.Unhandled
    }

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        local.back { result ->
            when (result) {
                NavigationRequestStatus.Handled -> onResult(NavigationRequestStatus.Handled)
                NavigationRequestStatus.Unhandled -> backParent(onResult)
            }
        }
    }

    private fun backParent(onResult: (NavigationRequestStatus) -> Unit) {
        parent?.back(onResult) ?: onResult(NavigationRequestStatus.Unhandled)
    }
}
