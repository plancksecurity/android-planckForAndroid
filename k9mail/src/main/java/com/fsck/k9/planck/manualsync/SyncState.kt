package com.fsck.k9.planck.manualsync

sealed class SyncState {
    object Idle: SyncState()

    object AwaitingOtherDevice: SyncState()

    object HandshakeReadyAwaitingUser: SyncState()

    data class UserHandshaking(
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
    ): SyncState()

    data class AwaitingHandshakeCompletion(
        val ownFpr: String,
        val partnerFpr: String,
    ): SyncState()

    object Done: SyncState()

    object TimeoutError: SyncState()

    data class Error(val throwable: Throwable): SyncState()

    object Cancelled: SyncState()

    fun finish() = Idle

    val needsFastPolling: Boolean
        get() = this == AwaitingOtherDevice || this is AwaitingHandshakeCompletion

    val allowSyncNewDevices: Boolean
        get() = this == AwaitingOtherDevice
}



