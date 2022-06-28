package security.pEp.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


data class AppConfigEntry(
        val key: String,
        val value: String?,
) {
    inline fun <reified TYPE> getValue(): ManageableSettingMdmEntry<TYPE>? {
        return if (value != null) {
            Json.runCatching { decodeFromString<ManageableSettingMdmEntry<TYPE>>(value) }
                    .getOrNull()
        } else null

    }

}

@Serializable
data class ManageableSettingMdmEntry<TYPE>(
        @SerialName("value") val value: TYPE,
        @SerialName("locked") val locked: Boolean = true,
) {
    fun toManageableSetting(): ManageableSetting<TYPE> =
            ManageableSetting(value = value, locked = locked)
}

@Serializable
data class ExtraKey(@SerialName("fpr") val fpr: String)
