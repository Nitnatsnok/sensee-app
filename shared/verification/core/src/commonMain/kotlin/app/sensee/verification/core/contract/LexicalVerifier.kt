package app.sensee.verification.core.contract

/**
 * The lexical verification seam. Features depend only on this interface —
 * never on a dictionary SDK or asset format. The umbrella aggregates the
 * narrower sub-contracts (lookup, sense inventory, frequency, CEFR, example
 * quality, pronunciation, family) into one report so a feature use case can
 * read it as a single coherent answer.
 *
 * Availability is first-class (mirrors ADR-005 for the AI seam): partial or
 * unavailable verification is a normal state the orchestrator renders or
 * routes around, not an exception.
 */
public interface LexicalVerifier {
    public suspend fun verify(query: LexicalVerificationQuery): LexicalVerificationReport
}
