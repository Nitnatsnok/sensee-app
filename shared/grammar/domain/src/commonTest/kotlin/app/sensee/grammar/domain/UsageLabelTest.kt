package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UsageLabelTest {
    private val invariants =
        mapOf(
            "register" to setOf("formal", "informal"),
            "temporality" to setOf("current", "archaic"),
        )

    @Test
    fun `direct construction carries the pair without enforcing the runtime invariant`() {
        val label = UsageLabel(UsageAxis.Temporality, UsageValue.Archaic)

        assertEquals(UsageAxis.Temporality, label.axis)
        assertEquals(UsageValue.Archaic, label.value)
    }

    @Test
    fun `resolve maps axis-scoped ids and drops unknown or off-axis pairs`() {
        assertEquals(
            UsageLabel(UsageAxis.Register, UsageValue.Formal),
            UsageLabel.resolve("register", "formal", invariants),
        )
        assertNull(UsageLabel.resolve("register", "archaic", invariants))
        assertNull(UsageLabel.resolve("nope", "formal", invariants))
    }

    @Test
    fun `null invariants pass any pair through and resolve unknown ids to Unknown`() {
        val label = UsageLabel.resolve("brand_new_axis", "brand_new_value", allowedValuesByAxis = null)

        assertEquals(
            UsageLabel(UsageAxis.Unknown("brand_new_axis"), UsageValue.Unknown("brand_new_value")),
            label,
        )
    }
}
