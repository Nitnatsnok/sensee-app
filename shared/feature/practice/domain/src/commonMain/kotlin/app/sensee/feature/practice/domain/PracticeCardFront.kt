package app.sensee.feature.practice.domain

/**
 * Which side of a practice card the learner sees first.
 *
 * Selection is per-card so the SRS layer can mix directions inside a session. A user-level override
 * may force one direction; that override is applied above this enum, not here.
 */
public enum class PracticeCardFront {
    English,
    Russian,
}
