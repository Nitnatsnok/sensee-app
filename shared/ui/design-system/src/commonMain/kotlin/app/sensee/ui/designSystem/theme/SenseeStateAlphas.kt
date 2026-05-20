package app.sensee.ui.designSystem.theme

/**
 * Shared opacity values for the disabled state of design-system components.
 *
 * Mirrors the alpha pair that Material 3 specifies for disabled controls so
 * the disabled appearance is consistent across the design system without
 * repeating the literals in every `*Defaults.kt`.
 */
public object SenseeStateAlphas {
    public const val DISABLED_CONTAINER: Float = 0.12f
    public const val DISABLED_CONTENT: Float = 0.38f
}
