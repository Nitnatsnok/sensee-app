package app.sensee.verification.integration

import app.sensee.verification.core.contract.CefrLevelProvider
import app.sensee.verification.core.contract.FrequencyProvider
import app.sensee.verification.core.contract.LexicalEntryLookup
import app.sensee.verification.core.contract.LexicalFamilyProvider
import app.sensee.verification.core.contract.SenseInventoryProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Aggregates every sub-contract Set Metro provides into one object so
 * [RoutingLexicalVerifier] stays under detekt's parameter cap. Adding a new
 * sub-contract is a field addition here plus a `@Provides` in
 * [VerificationIntegrationProviders] — no change in the routing class.
 */
@SingleIn(AppScope::class)
@Inject
public class VerificationContributors(
    public val entryLookups: Set<LexicalEntryLookup>,
    public val frequencyProviders: Set<FrequencyProvider>,
    public val cefrProviders: Set<CefrLevelProvider>,
    public val senseInventoryProviders: Set<SenseInventoryProvider>,
    public val familyProviders: Set<LexicalFamilyProvider>,
)
