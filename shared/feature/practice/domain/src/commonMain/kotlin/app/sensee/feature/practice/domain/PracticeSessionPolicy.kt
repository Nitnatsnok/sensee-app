package app.sensee.feature.practice.domain

import app.sensee.srs.core.model.SrsCardSnapshot
import app.sensee.srs.core.model.SrsCardState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * How a practice session projects an FSRS-scheduled card into a presentation queue.
 *
 * FSRS is the single source of truth for *scheduling* (ADR-004). This policy never schedules;
 * it only decides how an already-scheduled card is *shown* within the current session: whether
 * it returns, how far down the queue, and which side faces the learner.
 */
public object PracticeSessionPolicy {
    /**
     * Safety net so a learner who keeps pressing "Again" cannot loop a single card forever.
     */
    public const val MAX_PRESENTATIONS_PER_CARD: Int = 8

    /**
     * Upper bound on cards pulled into a single "due now" session — a safety net for a large
     * backlog (e.g. after a long absence). The Home «Стоит повторить» widget caps its due count
     * to the same limit and renders it as "N+", so the widget never promises more than one
     * session delivers.
     */
    public const val DUE_SESSION_LIMIT: Int = 60

    /**
     * Whether the just-reviewed card should be shown again later in this same session.
     *
     * A card FSRS leaves in [SrsCardState.Learning] / [SrsCardState.Relearning] is still being
     * acquired and is due within minutes (a learning step), so it belongs back in the session;
     * once FSRS graduates it to [SrsCardState.Review] its next due date is days away and it
     * leaves the session. [MAX_PRESENTATIONS_PER_CARD] caps the in-session loop.
     */
    public fun shouldReinject(
        srs: SrsCardSnapshot,
        shownCount: Int,
    ): Boolean = shownCount < MAX_PRESENTATIONS_PER_CARD && srs.state in IN_SESSION_STATES

    /**
     * How many other cards to show before a reinjected card comes back.
     *
     * FSRS expresses urgency as a due *duration* (≈1 min after "Again", ≈10 min after a
     * learning-step "Good"). A session is only a few minutes of wall-clock time, so that
     * duration barely elapses and a literal due-time queue would never resurface anything. The
     * relative urgency is projected onto a queue distance instead: a short step comes back
     * soon, a longer step further down the deck.
     */
    public fun reinjectionGap(srs: SrsCardSnapshot): Int =
        if ((srs.scheduledInterval ?: Duration.ZERO) <= SHORT_STEP) NEAR_GAP else FAR_GAP

    /**
     * The practice direction for a given showing. Seeding with the card id keeps different
     * cards starting on different sides, while adding the presentation index flips the
     * direction on every revisit, so a card the learner keeps getting wrong is drilled both
     * English -> Russian and Russian -> English.
     */
    public fun frontFor(
        cardId: String,
        presentationIndex: Int,
    ): PracticeCardFront =
        if ((cardId.stableSeed() + presentationIndex) and 1 == 0) {
            PracticeCardFront.English
        } else {
            PracticeCardFront.Russian
        }

    // Not String.hashCode(): it is not contractually equal across KMP targets, so the
    // starting side would differ per platform for the same card.
    private fun String.stableSeed(): Int = fold(0) { acc, c -> acc * 31 + c.code }

    private val IN_SESSION_STATES = setOf(SrsCardState.Learning, SrsCardState.Relearning)
    private const val NEAR_GAP = 3
    private const val FAR_GAP = 8
    private val SHORT_STEP = 2.minutes
}
