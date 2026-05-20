package app.sensee.core.decompose.navigation

public enum class NavigationRequestStatus {
    Handled,
    Unhandled,
}

public interface NavigationDispatcher {
    public fun open(
        target: ScreenConfig,
        onComplete: (isSuccess: Boolean) -> Unit = {},
    ): NavigationRequestStatus

    public fun back(onResult: (NavigationRequestStatus) -> Unit = {})
}
