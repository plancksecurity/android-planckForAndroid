package security.pEp.provisioning

import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import security.pEp.mdm.AuthType
import security.pEp.mdm.toMdmAuthType
import security.pEp.network.UrlChecker

data class AccountMailSettingsProvision(
    val incoming: SimpleMailSettings,
    val outgoing: SimpleMailSettings,
    val oAuthType: OAuthProviderType = OAuthProviderType.NONE
)

fun AccountMailSettingsProvision?.isValidForProvision(urlChecker: UrlChecker): Boolean =
    this != null && incoming.isValid(urlChecker) && outgoing.isValid(urlChecker)

data class SimpleMailSettings(
    var port: Int = -1,
    var server: String? = null,
    var connectionSecurity: ConnectionSecurity? = null,
    var userName: String? = null,
    var authType: AuthType? = null,
){
    fun getConnectionSecurityString(): String = when(connectionSecurity) {
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
}

fun ServerSettings.toSimpleMailSettings(): SimpleMailSettings = SimpleMailSettings(
    port, host, connectionSecurity, username, authenticationType.toMdmAuthType()
)

fun Int.isValidPort() = this in 1..65535

fun String.isValidServer(urlChecker: UrlChecker) =
    this.isNotBlank() && urlChecker.isValidUrl(this)

