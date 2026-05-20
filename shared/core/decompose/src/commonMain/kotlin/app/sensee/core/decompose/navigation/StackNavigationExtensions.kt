package app.sensee.core.decompose.navigation

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.pop as decomposePop

public fun <C : Any> StackNavigation<C>.pop(onResult: (NavigationRequestStatus) -> Unit = {}) {
    decomposePop { isSuccess ->
        onResult(isSuccess.toNavigationRequestStatus())
    }
}

private fun Boolean.toNavigationRequestStatus(): NavigationRequestStatus =
    if (this) {
        NavigationRequestStatus.Handled
    } else {
        NavigationRequestStatus.Unhandled
    }
