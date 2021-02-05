package security.pEp.mdm

import kotlinx.serialization.*

data class AppConfig(
        val key: String,
        val json: String?,
)

@Serializable
data class PEpGeneralConfigBoolean(
        val value: Boolean = true,
        val blocked: Boolean = false,
)