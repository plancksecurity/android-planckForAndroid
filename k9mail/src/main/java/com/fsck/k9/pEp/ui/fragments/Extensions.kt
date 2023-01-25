package com.fsck.k9.pEp.ui.fragments

import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.mail.ServerSettings

fun DiscoveredServerSettings.toServerSettings(): ServerSettings? {
    val authType = this.authType ?: return null
    val username = this.username ?: return null

    return ServerSettings(
        ServerSettings.Type.valueOf(protocol),
        host,
        port,
        security,
        authType,
        username,
        null,
        null
    )
}