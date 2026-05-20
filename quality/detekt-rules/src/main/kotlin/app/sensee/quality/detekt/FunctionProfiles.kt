package app.sensee.quality.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtWhenExpression

private const val COMPOSABLE = "Composable"
private const val PROVIDES = "Provides"

class ProfiledLongMethod(
    config: Config,
) : Rule(
        config = config,
        description = "Enforces separate method-length budgets for regular Kotlin and @Composable functions.",
    ) {
    private val allowedLines: Int by config(defaultValue = 50)
    private val allowedComposableLines: Int by config(defaultValue = 120)

    override fun visitNamedFunction(function: KtNamedFunction) {
        val actual = function.ownBodyLineCount()
        val allowed = if (function.isComposable()) allowedComposableLines else allowedLines
        if (actual > allowed) {
            val profile = if (function.isComposable()) "@Composable" else "regular Kotlin"
            report(
                Finding(
                    entity = Entity.from(function),
                    message =
                        "$profile function '${function.name ?: "<anonymous>"}' has $actual " +
                            "body lines; allowed is $allowed.",
                ),
            )
        }
        super.visitNamedFunction(function)
    }
}

class ProfiledCyclomaticComplexMethod(
    config: Config,
) : Rule(
        config = config,
        description = "Enforces separate cyclomatic-complexity budgets for regular Kotlin and @Composable functions.",
    ) {
    private val allowedComplexity: Int by config(defaultValue = 10)
    private val allowedComposableComplexity: Int by config(defaultValue = 15)

    override fun visitNamedFunction(function: KtNamedFunction) {
        val actual = function.cyclomaticComplexity()
        val allowed = if (function.isComposable()) allowedComposableComplexity else allowedComplexity
        if (actual > allowed) {
            val profile = if (function.isComposable()) "@Composable" else "regular Kotlin"
            report(
                Finding(
                    entity = Entity.from(function),
                    message =
                        "$profile function '${function.name ?: "<anonymous>"}' has cyclomatic " +
                            "complexity $actual; allowed is $allowed.",
                ),
            )
        }
        super.visitNamedFunction(function)
    }
}

