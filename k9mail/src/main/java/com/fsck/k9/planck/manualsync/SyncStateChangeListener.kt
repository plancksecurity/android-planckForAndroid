package com.fsck.k9.planck.manualsync

import foundation.pEp.jniadapter.Identity

interface SyncStateChangeListener {
    fun syncStateChanged(state: SyncState)

    fun syncStateChanged(
        state: SyncState.HandshakeReadyAwaitingUser,
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean,
    )
}