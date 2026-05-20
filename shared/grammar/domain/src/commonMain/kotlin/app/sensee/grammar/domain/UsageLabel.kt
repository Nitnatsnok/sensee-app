package app.sensee.grammar.domain

/**
 * A structured usage-nuance label on a sense (ADR-001, canon in
 * `docs/pos-and-forms.adoc`): the *same* sense constrained in register,
 * region, domain, connotation or temporal status. A meaning-changing nuance
 * is sense segregation, not a label. Open selectional restrictions that
 * cannot be enumerated live in `usageNote`, not here.
 *
 * Closed value sets per axis, validated like [GrammarTag]: a pair outside the
 * axis is invalid and cannot be constructed; [resolve] drops an unknown/invalid
 * pair at the AI boundary.
 */
public enum class UsageValue(
    public val id: String,
) {
    Formal("formal"),
    Informal("informal"),
    Slang("slang"),
    Literary("literary"),
    NeutralRegister("neutral"),
    BritishEnglish("bre"),
    AmericanEnglish("ame"),
    AustralianEnglish("ause"),
    CanadianEnglish("cane"),
    Law("law"),
    Medicine("medicine"),
    ComputingIt("it"),
    Science("science"),
    Business("business"),
    NeutralConnotation("neutral_connotation"),
    Positive("positive_connotation"),
    Pejorative("pejorative"),
    Euphemistic("euphemistic"),
    Current("current"),
    Dated("dated"),
    Archaic("archaic"),
    Obsolete("obsolete"),
}

public enum class UsageAxis(
    public val id: String,
    public val allowedValues: Set<UsageValue>,
) {
    Register(
        "register",
        setOf(
            UsageValue.Formal,
            UsageValue.Informal,
            UsageValue.Slang,
            UsageValue.Literary,
            UsageValue.NeutralRegister,
        ),
    ),
    Region(
        "region",
        setOf(
            UsageValue.BritishEnglish,
            UsageValue.AmericanEnglish,
            UsageValue.AustralianEnglish,
            UsageValue.CanadianEnglish,
        ),
    ),
    Domain(
        "domain",
        setOf(
            UsageValue.Law,
            UsageValue.Medicine,
            UsageValue.ComputingIt,
            UsageValue.Science,
            UsageValue.Business,
        ),
    ),
    Connotation(
        "connotation",
        setOf(
            UsageValue.NeutralConnotation,
            UsageValue.Positive,
            UsageValue.Pejorative,
            UsageValue.Euphemistic,
        ),
    ),
    Temporality(
        "temporality",
        setOf(UsageValue.Current, UsageValue.Dated, UsageValue.Archaic, UsageValue.Obsolete),
    ),
    ;

    public companion object {
        public fun fromId(id: String): UsageAxis? = entries.firstOrNull { it.id == id }
    }
}

public data class UsageLabel(
    val axis: UsageAxis,
    val value: UsageValue,
) {
    init {
        require(value in axis.allowedValues) {
            "Usage value ${value.id} is not valid for axis ${axis.id}"
        }
    }

    public companion object {
        /** Resolves a (axisId, valueId) pair, null when unknown or off-axis (dropped at the boundary). */
        public fun resolve(
            axisId: String,
            valueId: String,
        ): UsageLabel? {
            val axis = UsageAxis.fromId(axisId) ?: return null
            val value = axis.allowedValues.firstOrNull { it.id == valueId } ?: return null
            return UsageLabel(axis, value)
        }
    }
}
