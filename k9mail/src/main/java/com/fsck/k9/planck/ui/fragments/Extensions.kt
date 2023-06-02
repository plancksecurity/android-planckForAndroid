package com.fsck.k9.planck.ui.fragments

import com.fsck.k9.autodiscovery.api.DiscoveredServerSettings
import com.fsck.k9.mail.ServerSettings

fun DiscoveredServerSettings.toServerSettings(): ServerSettings? {
    val authType = this.authType ?: return null
    val username = this.username ?: return null
    this.authType
    this.host
    this.protocol
    this.security
    this.authType

    return ServerSettings(
        protocol,
        host,
        port,
        security,
        authType,
        username,
        null,
        null
    )
}