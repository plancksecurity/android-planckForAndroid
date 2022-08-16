package security.pEp.provisioning

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.ConnectionSecurity
import security.pEp.network.UrlChecker

data class AccountMailSettingsProvision(
    val incoming: SimpleMailSettings,
    val outgoing: SimpleMailSettings,
    val oAuthType: OAuthProviderType = OAuthProviderType.NONE
)

fun AccountMailSettingsProvision?.isValidForProvision(urlChecker: UrlChecker): Boolean =
    this != null && incoming.isValid(urlChecker) && outgoing.isValid(urlChecker)

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

    fun isValid(urlChecker: UrlChecker): Boolean =
        port in 1..65535
                && !server.isNullOrBlank() && urlChecker.isValidUrl(server)
                && connectionSecurity != null
                && !userName.isNullOrBlank()
}

