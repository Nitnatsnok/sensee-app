package app.sensee.verification.core

import kotlin.test.Test
import kotlin.test.assertFalse

class LicensePolicyTest {
    @Test
    fun `defaults are the most restrictive so a misconfigured adapter is safe`() {
        val policy = LicensePolicy()

        // A new adapter that forgets to declare its policy must not be able to
        // feed an LLM with snippets or cache content locally.
        assertFalse(policy.storeContentAllowed)
        assertFalse(policy.usableAsLlmContext)
    }
}
