package app.sensee.settings.data

import app.sensee.core.database.User_setting
import app.sensee.settings.domain.UserSettingsCategory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Encodes typed values into [UserSettingEntry] rows and decodes them back.
 * Pulled out of the repository so the latter stays focused on read/update
 * orchestration; the per-type round-trip lives here.
 */
internal class SettingsRowCodec(
    private val json: Json,
) {
    fun entry(
        key: SettingKey,
        value: String?,
    ): UserSettingEntry =
        UserSettingEntry(
            category = key.category,
            key = key.key,
            valueJson = json.encodeToString(String.serializer().nullable, value),
        )

    fun entry(
        key: SettingKey,
        value: Boolean,
    ): UserSettingEntry =
        UserSettingEntry(
            category = key.category,
            key = key.key,
            valueJson = json.encodeToString(Boolean.serializer(), value),
        )

    fun entry(
        key: SettingKey,
        value: Int,
    ): UserSettingEntry =
        UserSettingEntry(
            category = key.category,
            key = key.key,
            valueJson = json.encodeToString(Int.serializer(), value),
        )

    fun entry(
        key: SettingKey,
        value: List<String>,
    ): UserSettingEntry =
        UserSettingEntry(
            category = key.category,
            key = key.key,
            valueJson = json.encodeToString(ListSerializer(String.serializer()), value),
        )

    fun stringValue(
        rows: Map<UserSettingId, User_setting>,
        settingKey: SettingKey,
        defaultValue: String?,
    ): String? =
        settingKey.rawValue(rows)?.let { valueJson ->
            decodeOrNull { json.decodeFromString(String.serializer().nullable, valueJson) }
        } ?: defaultValue

    fun booleanValue(
        rows: Map<UserSettingId, User_setting>,
        settingKey: SettingKey,
        defaultValue: Boolean,
    ): Boolean =
        settingKey.rawValue(rows)?.let { valueJson ->
            decodeOrNull { json.decodeFromString(Boolean.serializer(), valueJson) }
        } ?: defaultValue

    fun intValue(
        rows: Map<UserSettingId, User_setting>,
        settingKey: SettingKey,
        defaultValue: Int,
    ): Int =
        settingKey.rawValue(rows)?.let { valueJson ->
            decodeOrNull { json.decodeFromString(Int.serializer(), valueJson) }
        } ?: defaultValue

    fun stringListValue(
        rows: Map<UserSettingId, User_setting>,
        settingKey: SettingKey,
        defaultValue: List<String>,
    ): List<String> =
        settingKey.rawValue(rows)?.let { valueJson ->
            decodeOrNull { json.decodeFromString(ListSerializer(String.serializer()), valueJson) }
        } ?: defaultValue

    inline fun <reified T : Enum<T>> enumValue(
        rows: Map<UserSettingId, User_setting>,
        settingKey: SettingKey,
        defaultValue: T,
    ): T =
        stringValue(rows, settingKey, null)
            ?.let { value -> enumValues<T>().firstOrNull { it.name == value } }
            ?: defaultValue
}

internal data class UserSettingId(
    val category: String,
    val key: String,
)

internal sealed interface SettingKey {
    val category: UserSettingsCategory
    val key: String
}

internal fun SettingKey.rawValue(rows: Map<UserSettingId, User_setting>): String? =
    rows[UserSettingId(category = category.storageName, key = key)]?.value_json

private fun <T> decodeOrNull(block: () -> T): T? =
    try {
        block()
    } catch (_: SerializationException) {
        null
    }
