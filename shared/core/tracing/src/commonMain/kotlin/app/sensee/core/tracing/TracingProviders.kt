package app.sensee.core.tracing

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock

@ContributesTo(AppScope::class)
public interface TracingProviders {
    @SingleIn(AppScope::class)
    @Provides
    public fun provideTracing(
        processors: Set<SpanProcessor>,
        clock: Clock,
    ): Tracing = Tracing(processors.toList(), clock)

    @SingleIn(AppScope::class)
    @Provides
    public fun provideTracer(tracing: Tracing): Tracer = tracing.tracer()
}
