// Cohesive Konsist guard kit — scopes, path/text helpers and the shared
// comment/string lexer belong together (see object KDoc); splitting to satisfy
// the per-file function threshold would scatter tightly-coupled helpers.
@file:Suppress("TooManyFunctions")

package app.sensee.quality.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.provider.KoFullyQualifiedNameProvider
import com.lemonappdev.konsist.api.provider.KoPathProvider
import java.io.File

/**
 * Shared kit for the Konsist guard suite: scopes, path/text helpers, and the
 * comment/string lexer. The guards themselves live in area subpackages so the
 * suite is transparent by concern:
 *
 * - `..konsist.structure`  — package/module/feature layout and naming.
 * - `..konsist.layering`   — layer boundaries and dependency direction.
 * - `..konsist.hygiene`    — placeholder/empty files, module presence.
 * - `..konsist.buildscript`— Gradle build-script rules.
 * - `..konsist.runtime`    — runtime-safety/API discipline (StateFlow
 *   mutation, Decompose Value scope, direct system-clock reads). Bans on
 *   method call sites (`runBlocking`, SqlDelight `executeAs*`, …) live in
 *   detekt's TR-resolved `style.ForbiddenMethodCall`.
 *
 * Adding a guard: put it in the matching area package; reuse these helpers.
 */
internal object KonsistTestSupport {
    val sharedScope = Konsist.scopeFromDirectory("shared")
    val featureScope = Konsist.scopeFromDirectory("shared/feature")

    // Host apps (Android/desktop/iOS/web entry points). Runtime-safety guards
    // scan this too: hosts are where drivers/clocks/scopes get constructed.
    val appsScope = Konsist.scopeFromDirectory("apps")

    val featurePresentationSourceRegex =
        Regex("""shared/feature/([^/]+)/presentation/(api|impl|navigation-api)/src/[^/]+/kotlin/""")

    val featurePresentationImplRegex =
        Regex("""shared/feature/[^/]+/presentation/impl/""")

    val featureSourceRegex =
        Regex("""shared/feature/([^/]+)/[^/]+(?:/[^/]+)?/src/[^/]+/kotlin/""")

    val featureDataSourceRegex =
        Regex("""shared/feature/([^/]+)/data/src/[^/]+/kotlin/""")

    // Matches every shared/<area>/domain/src/<...>/kotlin/ outside the feature
    // tree — e.g. shared/grammar/domain, shared/ai/core, shared/srs/core. Used to
    // enforce "no domain depends on any data package" symmetrically.
    val sharedDomainSourceRegex =
        Regex("""shared/(?!feature/)[^/]+/(?:domain|core)/src/[^/]+/kotlin/""")

    val metroAnnotationRegex =
        Regex("""@(AssistedInject|AssistedFactory|ContributesBinding)\b""")

    // Konsist-classified production files. `scopeFromProduction()` correctly
    // splits every KMP source set (commonMain/androidMain/iosMain/jvmMain/
    // jsMain/wasmJsMain/webMain/nativeMain) and apps `src/main`, and excludes
    // commonTest/jvmTest.
    val productionProjectPaths: Set<String> =
        Konsist
            .scopeFromProduction()
            .files
            .map { it.projectPath.normalizePath() }
            .toSet()

    val featurePackageRegex =
        Regex("""app\.sensee\.feature\.([^.]+)\.""")
}

internal fun assertNoViolations(violations: List<String>) {
    if (violations.isNotEmpty()) {
        throw AssertionError(violations.joinToString(separator = "\n", prefix = "\n"))
    }
}

internal fun violation(
    subject: String,
    message: String,
): String = "$subject -> $message"

internal fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.normalizePath()

internal fun String.normalizePath(): String = replace('\\', '/').trimStart('/')

internal fun String?.matchesPackagePrefix(expectedPrefix: String): Boolean =
    this == expectedPrefix || this?.startsWith("$expectedPrefix.") == true

/** True if [this] normalized project path is a Konsist-classified production file. */
internal fun String.isProductionSourcePath(): Boolean = normalizePath() in KonsistTestSupport.productionProjectPaths

/**
 * Converts a kebab-case identifier to camelCase. Used to map module path
 * segments to package segments (e.g. `vocabulary-editor` → `vocabularyEditor`,
 * `navigation-api` → `navigationApi`).
 */
