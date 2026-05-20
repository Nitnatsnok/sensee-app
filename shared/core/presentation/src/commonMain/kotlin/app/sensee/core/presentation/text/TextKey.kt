package app.sensee.core.presentation.text

import kotlin.jvm.JvmInline
import kotlin.math.abs

@JvmInline
public value class TextKey(
    public val value: String,
)

@JvmInline
public value class QuantityTextKey(
    public val value: String,
)

public interface TextProvider {
    public fun hasText(key: TextKey): Boolean = false

    public fun hasQuantity(key: QuantityTextKey): Boolean = false

    public fun text(
        key: TextKey,
        vararg arguments: Any?,
    ): String

    public fun quantity(
        key: QuantityTextKey,
        count: Int,
        vararg arguments: Any?,
    ): String = text(TextKey(key.value), count, *arguments)

    public fun errorText(
        throwable: Throwable,
        fallback: TextKey,
    ): String = throwableText(throwable) ?: text(fallback)

    public fun throwableText(throwable: Throwable): String? = null
}

public class MapTextProvider(
    private val values: Map<TextKey, String>,
    private val quantities: Map<QuantityTextKey, QuantityTextTemplates> = emptyMap(),
    private val throwableTextMapper: ThrowableTextMapper = ThrowableTextMapper { null },
) : TextProvider {
    override fun hasText(key: TextKey): Boolean = values.containsKey(key)

    override fun hasQuantity(key: QuantityTextKey): Boolean =
        quantities.containsKey(key) || values.containsKey(TextKey(key.value))

    override fun text(
        key: TextKey,
        vararg arguments: Any?,
    ): String {
        val template = values[key] ?: key.value
        return template.formatText(arguments)
    }

    override fun quantity(
        key: QuantityTextKey,
        count: Int,
        vararg arguments: Any?,
    ): String {
        val template =
            quantities[key]?.templateFor(russianPluralForm(count))
                ?: values[TextKey(key.value)]
                ?: key.value
        return template.formatText(quantityArguments(count, arguments))
    }

    override fun throwableText(throwable: Throwable): String? = throwableTextMapper.text(throwable)
}

public class CompositeTextProvider(
    private val providers: List<TextProvider>,
) : TextProvider {
    public constructor(vararg providers: TextProvider) : this(providers.toList())

    override fun hasText(key: TextKey): Boolean = providers.any { it.hasText(key) }

    override fun hasQuantity(key: QuantityTextKey): Boolean = providers.any { it.hasQuantity(key) }

    override fun text(
        key: TextKey,
        vararg arguments: Any?,
    ): String =
        providers
            .firstOrNull { it.hasText(key) }
            ?.text(key, *arguments)
            ?: key.value

    override fun quantity(
        key: QuantityTextKey,
        count: Int,
        vararg arguments: Any?,
    ): String =
        providers
            .firstOrNull { it.hasQuantity(key) }
            ?.quantity(key, count, *arguments)
            ?: text(TextKey(key.value), count, *arguments)

    override fun errorText(
        throwable: Throwable,
        fallback: TextKey,
    ): String = throwableText(throwable) ?: text(fallback)

    override fun throwableText(throwable: Throwable): String? =
        providers.firstNotNullOfOrNull { provider -> provider.throwableText(throwable) }
}

public fun interface ThrowableTextMapper {
    public fun text(throwable: Throwable): String?
}

public fun TextProvider.withFallback(fallback: TextProvider): TextProvider = CompositeTextProvider(this, fallback)

public data class QuantityTextTemplates(
    public val one: String,
    public val few: String,
    public val many: String,
    public val other: String = many,
)

private fun String.formatText(arguments: Array<out Any?>): String {
    var result = this
    arguments.forEachIndexed { index, argument ->
        val value = argument?.toString().orEmpty()
        val resourceArgumentIndex = index + 1
        result =
            result
                .replace("{$index}", value)
                .replace("%${resourceArgumentIndex}\$s", value)
                .replace("%${resourceArgumentIndex}\$d", value)
    }
    return result
}

private enum class PluralForm {
    One,
    Few,
    Many,
    Other,
}

private fun QuantityTextTemplates.templateFor(form: PluralForm): String =
    when (form) {
        PluralForm.One -> one
        PluralForm.Few -> few
        PluralForm.Many -> many
        PluralForm.Other -> other
    }

private fun russianPluralForm(count: Int): PluralForm {
    val value = abs(count.toLong())
    val mod10 = value % 10
    val mod100 = value % 100
    return when {
        mod10 == 1L && mod100 != 11L -> PluralForm.One
        mod10 in 2L..4L && mod100 !in 12L..14L -> PluralForm.Few
        else -> PluralForm.Many
    }
}

private fun quantityArguments(
    count: Int,
    arguments: Array<out Any?>,
): Array<Any?> =
    arrayOfNulls<Any?>(arguments.size + 1).also { result ->
        result[0] = count
        arguments.forEachIndexed { index, argument ->
            result[index + 1] = argument
        }
    }
