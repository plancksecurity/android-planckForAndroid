package security.planck.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class ManageableSetting<SETTING_TYPE> @JvmOverloads constructor(
    @SerialName("value") var value: SETTING_TYPE,
    @SerialName("locked") val locked: Boolean = false,
) {
    fun toManageableMdmEntry(): ManageableSettingMdmEntry<SETTING_TYPE> =
        ManageableSettingMdmEntry(value = value, locked = locked)
}

fun deserializeBooleanManageableSetting(json: String?): ManageableSetting<Boolean>? {
    return when (json) {
        "true" -> ManageableSetting(value = true, locked = false)
        "false" -> ManageableSetting(value = false, locked = false)
        null -> ManageableSetting(value = true, locked = false)
        else -> Json.decodeFromString(json)
    }
}

fun serializeBooleanManageableSetting(setting: ManageableSetting<Boolean>?): String? {
    return setting?.let { Json.encodeToString(setting) }
}

fun deserializeStringManageableSetting(json: String?): ManageableSetting<String>? {
    return json?.let { Json.decodeFromString(json) }
}

fun serializeStringManageableSetting(setting: ManageableSetting<String>?): String? {
    return setting?.let { Json.encodeToString(setting) }
}

fun deserializeLongManageableSetting(json: String?): ManageableSetting<Long>? {
    return json?.let { Json.decodeFromString(json) }
}

fun serializeLongManageableSetting(setting: ManageableSetting<Long>?): String? {
    return setting?.let { Json.encodeToString(setting) }
}

fun deserializeIntManageableSetting(json: String?): ManageableSetting<Int>? {
    return json?.let { Json.decodeFromString(json) }
}

fun serializeIntManageableSetting(setting: ManageableSetting<Int>?): String? {
    return setting?.let { Json.encodeToString(setting) }
}