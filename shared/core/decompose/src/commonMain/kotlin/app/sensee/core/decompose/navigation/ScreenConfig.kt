package app.sensee.core.decompose.navigation

public interface ScreenConfig

public interface TargetedScreenConfig<out T : ScreenConfig> : ScreenConfig {
    public val target: T?
}
