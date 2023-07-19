package com.fsck.k9.planck.manualsync

sealed class SyncScreenState {
    object Idle: SyncScreenState()

    object AwaitingOtherDevice: SyncScreenState()

    object HandshakeReadyAwaitingUser: SyncScreenState()

    data class UserHandshaking(
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
    ): SyncScreenState()

    data class AwaitingHandshakeCompletion(
        val ownFpr: String,
        val partnerFpr: String,
    ): SyncScreenState()

    object Done: SyncScreenState()

    object TimeoutError: SyncScreenState()

    object Cancelled: SyncScreenState()

    fun finish() = Idle

    val needsFastPolling: Boolean
        get() = this == AwaitingOtherDevice || this is AwaitingHandshakeCompletion

    val allowSyncNewDevices: Boolean
        get() = this == AwaitingOtherDevice
}



