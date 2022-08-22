package security.pEp.provisioning

import android.util.Patterns
import com.fsck.k9.mail.ServerSettings
import security.pEp.mdm.toMdmAuthType
import security.pEp.network.UrlChecker

fun ServerSettings.toSimpleMailSettings(): SimpleMailSettings = SimpleMailSettings(
    port, host, connectionSecurity, username, authenticationType.toMdmAuthType()
)

fun Int.isValidPort() = this in 1..65535

fun String.isValidServer(urlChecker: UrlChecker) =
    this.isNotBlank() && urlChecker.isValidUrl(this)

fun String.isValidEmailAddress() = Patterns.EMAIL_ADDRESS.matcher(this).matches()