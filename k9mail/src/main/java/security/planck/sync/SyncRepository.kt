package security.planck.sync

import com.fsck.k9.K9
import com.fsck.k9.planck.manualsync.SyncAppState
import foundation.pEp.jniadapter.Sync.NotifyHandshakeCallback
import kotlinx.coroutines.flow.StateFlow

/**
 * SyncRepository
 *
 * Repository for sync-related operations.
 */
interface SyncRepository {
    /**
     * isGrouped
     *
     * Whether the device is in a device group.
     */
    var isGrouped: Boolean

    /**
     * syncStateFlow
     *
     * A [StateFlow] of [SyncAppState] that can be collected to receive updates on current sync state.
     */
    val syncStateFlow: StateFlow<SyncAppState>

    /**
     * notifyHandshakeCallback
     *
     * A [NotifyHandshakeCallback] to receive sync-related signals from the core.
     */
    val notifyHandshakeCallback: NotifyHandshakeCallback

    /**
     * setCurrentState
     *
     * Sets current state of [syncStateFlow]. Used from ViewModels to communicate a desired state.
     *
     * @param state [SyncAppState] to be set.
     */
    fun setCurrentState(state: SyncAppState)

    /**
     * setupFastPoller
     *
     * Initially enable the fast polling of sync messages.
     */
    fun setupFastPoller()

    /**
     * setPlanckSyncEnabled
     *
     * Enable or disable key sync.
     * - If [enabled] is false and device is grouped, device group is left.
     * - If [enabled] is false and device is not grouped, sync is disabled.
     *
     * @param enabled Whether to enable sync.
     */
    fun setPlanckSyncEnabled(enabled: Boolean)

    /**
     * planckInitSyncEnvironment
     *
     * Initialize device sync, expected to be called on application startup and account setup.
     */
    fun planckInitSyncEnvironment()

    /**
     * allowManualSync
     *
     * Allow key sync handshake with another device with a timeout for handshake to start.
     */
    fun allowTimedManualSync()

    /**
     * cancelSync
     *
     * Cancel ongoing key sync handshake process.
     */
    fun cancelSync()

    /**
     * syncStartTimeout
     *
     * Key sync handshake timed out its start.
     */
    fun syncStartTimeout()

    /**
     * shutdownSync
     *
     * Temporarily disable sync. This status is not persisted unless [K9.save] is called.
     */
    suspend fun shutdownSync()

    /**
     * userConnected
     *
     * User interface is connected to this [SyncRepository] and starts collecting the [syncStateFlow].
     */
    suspend fun userConnected()

    /**
     * lockHandshake
     *
     * The user locked the last available handshake data. This data will be used through the rest of the process.
     * So it cannot be externally changed by any other handshake signal coming from the core.
     */
    fun lockHandshake()

    /**
     * userDisconnected
     *
     * User interface is disconnected from this [SyncRepository].
     */
    fun userDisconnected()
}