package app.sensee.quality.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeReference

class ForbiddenRunCatching(
    config: Config,
) : Rule(
        config = config,
        description =
            "Forbids runCatching so cancellation and fallback behavior stay explicit " +
                "at each failure boundary.",
    ),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.matchesRunCatching()) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message =
                        "Use explicit try/catch instead of runCatching; rethrow " +
                            "CancellationException when the block can suspend.",
                ),
            )
        }
        super.visitCallExpression(expression)
    }

    private fun KtCallExpression.matchesRunCatching(): Boolean =
        matchesResolvedCallable { fqName -> fqName == KOTLIN_RUN_CATCHING }

    private companion object {
        const val KOTLIN_RUN_CATCHING = "kotlin.runCatching"
    }
}

class BroadCatchCancellationGuard(
    config: Config,
) : Rule(
        config = config,
        description =
            "Requires broad catch clauses to preserve coroutine cancellation by " +
                "rethrowing CancellationException.",
    ),
    RequiresAnalysisApi {
    override fun visitTryExpression(expression: KtTryExpression) {
        if (!expression.isInsideCancellationSensitiveContext()) {
            super.visitTryExpression(expression)
            return
        }
        var cancellationCatchSeen = false
        expression.catchClauses.forEach { catchClause ->
            when {
                catchClause.catchesCancellationException() -> cancellationCatchSeen = true
                catchClause.catchesBroadFailure() -> {
                    if (!cancellationCatchSeen && !catchClause.rethrowsCaughtValue()) {
                        report(
                            Finding(
                                entity = Entity.from(catchClause),
                                message =
                                    "Broad catch must either be preceded by " +
                                        "catch (CancellationException) or rethrow the caught value.",
                            ),
                        )
                    }
                }
            }
        }
        super.visitTryExpression(expression)
    }

    private fun KtCatchClause.catchesCancellationException(): Boolean =
        matchesCaughtType(CANCELLATION_EXCEPTION_FQ_NAMES)

    private fun KtCatchClause.catchesBroadFailure(): Boolean = matchesCaughtType(BROAD_FAILURE_FQ_NAMES)

    private fun KtCatchClause.matchesCaughtType(fqNames: Set<String>): Boolean {
        val typeReference = catchParameter?.typeReference ?: return false
        return typeReference.matchesResolvedType(fqNames)
    }

    private fun KtCatchClause.rethrowsCaughtValue(): Boolean {
        val parameterName = catchParameter?.name ?: return false
        val visitor =
            object : KtTreeVisitorVoid() {
                var rethrows = false

                override fun visitThrowExpression(expression: KtThrowExpression) {
                    if (expression.thrownExpression?.text == parameterName) {
                        rethrows = true
                    }
                    super.visitThrowExpression(expression)
                }
            }
        catchBody?.accept(visitor)
        return visitor.rethrows
    }

    private fun KtTryExpression.isInsideCancellationSensitiveContext(): Boolean =
        isInsideSuspendFunction() ||
            isInsideSuspendLambda() ||
            isInsideExpectedSuspendLambda() ||
            isInsideCoroutineBoundaryLambda()

    private fun KtTryExpression.isInsideSuspendFunction(): Boolean =
        (
            generateSequence(parent) { element -> element.parent }
                .filterIsInstance<KtNamedFunction>()
                .firstOrNull()
                ?.hasModifier(KtTokens.SUSPEND_KEYWORD)
        ) == true

    private fun KtTryExpression.isInsideSuspendLambda(): Boolean =
        generateSequence(parent) { element -> element.parent }
            .filterIsInstance<KtFunctionLiteral>()
            .any { functionLiteral ->
                functionLiteral.modifierList?.hasModifier(KtTokens.SUSPEND_KEYWORD) == true
            }

    private fun KtTryExpression.isInsideExpectedSuspendLambda(): Boolean =
        generateSequence(parent) { element -> element.parent }
            .filterIsInstance<KtFunctionLiteral>()
            .any { functionLiteral -> functionLiteral.hasSuspendExpectedType() }

    private fun KtFunctionLiteral.hasSuspendExpectedType(): Boolean {
        val lambdaExpression = parent as? KtLambdaExpression ?: this
        return analyze(lambdaExpression) {
            lambdaExpression.expectedType.isSuspendFunctionType()
        }
    }

    private fun KtTryExpression.isInsideCoroutineBoundaryLambda(): Boolean =
        generateSequence(parent) { element -> element.parent }
            .filterIsInstance<KtCallExpression>()
            .any { call -> call.matchesCancellationSensitiveCall() }

    private fun KtCallExpression.matchesCancellationSensitiveCall(): Boolean =
        matchesResolvedCallable { fqName -> fqName in CANCELLATION_SENSITIVE_CALL_FQ_NAMES }

    private companion object {
        val CANCELLATION_EXCEPTION_FQ_NAMES =
            setOf(
                "java.util.concurrent.CancellationException",
                "kotlin.coroutines.cancellation.CancellationException",
                "kotlinx.coroutines.CancellationException",
            )
        val BROAD_FAILURE_FQ_NAMES =
            setOf(
                "java.lang.Exception",
                "java.lang.Throwable",
                "kotlin.Exception",
                "kotlin.Throwable",
            )
        val CANCELLATION_SENSITIVE_CALL_FQ_NAMES =
            setOf(
                "kotlinx.coroutines.async",
                "kotlinx.coroutines.coroutineScope",
                "kotlinx.coroutines.launch",
                "kotlinx.coroutines.supervisorScope",
                "kotlinx.coroutines.withContext",
                "kotlinx.coroutines.withTimeout",
                "kotlinx.coroutines.withTimeoutOrNull",
                "kotlinx.coroutines.channels.produce",
                "kotlinx.coroutines.flow.callbackFlow",
                "kotlinx.coroutines.flow.channelFlow",
                "kotlinx.coroutines.flow.flow",
            )
    }
}

private fun KtCallExpression.matchesResolvedCallable(predicate: (String) -> Boolean): Boolean =
    resolvedCallableSymbol()
        ?.callableId
        ?.asSingleFqName()
        ?.asString()
        ?.let(predicate)
        ?: false

@OptIn(KaExperimentalApi::class)
private fun KtCallExpression.resolvedCallableSymbol(): KaCallableSymbol? =
    analyze(this) {
        this@resolvedCallableSymbol.resolveSymbol()
    }

private fun KtTypeReference.matchesResolvedType(fqNames: Set<String>): Boolean =
    resolvedTypeFqNames()
        .any { fqName -> fqName in fqNames }

private fun KtTypeReference.resolvedTypeFqNames(): Set<String> =
    analyze(this) {
        this@resolvedTypeFqNames.type.classFqNames()
    }

private fun KaType.classFqNames(): Set<String> =
    buildSet {
        addClassFqName(this@classFqNames)
        abbreviation?.let { typeAlias -> addClassFqName(typeAlias) }
    }

private fun KaType?.isSuspendFunctionType(): Boolean = (this as? KaFunctionType)?.isSuspend == true

private fun MutableSet<String>.addClassFqName(type: KaType) {
    val classType = type as? KaClassType ?: return
    add(classType.classId.asSingleFqName().asString())
}
