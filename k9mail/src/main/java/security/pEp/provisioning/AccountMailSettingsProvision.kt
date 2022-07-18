package security.pEp.provisioning

import com.fsck.k9.mail.ConnectionSecurity

data class SimpleMailSettings(
    val port: Int = -1,
    val server: String? = null,
    val connectionSecurity: ConnectionSecurity? = null,
    val userName: String? = null,
)