class ProfiledLongParameterList(
    config: Config,
) : Rule(
        config = config,
        description =
            "Enforces separate parameter budgets for regular functions, " +
                "@Composable functions, and profiled constructors.",
    ) {
    private val allowedFunctionParameters: Int by config(defaultValue = 5)
    private val allowedProviderFunctionParameters: Int by config(defaultValue = 6)
    private val allowedComposableFunctionParameters: Int by config(defaultValue = 8)
    private val allowedConstructorParameters: Int by config(defaultValue = 6)
    private val allowedLogicConstructorParameters: Int by config(defaultValue = 8)
    private val allowedDecomposeComponentConstructorParameters: Int by config(defaultValue = 8)
    private val allowedDataClassConstructorParameters: Int by config(defaultValue = 8)
    private val allowedDomainModelConstructorParameters: Int by config(defaultValue = 10)
    private val allowedUiStateConstructorParameters: Int by config(defaultValue = 12)
    private val allowedSnapshotConstructorParameters: Int by config(defaultValue = 12)
    private val allowedComponentTokenConstructorParameters: Int by config(defaultValue = 24)
    private val allowedThemeTokenConstructorParameters: Int by config(defaultValue = 64)
    private val ignoreDefaultParameters: Boolean by config(defaultValue = true)
    private val themeTokenPackagePatterns: List<String> by config(
        defaultValue = listOf("""^app\.sensee\.ui\.designSystem\.theme$"""),
    )
    private val componentTokenPackagePatterns: List<String> by config(
        defaultValue = listOf("""^app\.sensee\.ui\.designSystem\.component\..*"""),
    )
    private val componentTokenClassNamePatterns: List<String> by config(
        defaultValue = listOf(""".*(Colors|Styles)$"""),
    )
    private val snapshotClassNamePatterns: List<String> by config(defaultValue = listOf(""".*Snapshot$"""))
    private val snapshotPackagePatterns: List<String> by config(
        defaultValue = listOf("""^app\.sensee\.core\.tracing(\..*)?$"""),
    )
    private val logicClassNamePatterns: List<String> by config(defaultValue = listOf(""".*Logic$"""))
    private val logicPackagePatterns: List<String> by config(
        defaultValue = listOf(""".*\.presentation\.impl(\..*)?$"""),
    )
    private val logicAnnotationNames: List<String> by config(defaultValue = listOf("AssistedInject"))
    private val decomposeComponentClassNamePatterns: List<String> by config(
        defaultValue = listOf("""Default.*Component$"""),
    )
    private val decomposeComponentPackagePatterns: List<String> by config(
        defaultValue =
            listOf(
                """^app\.sensee\.appShell(\..*)?$""",
                """.*\.presentation\.impl(\..*)?$""",
            ),
    )
    private val decomposeComponentAnnotationNames: List<String> by config(defaultValue = listOf("AssistedInject"))
    private val uiStateClassNamePatterns: List<String> by config(defaultValue = listOf(""".*UiState$"""))
    private val domainModelPackagePatterns: List<String> by config(
        defaultValue = listOf(""".*\.domain(\..*)?$"""),
    )

    private val themeTokenPackageRegexes = themeTokenPackagePatterns.toRegexes()
    private val componentTokenPackageRegexes = componentTokenPackagePatterns.toRegexes()
    private val componentTokenClassNameRegexes = componentTokenClassNamePatterns.toRegexes()
    private val snapshotClassNameRegexes = snapshotClassNamePatterns.toRegexes()
    private val snapshotPackageRegexes = snapshotPackagePatterns.toRegexes()
    private val logicClassNameRegexes = logicClassNamePatterns.toRegexes()
    private val logicPackageRegexes = logicPackagePatterns.toRegexes()
    private val decomposeComponentClassNameRegexes = decomposeComponentClassNamePatterns.toRegexes()
    private val decomposeComponentPackageRegexes = decomposeComponentPackagePatterns.toRegexes()
    private val uiStateClassNameRegexes = uiStateClassNamePatterns.toRegexes()
    private val domainModelPackageRegexes = domainModelPackagePatterns.toRegexes()

    override fun visitNamedFunction(function: KtNamedFunction) {
        val actual = function.valueParameters.countEffectiveParameters()
        val profile = function.parameterProfile()
        val allowed = profile.allowed
        if (actual > allowed) {
            report(
                Finding(
                    entity = Entity.from(function),
                    message =
                        "${profile.name} function '${function.name ?: "<anonymous>"}' " +
                            "has $actual parameters; allowed is $allowed.",
                ),
            )
        }
        super.visitNamedFunction(function)
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val constructor = klass.primaryConstructor ?: return
        val actual = constructor.valueParameters.countEffectiveParameters()
        val profile = klass.constructorParameterProfile()
        val allowed = profile.allowed
        if (actual > allowed) {
            report(
                Finding(
                    entity = Entity.from(constructor),
                    message =
                        "${profile.name} '${klass.name ?: "<anonymous>"}' " +
                            "has $actual parameters; allowed is $allowed.",
                ),
            )
        }
    }

    private fun KtClass.constructorParameterProfile(): ParameterProfile {
        val packageName = containingKtFile.packageFqName.asString()
        val className = name.orEmpty()
        return when {
            packageName.matchesAny(themeTokenPackageRegexes) ->
                ParameterProfile("theme token constructor", allowedThemeTokenConstructorParameters)

            packageName.matchesAny(componentTokenPackageRegexes) &&
                className.matchesAny(componentTokenClassNameRegexes) ->
                ParameterProfile("component token constructor", allowedComponentTokenConstructorParameters)

            className.matchesAny(snapshotClassNameRegexes) || packageName.matchesAny(snapshotPackageRegexes) ->
                ParameterProfile("snapshot constructor", allowedSnapshotConstructorParameters)

            isLogicConstructor(packageName = packageName, className = className) ->
                ParameterProfile("Logic constructor", allowedLogicConstructorParameters)

            isDecomposeComponentConstructor(packageName = packageName, className = className) ->
                ParameterProfile(
                    "Decompose component constructor",
                    allowedDecomposeComponentConstructorParameters,
                )

            className.matchesAny(uiStateClassNameRegexes) ->
                ParameterProfile("UI-state constructor", allowedUiStateConstructorParameters)

            isData() && packageName.matchesAny(domainModelPackageRegexes) ->
                ParameterProfile("domain-model constructor", allowedDomainModelConstructorParameters)

            isData() ->
                ParameterProfile("data-class constructor", allowedDataClassConstructorParameters)

            else ->
                ParameterProfile("constructor", allowedConstructorParameters)
        }
    }

    private fun KtClass.isLogicConstructor(
        packageName: String,
        className: String,
    ): Boolean =
        className.matchesAny(logicClassNameRegexes) &&
            (hasAnyAnnotation(logicAnnotationNames) || packageName.matchesAny(logicPackageRegexes))

    private fun KtClass.isDecomposeComponentConstructor(
        packageName: String,
        className: String,
    ): Boolean =
        className.matchesAny(decomposeComponentClassNameRegexes) &&
            (
                hasAnyAnnotation(decomposeComponentAnnotationNames) ||
                    packageName.matchesAny(decomposeComponentPackageRegexes)
            )

    private fun KtNamedFunction.parameterProfile(): ParameterProfile =
        when {
            isComposable() ->
                ParameterProfile("@Composable", allowedComposableFunctionParameters)

            hasAnnotation(PROVIDES) ->
                ParameterProfile("DI provider", allowedProviderFunctionParameters)

            else ->
                ParameterProfile("regular Kotlin", allowedFunctionParameters)
        }

    private fun List<org.jetbrains.kotlin.psi.KtParameter>.countEffectiveParameters(): Int =
        count { parameter -> !ignoreDefaultParameters || !parameter.hasDefaultValue() }

    private data class ParameterProfile(
        val name: String,
        val allowed: Int,
    )
}

