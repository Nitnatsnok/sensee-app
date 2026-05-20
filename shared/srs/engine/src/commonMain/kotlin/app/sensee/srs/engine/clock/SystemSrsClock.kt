package app.sensee.srs.engine.clock

import kotlin.time.Clock
import kotlin.time.Instant

public object SystemSrsClock : SrsClock {
    override fun now(): Instant = Clock.System.now()
}
