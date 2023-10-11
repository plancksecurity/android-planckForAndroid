package security.planck.sync

import com.fsck.k9.planck.manualsync.SyncAppState
import foundation.pEp.jniadapter.Sync
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
     * planckSyncEnvironmentInitialized
     *
     * Whether the sync environment was already initialized
     */
    val planckSyncEnvironmentInitialized: Boolean

    /**
     * notifyHandshakeCallback
     *
     * A [NotifyHandshakeCallback] to receive sync-related signals from the core.
     */
    val notifyHandshakeCallback: Sync.NotifyHandshakeCallback

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
    fun shutdownSync()
    fun userConnected()
}