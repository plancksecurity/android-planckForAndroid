package com.fsck.k9.planck.ui

import com.fsck.k9.mail.ServerSettings

data class ConnectionSettings(val incoming: ServerSettings, val outgoing: ServerSettings)
