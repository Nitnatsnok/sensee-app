package app.sensee.feature.home.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.feature.home.presentation.api.HomeComponent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding

@AssistedInject
public class DefaultHomeComponent(
    @Assisted componentContext: AppComponentContext,
) : HomeComponent,
    AppComponentContext by componentContext {
    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<HomeComponent.Factory>(),
    )
    public interface Factory : HomeComponent.Factory {
        override fun create(componentContext: AppComponentContext): DefaultHomeComponent
    }
}
