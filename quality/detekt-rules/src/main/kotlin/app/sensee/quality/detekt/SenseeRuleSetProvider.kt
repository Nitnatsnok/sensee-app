package app.sensee.quality.detekt

import dev.detekt.api.Config
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class SenseeRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("sensee")

    override fun instance(): RuleSet =
        RuleSet(
            id = ruleSetId,
            rules =
                mapOf<RuleName, (Config) -> Rule>(
                    RuleName("ProfiledLongMethod") to ::ProfiledLongMethod,
                    RuleName("ProfiledCyclomaticComplexMethod") to ::ProfiledCyclomaticComplexMethod,
                    RuleName("ProfiledLongParameterList") to ::ProfiledLongParameterList,
                    RuleName("ForbiddenRunCatching") to ::ForbiddenRunCatching,
                    RuleName("BroadCatchCancellationGuard") to ::BroadCatchCancellationGuard,
                ),
        )
}
