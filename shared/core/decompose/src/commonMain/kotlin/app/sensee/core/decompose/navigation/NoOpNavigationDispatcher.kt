package app.sensee.core.decompose.navigation

public object NoOpNavigationDispatcher : NavigationDispatcher {
    override fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit,
    ): NavigationRequestStatus = NavigationRequestStatus.Unhandled

    override fun back(onResult: (NavigationRequestStatus) -> Unit) {
        onResult(NavigationRequestStatus.Unhandled)
    }
}
