package app.sensee.verification.languagetool

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of the `/v2/check` success response — the fields the adapter
 * needs. LT returns more (sentences, software, warnings, language) which we
 * ignore via `ignoreUnknownKeys = true`.
 */
@Serializable
internal data class LanguageToolCheckResponse(
    @SerialName("matches") val matches: List<LanguageToolMatchDto> = emptyList(),
)

@Serializable
internal data class LanguageToolMatchDto(
    @SerialName("message") val message: String = "",
    @SerialName("shortMessage") val shortMessage: String = "",
    @SerialName("offset") val offset: Int = 0,
    @SerialName("length") val length: Int = 0,
    @SerialName("replacements") val replacements: List<LanguageToolReplacementDto> = emptyList(),
    @SerialName("rule") val rule: LanguageToolRuleDto? = null,
)

@Serializable
internal data class LanguageToolReplacementDto(
    @SerialName("value") val value: String = "",
)

@Serializable
internal data class LanguageToolRuleDto(
    @SerialName("id") val id: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("issueType") val issueType: String = "",
    @SerialName("category") val category: LanguageToolCategoryDto? = null,
)

@Serializable
internal data class LanguageToolCategoryDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
)
