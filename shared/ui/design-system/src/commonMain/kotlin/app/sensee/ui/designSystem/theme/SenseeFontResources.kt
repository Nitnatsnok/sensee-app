package app.sensee.ui.designSystem.theme

import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceItem

@OptIn(InternalResourceApi::class)
internal object SenseeFontResources {
    private const val RESOURCE_BASE_PATH =
        "composeResources/sensee.shared.ui.design_system.generated.resources/font"

    val mulishVariable =
        FontResource(
            id = "font:mulish_variable",
            items = setOf(ResourceItem(setOf(), "$RESOURCE_BASE_PATH/mulish_variable.ttf", -1, -1)),
        )

    val mulishItalicVariable =
        FontResource(
            id = "font:mulish_italic_variable",
            items = setOf(ResourceItem(setOf(), "$RESOURCE_BASE_PATH/mulish_italic_variable.ttf", -1, -1)),
        )
}
