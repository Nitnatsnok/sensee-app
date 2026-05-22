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

/** Whether the host is a browser target — the `Js` or `WasmJs` build of the web app. */
public val Platform.isWeb: Boolean
    get() = this == Platform.Js || this == Platform.WasmJs
