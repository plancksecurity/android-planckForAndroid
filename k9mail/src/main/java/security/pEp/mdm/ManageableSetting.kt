package security.pEp.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


@Serializable
data class ManageableSetting<SETTING_TYPE>(
        @SerialName("value") var value: SETTING_TYPE,
        @SerialName("locked") val locked: Boolean,
)

fun decodeBooleanFromString(json: String?): ManageableSetting<Boolean>? {
    return when (json) {
        "true" -> ManageableSetting(value = true, locked = false)
        "false" -> ManageableSetting(value = false, locked = false)
        null -> ManageableSetting(value = true, locked = false)
        else -> Json.decodeFromString(json)
    }
}

fun encodeBooleanToString(json: ManageableSetting<Boolean>?): String? {
    return if (json != null) {
        Json.encodeToString(json)
    } else null
}