package app.sensee.grammar.domain

/**
 * A structured usage-nuance label on a sense (ADR-001, canon in
 * `docs/domain/pos-and-forms.adoc`): the *same* sense constrained in register,
 * region, domain, connotation or temporal status. A meaning-changing nuance
 * is sense segregation, not a label. Open selectional restrictions that
 * cannot be enumerated live in `usageNote`, not here.
 *
 * Sealed; pair validity (axis ⇒ value) is a runtime invariant
 * ([TaxonomyInvariants]) so the inventory can grow without a client update.
 */
public sealed interface UsageValue {
    public val id: String

    public data object Formal : UsageValue {
        override val id: String = "formal"
    }

    public data object Informal : UsageValue {
        override val id: String = "informal"
    }

    public data object Slang : UsageValue {
        override val id: String = "slang"
    }

    public data object Literary : UsageValue {
        override val id: String = "literary"
    }

    public data object NeutralRegister : UsageValue {
        override val id: String = "neutral"
    }

    public data object BritishEnglish : UsageValue {
        override val id: String = "bre"
    }

    public data object AmericanEnglish : UsageValue {
        override val id: String = "ame"
    }

    public data object AustralianEnglish : UsageValue {
        override val id: String = "ause"
    }

    public data object CanadianEnglish : UsageValue {
        override val id: String = "cane"
    }

    public data object Law : UsageValue {
        override val id: String = "law"
    }

    public data object Medicine : UsageValue {
        override val id: String = "medicine"
    }

    public data object ComputingIt : UsageValue {
        override val id: String = "it"
    }

    public data object Science : UsageValue {
        override val id: String = "science"
    }

    public data object Business : UsageValue {
        override val id: String = "business"
    }

    public data object NeutralConnotation : UsageValue {
        override val id: String = "neutral_connotation"
    }

    public data object Approving : UsageValue {
        override val id: String = "approving"
    }

    public data object Disapproving : UsageValue {
        override val id: String = "disapproving"
    }

    public data object Euphemistic : UsageValue {
        override val id: String = "euphemistic"
    }

    public data object Current : UsageValue {
        override val id: String = "current"
    }

    public data object Dated : UsageValue {
        override val id: String = "dated"
    }

    public data object Archaic : UsageValue {
        override val id: String = "archaic"
    }

    public data object Obsolete : UsageValue {
        override val id: String = "obsolete"
    }

    public data class Unknown(
        override val id: String,
    ) : UsageValue

    public companion object {
        public val knownEntries: List<UsageValue> =
            listOf(
                Formal,
                Informal,
                Slang,
                Literary,
                NeutralRegister,
                BritishEnglish,
                AmericanEnglish,
                AustralianEnglish,
                CanadianEnglish,
                Law,
                Medicine,
                ComputingIt,
                Science,
                Business,
                NeutralConnotation,
                Approving,
                Disapproving,
                Euphemistic,
                Current,
                Dated,
                Archaic,
                Obsolete,
            )

        private val BY_ID: Map<String, UsageValue> = knownEntries.associateBy { it.id }

        public fun fromId(id: String): UsageValue = BY_ID[id] ?: Unknown(id)
    }
}

public sealed interface UsageAxis {
    public val id: String

    public data object Register : UsageAxis {
        override val id: String = "register"
    }

    public data object Region : UsageAxis {
        override val id: String = "region"
    }

    public data object Domain : UsageAxis {
        override val id: String = "domain"
    }

    public data object Connotation : UsageAxis {
        override val id: String = "connotation"
    }

    public data object Temporality : UsageAxis {
        override val id: String = "temporality"
    }

    public data class Unknown(
        override val id: String,
    ) : UsageAxis

    public companion object {
        public val knownEntries: List<UsageAxis> =
            listOf(Register, Region, Domain, Connotation, Temporality)

        private val BY_ID: Map<String, UsageAxis> = knownEntries.associateBy { it.id }

        public fun fromId(id: String): UsageAxis = BY_ID[id] ?: Unknown(id)
    }
}

/**
 * A `(axis, value)` usage label. Pair validity is runtime — [resolve] checks
 * it against [TaxonomyInvariants.allowedValuesByAxis] at the AI boundary;
 * direct construction skips validation so backend-valid Unknown pairs flow
 * through.
 */
public data class UsageLabel(
    val axis: UsageAxis,
    val value: UsageValue,
) {
    public companion object {
        /**
         * Boundary resolver. `null` [allowedValuesByAxis] = pass-through; a
         * non-null map drops off-schema pairs (the AI cannot inject values
         * outside the live taxonomy).
         */
        public fun resolve(
            axisId: String,
            valueId: String,
            allowedValuesByAxis: Map<String, Set<String>>? = null,
        ): UsageLabel? {
            if (allowedValuesByAxis != null) {
                val allowed = allowedValuesByAxis[axisId] ?: return null
                if (valueId !in allowed) return null
            }
            return UsageLabel(UsageAxis.fromId(axisId), UsageValue.fromId(valueId))
        }
    }
}
