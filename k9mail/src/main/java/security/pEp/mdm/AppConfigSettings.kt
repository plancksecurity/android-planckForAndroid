package security.pEp.mdm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


data class AppConfigEntry(
        val key: String,
        val value: String?,
){
    inline fun <reified TYPE> getValue(): ManageableSettingMDMEntry<TYPE> =
            Json.decodeFromString(value!!)
}

@Serializable
data class ManageableSettingMDMEntry<TYPE>(
        @SerialName("locked") val locked: Boolean,
        @SerialName("value") val value: TYPE,
){
    fun toManageableSetting(): ConfiguredSetting<TYPE> =
            ConfiguredSetting(value = value, locked = locked)
}

