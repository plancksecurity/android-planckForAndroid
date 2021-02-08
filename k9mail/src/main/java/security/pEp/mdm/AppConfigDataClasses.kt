package security.pEp.mdm

import com.fsck.k9.ui.settings.account.ConfiguredSetting
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


data class AppConfig(
        val key: String,
        val json: String?,
)

inline fun <reified TYPE> AppConfig.getValue(): ConfiguredSetting<TYPE>? {
    return if (json != null) {
        Json.decodeFromString(json)
    } else null
}