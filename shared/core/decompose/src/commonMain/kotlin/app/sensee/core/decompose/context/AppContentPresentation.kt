package app.sensee.core.decompose.context

import com.arkivanov.decompose.value.Value

public enum class AppContentPresentation {
    SinglePane,
    ListDetail,
    SupportingPane,
}

public interface AdaptivePresentationContext {
    public val contentPresentation: Value<AppContentPresentation>
}

public interface MutableAdaptivePresentationContext : AdaptivePresentationContext {
    public fun setContentPresentation(presentation: AppContentPresentation)
}
