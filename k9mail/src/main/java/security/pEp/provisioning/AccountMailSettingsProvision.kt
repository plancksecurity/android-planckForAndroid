package security.pEp.provisioning

import com.fsck.k9.mail.ConnectionSecurity

data class AccountMailSettingsProvision(
    val incoming: SimpleMailSettings,
    val outgoing: SimpleMailSettings,
) {
    fun isValidForProvision(): Boolean =
        incoming.isValidForProvision() && outgoing.isValidForProvision()
}

data class SimpleMailSettings(
    val port: Int = -1,
    val server: String? = null,
    val connectionSecurity: ConnectionSecurity? = null,
    val userName: String? = null,
){
    fun getConnectionSecurityString(): String = when(connectionSecurity) {
        null, ConnectionSecurity.NONE -> ""
        ConnectionSecurity.SSL_TLS_REQUIRED -> "ssl"
        ConnectionSecurity.STARTTLS_REQUIRED -> "tls"
    }

    fun isValidForProvision(): Boolean =
        port > 0 && server != null && connectionSecurity != null
}

