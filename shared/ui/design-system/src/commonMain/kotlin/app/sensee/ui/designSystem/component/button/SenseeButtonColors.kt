package app.sensee.ui.designSystem.component.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import app.sensee.ui.designSystem.theme.SenseeStateAlphas
import app.sensee.ui.designSystem.theme.SenseeTheme

@Immutable
public data class SenseeButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val border: Color,
    val disabledBorder: Color,
) {
    @Stable
    public fun containerColor(enabled: Boolean): Color = if (enabled) container else disabledContainer

    @Stable
    public fun contentColor(enabled: Boolean): Color = if (enabled) content else disabledContent

    @Stable
    public fun borderColor(enabled: Boolean): Color = if (enabled) border else disabledBorder

    public companion object {
        @Composable
        public fun default(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
            disabledContainer: Color = Color.Unspecified,
            disabledContent: Color = Color.Unspecified,
            border: Color = Color.Unspecified,
            disabledBorder: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.accent },
                content = content.takeOrElse { colors.textOnAccent },
                disabledContainer =
                    disabledContainer.takeOrElse {
                        colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER)
                    },
                disabledContent =
                    disabledContent.takeOrElse {
                        colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT)
                    },
                border = border.takeOrElse { Color.Transparent },
                disabledBorder = disabledBorder.takeOrElse { Color.Transparent },
            )
        }

        @Composable
        public fun tonal(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.accentContainer },
                content = content.takeOrElse { colors.textOnAccentContainer },
                disabledContainer = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER),
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }

        @Composable
        public fun outlined(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
            border: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { Color.Transparent },
                content = content.takeOrElse { colors.accent },
                disabledContainer = Color.Transparent,
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = border.takeOrElse { colors.border },
                disabledBorder = colors.divider,
            )
        }

        @Composable
        public fun text(content: Color = Color.Unspecified): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = Color.Transparent,
                content = content.takeOrElse { colors.accent },
                disabledContainer = Color.Transparent,
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }

        @Composable
        public fun danger(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.danger },
                content = content.takeOrElse { colors.textOnDanger },
                disabledContainer = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER),
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }

        @Composable
        public fun success(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.success },
                content = content.takeOrElse { colors.textOnSuccess },
                disabledContainer = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER),
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }

        @Composable
        public fun warning(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.warning },
                content = content.takeOrElse { colors.textOnWarning },
                disabledContainer = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER),
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }

        @Composable
        public fun info(
            container: Color = Color.Unspecified,
            content: Color = Color.Unspecified,
        ): SenseeButtonColors {
            val colors = SenseeTheme.colors

            return SenseeButtonColors(
                container = container.takeOrElse { colors.info },
                content = content.takeOrElse { colors.textOnInfo },
                disabledContainer = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTAINER),
                disabledContent = colors.textMuted.copy(alpha = SenseeStateAlphas.DISABLED_CONTENT),
                border = Color.Transparent,
                disabledBorder = Color.Transparent,
            )
        }
    }
}
