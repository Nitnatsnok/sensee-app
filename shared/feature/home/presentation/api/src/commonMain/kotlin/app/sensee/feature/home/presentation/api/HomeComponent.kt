package app.sensee.feature.home.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.presentation.DataLoadingState
import kotlinx.coroutines.flow.StateFlow

public interface HomeComponent : AppComponent {
    public val uiState: StateFlow<HomeUiState>

    public fun onAction(action: HomeAction)

    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): HomeComponent
    }
}

public data class HomeUiState(
    val loadingState: DataLoadingState = DataLoadingState.Loading,
    /** Due cards to surface, capped to the due session limit; see [dueExceedsSessionLimit]. */
    val dueCount: Int = 0,
    /** True when more cards are due than one session takes, so the count is shown as "N+". */
    val dueExceedsSessionLimit: Boolean = false,
    val dailyGoal: Int = 0,
) {
    /** The review CTA is actionable only when something is actually due. */
    val isReviewActionable: Boolean get() = dueCount > 0
}

public sealed interface HomeAction {
    public data object Retry : HomeAction

    /** Start the review session over the cards due now («Стоит повторить»). */
    public data object StartDueSession : HomeAction
}
