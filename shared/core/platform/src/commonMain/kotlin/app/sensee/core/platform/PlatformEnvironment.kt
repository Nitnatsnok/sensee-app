package app.sensee.core.platform

public interface PlatformEnvironment {
    public val platform: Platform
    public val context: PlatformContext
}

public enum class Platform {
    Android,
    Ios,
    Desktop,
    Js,
    WasmJs,
}
