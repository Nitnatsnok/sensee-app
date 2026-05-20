package app.sensee.srs.testKit

import app.sensee.srs.engine.clock.SrsClock
import kotlin.time.Instant

public class FixedSrsClock(
    initialNow: Instant,
) : SrsClock {
    public var currentNow: Instant = initialNow
        private set

    override fun now(): Instant = currentNow

    public fun setNow(value: Instant) {
        currentNow = value
    }

    public fun advanceTo(value: Instant) {
        require(value >= currentNow) {
            "Cannot move FixedSrsClock backwards"
        }

        currentNow = value
    }
}
