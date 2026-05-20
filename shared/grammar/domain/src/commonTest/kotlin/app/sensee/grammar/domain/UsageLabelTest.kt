package app.sensee.grammar.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UsageLabelTest {
    @Test
    fun `a value is valid only for its own axis`() {
        val label = UsageLabel(UsageAxis.Temporality, UsageValue.Archaic)

        assertEquals(UsageAxis.Temporality, label.axis)
        assertEquals(UsageValue.Archaic, label.value)
    }

    @Test
    fun `an off-axis pair cannot be constructed`() {
        assertFailsWith<IllegalArgumentException> {
            UsageLabel(UsageAxis.Register, UsageValue.Archaic)
        }
    }

    @Test
    fun `resolve maps axis-scoped ids and drops unknown or off-axis pairs`() {
        assertEquals(
            UsageLabel(UsageAxis.Register, UsageValue.Formal),
            UsageLabel.resolve("register", "formal"),
        )
        assertNull(UsageLabel.resolve("register", "archaic"))
        assertNull(UsageLabel.resolve("nope", "formal"))
    }
}
