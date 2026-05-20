package app.sensee.feature.startup.presentation.impl

import app.sensee.core.decompose.context.AppComponentContext
import app.sensee.core.tracing.Tracer
import app.sensee.core.tracing.span
import app.sensee.feature.startup.presentation.api.StartupComponent
import app.sensee.grammar.data.GrammarLabelsProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding

@AssistedInject
public class DefaultStartupComponent(
    @Assisted componentContext: AppComponentContext,
    @Assisted private val onFinished: () -> Unit,
    private val grammarLabelsProvider: GrammarLabelsProvider,
    private val tracer: Tracer,
) : StartupComponent,
    AppComponentContext by componentContext {
    // Same memoized warm the RootComponent triggers app-scoped; awaiting it
    // here keeps the splash up exactly while the dictionary is loading.
    override suspend fun awaitReady() {
        tracer.span("startup.awaitReady") {
            tracer.span("startup.grammarLabels") {
                grammarLabelsProvider.labels()
            }
        }
    }

    override fun onFinished() {
        onFinished.invoke()
    }

    @AssistedFactory
    @ContributesBinding(
        scope = AppScope::class,
        binding = binding<StartupComponent.Factory>(),
    )
    public fun interface Factory : StartupComponent.Factory {
        override fun create(
            componentContext: AppComponentContext,
            onFinished: () -> Unit,
        ): DefaultStartupComponent
    }
}
