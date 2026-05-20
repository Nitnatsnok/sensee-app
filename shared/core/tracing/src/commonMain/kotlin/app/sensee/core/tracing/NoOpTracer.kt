package app.sensee.core.tracing

public object NoOpTracer : Tracer {
    override fun startSpan(
        name: String,
        parent: SpanContext?,
    ): Span = NoOpSpan
}

@Suppress("EmptyFunctionBlock")
public object NoOpSpan : Span {
    override val name: String = ""
    override val context: SpanContext = SpanContext("", "", null)

    override fun setAttribute(
        key: String,
        value: String,
    ) {}

    override fun setAttribute(
        key: String,
        value: Long,
    ) {}

    override fun setAttribute(
        key: String,
        value: Boolean,
    ) {}

    override fun addEvent(
        name: String,
        attributes: Map<String, String>,
    ) {}

    override fun setStatus(
        status: SpanStatus,
        description: String?,
    ) {}

    override fun recordException(throwable: Throwable) {}

    override fun end() {}
}
