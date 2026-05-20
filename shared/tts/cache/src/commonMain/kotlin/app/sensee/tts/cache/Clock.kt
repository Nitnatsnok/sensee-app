package app.sensee.tts.cache

import kotlin.time.Clock as KotlinClock

internal fun interface EpochMillisClock {
    fun nowEpochMillis(): Long
}

internal object SystemEpochMillisClock : EpochMillisClock {
    override fun nowEpochMillis(): Long = KotlinClock.System.now().toEpochMilliseconds()
}
