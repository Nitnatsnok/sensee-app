package app.sensee.core.decompose.navigation

import kotlinx.coroutines.flow.StateFlow

public interface BottomBarVisibilityOwner {
    public val showBottomBar: StateFlow<Boolean>
}
