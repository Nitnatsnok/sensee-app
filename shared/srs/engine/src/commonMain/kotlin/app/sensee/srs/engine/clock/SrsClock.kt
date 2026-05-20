package app.sensee.srs.engine.clock

import kotlin.time.Instant

public fun interface SrsClock {
    public fun now(): Instant
}
