package security.pEp.mdm

import kotlinx.serialization.Serializable

data class AppConfig(
        val key: String,
        val json: String?,
)

@Serializable
data class JsonAppConfig<SETTING_TYPE>(
        val value: SETTING_TYPE,
        val blocked: Boolean = false,
)