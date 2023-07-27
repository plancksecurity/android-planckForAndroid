package com.fsck.k9.planck.manualsync

object SyncState {
    // common states
    object Idle : SyncScreenState, SyncAppState

    object AwaitingOtherDevice : SyncScreenState, SyncAppState

    object HandshakeReadyAwaitingUser : SyncScreenState, SyncAppState

    object Done : SyncScreenState, SyncAppState

    object TimeoutError : SyncScreenState, SyncAppState

    object Cancelled : SyncScreenState, SyncAppState

    object SyncStartTimeout : SyncScreenState, SyncAppState

    // only Screen states

    data class Error(val throwable: Throwable) : SyncScreenState

    data class UserHandshaking(
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
    ) : SyncScreenState

    data class AwaitingHandshakeCompletion(
        val ownFpr: String,
        val partnerFpr: String,
    ) : SyncScreenState

    // only app state
    object PerformingHandshake : SyncAppState
}

sealed interface SyncScreenState

sealed interface SyncAppState {
    val needsFastPolling: Boolean
        get() = this == SyncState.AwaitingOtherDevice || this is SyncState.PerformingHandshake
    val allowSyncNewDevices: Boolean
        get() = this == SyncState.AwaitingOtherDevice

    fun finish() = SyncState.Idle
}

