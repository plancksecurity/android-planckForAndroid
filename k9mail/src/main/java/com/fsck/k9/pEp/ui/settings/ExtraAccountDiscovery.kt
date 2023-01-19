package com.fsck.k9.pEp.ui.settings

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.pEp.ui.ConnectionSettings

object ExtraAccountDiscovery {
    @JvmStatic
    fun discover(email: String): ConnectionSettings? {
        return if (email.endsWith("@fakemail")) {
            val serverSettings = ServerSettings(
                ServerSettings.Type.IMAP,
                "irrelevant",
                23,
                ConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.AUTOMATIC,
                "irrelevant",
                "irrelevant",
                null
            )
            ConnectionSettings(incoming = serverSettings, outgoing = serverSettings)
        } else {
            null
        }
    }
}
