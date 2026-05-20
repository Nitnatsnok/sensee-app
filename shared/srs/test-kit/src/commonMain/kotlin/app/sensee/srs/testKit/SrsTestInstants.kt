package app.sensee.srs.testKit

import kotlin.time.Instant

public object SrsTestInstants {
    public val Base: Instant =
        Instant.parse("2026-04-29T10:00:00Z")

    public val NextDay: Instant =
        Instant.parse("2026-04-30T10:00:00Z")

    public val AfterThreeDays: Instant =
        Instant.parse("2026-05-02T10:00:00Z")

    public val AfterWeek: Instant =
        Instant.parse("2026-05-06T10:00:00Z")
}
