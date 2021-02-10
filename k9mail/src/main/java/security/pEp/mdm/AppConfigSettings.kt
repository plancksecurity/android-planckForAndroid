package security.pEp.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


data class AppConfigEntry(
        val key: String,
        val value: String?,
) {
    inline fun <reified TYPE> getValue(): ManageableSettingMDMEntry<TYPE>? {
        return Json.runCatching { decodeFromString<ManageableSettingMDMEntry<TYPE>>(value!!) }
                .getOrNull()

    }

}

@Serializable
data class ManageableSettingMDMEntry<TYPE>(
        @SerialName("locked") val locked: Boolean,
        @SerialName("value") val value: TYPE,
) {
    fun toManageableSetting(): ManageableSetting<TYPE> =
            ManageableSetting(value = value, locked = locked)
}

