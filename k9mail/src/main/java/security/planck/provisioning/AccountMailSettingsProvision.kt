package security.planck.provisioning

import com.fsck.k9.mail.ConnectionSecurity
import security.planck.mdm.AuthType
import security.planck.network.UrlChecker

data class AccountMailSettingsProvision(
    val incoming: SimpleMailSettings,
    val outgoing: SimpleMailSettings,
) {
    fun isValidForProvision(urlChecker: UrlChecker): Boolean =
        incoming.isValid(urlChecker) && outgoing.isValid(urlChecker)
}

data class SimpleMailSettings(
    var port: Int = -1,
    var server: String? = null,
    var connectionSecurity: ConnectionSecurity? = null,
    var userName: String? = null,
    var authType: AuthType? = null,
) {
    private fun getConnectionSecurityString(): String = when (connectionSecurity) {
        null, ConnectionSecurity.NONE -> ""
        ConnectionSecurity.SSL_TLS_REQUIRED -> "ssl"
        ConnectionSecurity.STARTTLS_REQUIRED -> "tls"
    }

    fun isValid(urlChecker: UrlChecker): Boolean =
        port.isValidPort()
                && server != null && server!!.isValidServer(urlChecker)
                && connectionSecurity != null
                && !userName.isNullOrBlank()
                && authType != null

    fun toSeverUriTemplate(outgoing: Boolean): String {
        val protocol = if (outgoing) "smtp" else "imap"
        return (protocol +
                "+" +
                getConnectionSecurityString() +
                "+" +
                "://" +
                server +
                ":" +
                port)
    }
}
