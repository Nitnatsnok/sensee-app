package app.sensee.lexicon.domain

/**
 * Confirm-gate (ADR-001): a sense is fit to leave [SenseStatus.Draft] for
 * [SenseStatus.Confirmed] only when it carries a non-blank [Sense.translation]
 * AND at least one [Sense.contextualApplications] — a confirmed sense exists in
 * use, so the contextual example is a model invariant, not an optional field.
 *
 * A pure rule on the (frozen) [Sense] shape, kept in `lexicon/domain` so every
 * writer gates against one source of truth. The canonical sense store enforces
 * it on the write border ([SenseWriteRepository.upsert] with
 * [SenseStatus.Confirmed]); the editor uses it to decide when a draft is ready.
 */
public fun Sense.isConfirmable(): Boolean = translation.isNotBlank() && contextualApplications.isNotEmpty()

/**
 * Asserts the confirm-gate and returns the sense unchanged, or fails with a
 * descriptive message naming the missing requirement.
 */
public fun Sense.requireConfirmable(): Sense {
    require(translation.isNotBlank()) {
        "A confirmable sense needs a non-blank translation"
    }
    require(contextualApplications.isNotEmpty()) {
        "A confirmable sense needs at least one contextual application"
    }
    return this
}
