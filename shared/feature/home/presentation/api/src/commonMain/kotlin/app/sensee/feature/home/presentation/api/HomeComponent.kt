package app.sensee.feature.home.presentation.api

import app.sensee.core.decompose.AppComponent
import app.sensee.core.decompose.context.AppComponentContext

public interface HomeComponent : AppComponent {
    public fun interface Factory {
        public fun create(componentContext: AppComponentContext): HomeComponent
    }
}