private fun KtNamedFunction.cyclomaticComplexity(): Int {
    val visitor = CyclomaticComplexityVisitor()
    accept(visitor)
    return visitor.complexity
}

private class CyclomaticComplexityVisitor : KtTreeVisitorVoid() {
    var complexity: Int = 1
        private set

    private var rootVisited = false

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (rootVisited) return
        rootVisited = true
        super.visitNamedFunction(function)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        complexity += 1
        super.visitIfExpression(expression)
    }

    override fun visitWhenExpression(expression: KtWhenExpression) {
        complexity += expression.entries.count { !it.isElse }
        super.visitWhenExpression(expression)
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        complexity += 1
        super.visitLoopExpression(loopExpression)
    }

    override fun visitCatchSection(catchClause: KtCatchClause) {
        complexity += 1
        super.visitCatchSection(catchClause)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (expression.operationToken in complexityTokens) {
            complexity += 1
        }
        super.visitBinaryExpression(expression)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        if (expression.calleeExpression?.text in nestingFunctionNames) {
            complexity += 1
        }
        super.visitCallExpression(expression)
    }

    override fun visitParameterList(list: KtParameterList) {
        // Parameter default expressions belong to signature shape, not body complexity.
    }

    private companion object {
        val complexityTokens = setOf(KtTokens.ANDAND, KtTokens.OROR, KtTokens.ELVIS)
        val nestingFunctionNames = setOf("also", "apply", "forEach", "let", "run", "use", "with")
    }
}

private fun KtModifierListOwner.isComposable(): Boolean = hasAnnotation(COMPOSABLE)

private fun KtModifierListOwner.hasAnnotation(name: String): Boolean =
    annotationEntries.any { annotation ->
        annotation.shortName?.asString() == name ||
            annotation.typeReference?.text?.substringAfterLast('.') == name
    }

private fun KtModifierListOwner.hasAnyAnnotation(names: List<String>): Boolean = names.any(::hasAnnotation)

private fun List<String>.toRegexes(): List<Regex> = map(::Regex)

private fun String.matchesAny(regexes: List<Regex>): Boolean = regexes.any { regex -> regex.matches(this) }

private fun KtNamedFunction.ownBodyLineCount(): Int {
    val body = bodyExpression ?: return 0
    val bodyLines = body.text.lineSequence().count()
    val nestedLineCounter =
        object : KtTreeVisitorVoid() {
            var lines: Int = 0

            override fun visitNamedFunction(function: KtNamedFunction) {
                lines += function.text.lineSequence().count()
            }
        }
    body.acceptChildren(nestedLineCounter)
    return bodyLines - nestedLineCounter.lines
}
