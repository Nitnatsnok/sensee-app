package app.sensee.core.presentation.text

import kotlin.test.Test
import kotlin.test.assertEquals

class MapTextProviderTest {
    @Test
    fun `text falls back to key value when template is missing`() {
        val provider = MapTextProvider(values = emptyMap())

        assertEquals("missing.key", provider.text(TextKey("missing.key")))
    }

    @Test
    fun `text formats brace and resource placeholders`() {
        val provider =
            MapTextProvider(
                values =
                    mapOf(
                        TextKey("summary") to "{0}: %1\$s, %2\$d",
                    ),
            )

        assertEquals("Cards: Cards, 42", provider.text(TextKey("summary"), "Cards", 42))
    }

    @Test
    fun `quantity selects russian one form`() {
        val provider = quantityProvider()

        assertEquals("1 one", provider.quantity(QuantityKey, 1))
        assertEquals("21 one", provider.quantity(QuantityKey, 21))
    }

    @Test
    fun `quantity selects russian few form`() {
        val provider = quantityProvider()

        assertEquals("2 few", provider.quantity(QuantityKey, 2))
        assertEquals("24 few", provider.quantity(QuantityKey, 24))
    }

    @Test
    fun `quantity selects russian many form`() {
        val provider = quantityProvider()

        assertEquals("5 many", provider.quantity(QuantityKey, 5))
        assertEquals("11 many", provider.quantity(QuantityKey, 11))
    }

    @Test
    fun `errorText uses throwable mapper before fallback text`() {
        val provider =
            MapTextProvider(
                values = mapOf(FallbackKey to "Fallback"),
                throwableTextMapper =
                    ThrowableTextMapper { throwable ->
                        "Mapped ${throwable::class.simpleName}"
                    },
            )

        assertEquals(
            "Mapped IllegalStateException",
            provider.errorText(IllegalStateException(), FallbackKey),
        )
    }

    @Test
    fun `errorText uses fallback text when mapper returns null`() {
        val provider = MapTextProvider(values = mapOf(FallbackKey to "Fallback"))

        assertEquals(
            "Fallback",
            provider.errorText(IllegalStateException("Details"), FallbackKey),
        )
    }

    @Test
    fun `composite text provider uses first provider with matching key`() {
        val featureProvider = MapTextProvider(values = mapOf(TextKey("feature.title") to "Feature"))
        val commonProvider = MapTextProvider(values = mapOf(CommonTextKeys.Retry to "Retry"))
        val provider = CompositeTextProvider(featureProvider, commonProvider)

        assertEquals("Feature", provider.text(TextKey("feature.title")))
        assertEquals("Retry", provider.text(CommonTextKeys.Retry))
    }

    @Test
    fun `composite text provider keeps key fallback for missing key`() {
        val provider = CompositeTextProvider(MapTextProvider(values = emptyMap()))

        assertEquals("missing.key", provider.text(TextKey("missing.key")))
    }

    private fun quantityProvider(): TextProvider =
        MapTextProvider(
            values = emptyMap(),
            quantities =
                mapOf(
                    QuantityKey to
                        QuantityTextTemplates(
                            one = "{0} one",
                            few = "{0} few",
                            many = "{0} many",
                        ),
                ),
        )

    private companion object {
        val FallbackKey = TextKey("fallback")
        val QuantityKey = QuantityTextKey("quantity")
    }
}
