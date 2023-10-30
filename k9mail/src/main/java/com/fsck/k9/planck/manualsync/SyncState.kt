package com.fsck.k9.planck.manualsync

import foundation.pEp.jniadapter.Identity

/**
 * Internal state of the application regarding Sync.
 */
object SyncState {
    // common states

    /**
     * Initial state, nothing happened yet.
     */
    object Idle : SyncScreenState, SyncAppState

    /**
     * Waiting to find another device to offer handshake to the user.
     *
     * @param inCatchupAllowancePeriod Boolean When true, it means we are waiting some seconds just
     * to allow both devices to catchup to their states. We may already have an available handshake.
     */
    data class AwaitingOtherDevice(val inCatchupAllowancePeriod: Boolean = false) : SyncScreenState,
        SyncAppState

    /**
     * Handshake available.
     *
     * @param myself My own identity
     * @param partner The other device's identity
     * @param formingGroup Whether this handshake is to form a group.
     */
    data class HandshakeReadyAwaitingUser(
        val myself: Identity,
        val partner: Identity,
        val formingGroup: Boolean,
    ) : SyncScreenState, SyncAppState

    /**
     * Handshake ended successfully and devices are synced.
     * Last screen of the sync wizard in the "happy path".
     */
    object Done : SyncScreenState, SyncAppState

    /**
     * Sync timed out (SyncNotifyTimeout).
     */
    object TimeoutError : SyncScreenState, SyncAppState

    /**
     * Handshake was cancelled.
     */
    object Cancelled : SyncScreenState, SyncAppState

    /**
     * The other device could not be found during the allowed time window.
     */
    object SyncStartTimeout : SyncScreenState, SyncAppState

    // only Screen states

    /**
     * An error happened, feedback to the user.
     */
    data class Error(val throwable: Throwable) : SyncScreenState

    /**
     * User actually started taking active part in handshake process.
     * Trustwords screen of the sync wizard.
     */
    data class UserHandshaking(
        val ownFpr: String,
        val partnerFpr: String,
        val trustwords: String,
    ) : SyncScreenState

    /**
     * User accepted the trustwords and handshake is in its way to completion.
     */
    data class AwaitingHandshakeCompletion(
        val ownFpr: String,
        val partnerFpr: String,
    ) : SyncScreenState

    // only app state
    /**
     * Same as [AwaitingHandshakeCompletion] but not face to the user.
     */
    object PerformingHandshake : SyncAppState
}

sealed interface SyncScreenState

sealed interface SyncAppState {
    val needsFastPolling: Boolean
        get() = this is SyncState.AwaitingOtherDevice || this is SyncState.PerformingHandshake
    val allowSyncNewDevices: Boolean
        get() = this == SyncState.AwaitingOtherDevice(inCatchupAllowancePeriod = false)

    fun finish() = SyncState.Idle
}

