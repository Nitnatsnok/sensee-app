package app.sensee.feature.vocabularyEditor.domain.usecase

import app.sensee.core.observability.diagnostics.AppDiagnostics

/**
 * Records a backend taxonomy value the client does not know yet (a sealed
 * `Unknown(...)` branch from `shared/grammar/domain`). Feature-scoped — the
 * tag and wording are vocabulary-capture-specific; lift to shared when a
 * second caller appears.
 */
public fun AppDiagnostics.warnUnknownTaxonomyValue(
    field: String,
    id: String,
) {
    logger.tag("AiEnrichment").warn {
        "Backend returned an unknown $field id \"$id\" — the client taxonomy may be out of date."
    }
}