internal fun String.kebabToCamel(): String =
    split('-')
        .filter { it.isNotEmpty() }
        .mapIndexed { index, part ->
            if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")

internal fun KoFileDeclaration.featurePackageName(): String? =
    KonsistTestSupport.featureSourceRegex
        .find(normalizedProjectPath())
        ?.groupValues
        ?.get(1)
        ?.kebabToCamel()

internal fun KoImportDeclaration.importedPath(): String =
    text
        .lineSequence()
        .first()
        .removePrefix("import ")
        .substringBefore(" as ")
        .trim()

internal fun KoPathProvider.normalizedProjectPath(): String = projectPath.normalizePath()

internal fun KoFullyQualifiedNameProvider.qualifiedDisplayName(): String = fullyQualifiedName ?: this.toString()

internal fun KoFileDeclaration.strippedText(): String = text.withoutCommentsAndStringLiterals()

/**
 * Production Kotlin files across `shared` and host `apps`. Runtime-safety
 * guards (StateFlow mutation, Decompose Value scope, system-clock reads)
 * apply everywhere code runs, not just shared modules.
 */
internal fun productionScopeFiles(): List<KoFileDeclaration> =
    (KonsistTestSupport.sharedScope.files + KonsistTestSupport.appsScope.files)
        .filter { it.normalizedProjectPath().isProductionSourcePath() }

/**
 * The repo root, supplied by the konsist test task as the `projectRoot` system
 * property (Konsist's own path APIs are file-relative, so filesystem walks need
 * an absolute anchor). Fails loudly if the task wiring is missing.
 */
internal fun konsistProjectRoot(): File =
    File(
        System.getProperty("projectRoot")
            ?: error("System property 'projectRoot' is not set; check the konsist test task wiring"),
    )

internal fun collectModuleBuildRoots(root: File): List<File> =
    root
        .walkTopDown()
        .filter { it.isFile && it.name == "build.gradle.kts" }
        .map { it.parentFile }
        .toList()

/**
 * If a comment or string/char literal begins at [i], returns the index just
 * past it; otherwise -1 (the char at [i] is code). Kotlin string/comment
 * nesting is genuinely ambiguous to a regex (a `//` inside a string, a `"`
 * inside a comment), so this is a small shared single-pass lexer.
 */
private fun String.tokenSkipEnd(i: Int): Int {
    val window = substring(i, (i + 3).coerceAtMost(length))
    return when {
        window.startsWith("//") -> skipLineComment(i)
        window.startsWith("/*") -> skipBlockComment(i)
        window == "\"\"\"" -> skipRawString(i)
        this[i] == '"' -> skipQuoted(i, '"')
        this[i] == '\'' -> skipQuoted(i, '\'')
        else -> -1
    }
}

/**
 * Drops comments and string/char literals entirely. Use when only token
 * presence matters and source offsets do not (e.g. `containsMatchIn`).
 */
internal fun String.withoutCommentsAndStringLiterals(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val end = tokenSkipEnd(i)
        if (end < 0) {
            out.append(this[i])
            i++
        } else {
            i = end.coerceAtMost(length)
        }
    }
    return out.toString()
}

/**
 * Blanks comments and string/char literals to spaces while preserving total
 * length and every newline, so character offsets and line numbers in the
 * result still map onto the original source. Use when a scan needs positions
 * (brace matching, line-number reporting) but must ignore braces/tokens that
 * live inside comments or strings.
 */
internal fun String.maskCommentsAndStringLiterals(): String {
    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val end = tokenSkipEnd(i)
        if (end < 0) {
            out.append(this[i])
            i++
        } else {
            val stop = end.coerceAtMost(length)
            for (j in i until stop) out.append(if (this[j] == '\n') '\n' else ' ')
            i = stop
        }
    }
    return out.toString()
}

private fun String.skipLineComment(from: Int): Int {
    var i = from
    while (i < length && this[i] != '\n') i++
    return i
}

private fun String.skipBlockComment(from: Int): Int {
    var i = from + 2
    while (i < length && !startsWith("*/", i)) i++
    return (i + 2).coerceAtMost(length)
}

private fun String.skipRawString(from: Int): Int {
    var i = from + 3
    while (i < length && !startsWith("\"\"\"", i)) i++
    return (i + 3).coerceAtMost(length)
}

private fun String.skipQuoted(
    from: Int,
    quote: Char,
): Int {
    var i = from + 1
    while (i < length && this[i] != quote) {
        i += if (this[i] == '\\') 2 else 1
    }
    return (i + 1).coerceAtMost(length)
}
