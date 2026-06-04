package app.sensee.feature.practice.domain

import app.sensee.srs.core.model.SrsCardSnapshot

/**
 * Result of submitting a review: the card's refreshed SRS state. Practice owns
 * this — a session projects re-injection from [srs] — and a caller that also needs
 * the card's display content re-reads it through Library's catalog contract. The
 * card id rides on [SrsCardSnapshot.id], so the outcome is self-describing.
 */
public data class ReviewOutcome(
    val srs: SrsCardSnapshot,
)
