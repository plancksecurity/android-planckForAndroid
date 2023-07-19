package com.fsck.k9.planck.manualsync

import foundation.pEp.jniadapter.Identity

interface SyncStateChangeListener {
    fun syncStateChanged(state: SyncScreenState)

    fun syncStateChanged(
        state: SyncScreenState,
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean,
    )
}