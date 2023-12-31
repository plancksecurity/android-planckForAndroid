package security.planck.provisioning

import android.util.Patterns
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import security.planck.mdm.toMdmAuthType
import security.planck.network.UrlChecker

const val CONNECTION_SECURITY_NONE = "NONE"
const val CONNECTION_SECURITY_STARTTLS = "STARTTLS"
const val CONNECTION_SECURITY_SSL_TLS = "SSL/TLS"

fun ServerSettings.toSimpleMailSettings(): SimpleMailSettings = SimpleMailSettings(
    port, host, connectionSecurity, username, authenticationType.toMdmAuthType()
)

fun Int.isValidPort() = this in 1..65535

fun String.isValidServer(urlChecker: UrlChecker) = urlChecker.isValidUrl(this)

fun String.isValidEmailAddress() = Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.toConnectionSecurity(): ConnectionSecurity? = when {
    this.equals(CONNECTION_SECURITY_NONE, true) ->
        ConnectionSecurity.NONE

    this.equals(CONNECTION_SECURITY_STARTTLS, true) ->
        ConnectionSecurity.STARTTLS_REQUIRED

    this.equals(CONNECTION_SECURITY_SSL_TLS, true) ->
        ConnectionSecurity.SSL_TLS_REQUIRED

    else -> null
}
