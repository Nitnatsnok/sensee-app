package app.sensee.core.compose

import androidx.compose.ui.Modifier

public inline fun Modifier.thenIf(
    condition: Boolean,
    builder: Modifier.Companion.() -> Modifier,
): Modifier = then(if (condition) Modifier.Companion.builder() else Modifier)

public inline fun <T> Modifier.thenIfNotNull(
    value: T?,
    builder: Modifier.Companion.(T) -> Modifier,
): Modifier = then(if (value != null) Modifier.Companion.builder(value) else Modifier)
