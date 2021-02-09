package security.pEp.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class ConfiguredSetting<SETTING_TYPE>(
        @SerialName("value") var value: SETTING_TYPE,
        @SerialName("locked") val locked: Boolean,
)

fun decodeBooleanFromString(json: String?): ConfiguredSetting<Boolean>? {
    return when (json) {
        "true" -> ConfiguredSetting(value = true, locked = false)
        "false" -> ConfiguredSetting(value = false, locked = false)
        null -> ConfiguredSetting(value = true, locked = false)
        else -> Json.decodeFromString(json)
    }
}

fun encodeBooleanToString(json: ConfiguredSetting<Boolean>?): String? {
    return if (json != null) {
        Json.encodeToString(json)
    } else null
}