package app.sensee.core.observability.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.recordException

internal object FirebaseCrashReporter : CrashReporter {
    override fun recordException(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        if (attributes.isEmpty()) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
            return
        }
        FirebaseCrashlytics.getInstance().recordException(throwable) {
            attributes.forEach { (attributeKey, attributeValue) -> key(attributeKey, attributeValue) }
        }
    }
}

internal actual fun platformCrashReporter(): CrashReporter = FirebaseCrashReporter
