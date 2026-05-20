package app.sensee.ui.adaptive

public enum class WidthSizeClass {
    Compact,
    Medium,
    Expanded,
    Large,
    ExtraLarge,
}

public enum class HeightSizeClass {
    Compact,
    Medium,
    Expanded,
}

public enum class NavigationType {
    BottomBar,
    NavigationRail,
    PermanentNavigationRail,
}

public enum class ContentLayoutType {
    SinglePane,
    ListDetail,
    SupportingPane,
}

public data class AppAdaptiveInfo(
    val widthSizeClass: WidthSizeClass,
    val heightSizeClass: HeightSizeClass,
) {
    val navigationType: NavigationType
        get() =
            when {
                // Wide windows would normally get a rail, but a vertically stacked rail
                // overflows when the window is short (e.g. desktop window resized to a thin
                // strip). Falling back to the bottom bar lets the items lay out horizontally
                // and stay visible. M3 follows the same rule for landscape phones.
                isHeightCompact -> NavigationType.BottomBar
                isWidthAtLeast(WidthSizeClass.Large) -> NavigationType.PermanentNavigationRail
                isWidthAtLeast(WidthSizeClass.Medium) -> NavigationType.NavigationRail
                else -> NavigationType.BottomBar
            }

    val contentLayoutType: ContentLayoutType
        get() =
            when {
                isWidthAtLeast(WidthSizeClass.Large) -> ContentLayoutType.SupportingPane
                isWidthAtLeast(WidthSizeClass.Expanded) -> ContentLayoutType.ListDetail
                else -> ContentLayoutType.SinglePane
            }

    val showNavigationRail: Boolean
        get() = navigationType != NavigationType.BottomBar

    val supportsTwoPanes: Boolean
        get() = contentLayoutType != ContentLayoutType.SinglePane

    val showTopAppBar: Boolean
        get() = isHeightAtLeast(HeightSizeClass.Medium)

    val isHeightCompact: Boolean
        get() = heightSizeClass == HeightSizeClass.Compact

    public fun isWidthAtLeast(sizeClass: WidthSizeClass): Boolean = widthSizeClass.ordinal >= sizeClass.ordinal

    public fun isHeightAtLeast(sizeClass: HeightSizeClass): Boolean = heightSizeClass.ordinal >= sizeClass.ordinal
}
