package app.sensee.appShell.primary

import app.sensee.core.decompose.navigation.ScreenConfig
import app.sensee.core.decompose.navigation.TargetedScreenConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class PrimaryShellConfig(
    // Transient bootstrap hint, not persisted state: it is consumed
    // synchronously when the child PrimaryShell is created. `ScreenConfig` is a
    // non-sealed polymorphic type, and the web-history / StateKeeper path
    // serializes nested configs through Decompose's internal Json, whose empty
    // SerializersModule cannot resolve the concrete target subclass. The active
    // section is restored from PrimaryShell's own persisted stack, so the root
    // never needs to serialize this target.
    @Transient
    override val target: ScreenConfig? = null,
) : TargetedScreenConfig<ScreenConfig>
