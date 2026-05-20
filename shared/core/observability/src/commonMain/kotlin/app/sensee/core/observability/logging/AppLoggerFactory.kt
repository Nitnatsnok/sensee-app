package app.sensee.core.observability.logging

public interface AppLoggerFactory {
    public fun tagged(tag: String): AppLogger
}
