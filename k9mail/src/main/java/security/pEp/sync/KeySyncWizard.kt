package security.pEp.sync

import foundation.pEp.jniadapter.SyncHandshakeSignal
//TODO: Rewrite the wizard.
abstract class SyncAction
class Cancel: SyncAction()
class Next: SyncAction()
class Reject: SyncAction()
class confirm: SyncAction()
class leave: SyncAction()
class retry: SyncAction()

enum class DeviceGroupStatus {
    SOLE, GROUPED, ADDING
}
interface KeySyncWizardView {
    fun showHandshake()

}
interface KeySyncWizard {
    //fun getText(): String
    fun getCurrentState(): SyncState

    fun notify(signal: SyncHandshakeSignal) {
        when (signal) {
            SyncHandshakeSignal.SyncNotifySole -> TODO()
            SyncHandshakeSignal.SyncNotifyUndefined -> TODO()
            SyncHandshakeSignal.SyncNotifyInitAddOurDevice -> TODO()
            SyncHandshakeSignal.SyncNotifyInitAddOtherDevice -> TODO()
            SyncHandshakeSignal.SyncNotifyInitFormGroup -> TODO()
            SyncHandshakeSignal.SyncNotifyTimeout -> TODO()
            SyncHandshakeSignal.SyncNotifyAcceptedDeviceAdded -> TODO()
            SyncHandshakeSignal.SyncNotifyAcceptedGroupCreated -> TODO()
            SyncHandshakeSignal.SyncNotifyAcceptedDeviceAccepted -> TODO()
            SyncHandshakeSignal.SyncNotifyInGroup -> TODO()
        }
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class KeySyncAppWizard(var state: SyncState = SyncState.INITIAL) : KeySyncWizard {
    init {
        state.next()
    }

    override fun getCurrentState(): SyncState {
        return state
    }
}

enum class SyncState {
    INITIAL {
        override fun next() = HANDSHAKING
    },
    HANDSHAKING {
        override fun next() = WAITING
    },
    WAITING {
        override fun next() = DONE
    },
    DONE {
        override fun next() = INITIAL
    },
    ERROR {
        override fun next(): SyncState {
            throw IllegalStateException("In case of error is needed to explicitly cancel().")
        }
    };

    abstract fun next(): SyncState
    fun finish(): SyncState = INITIAL
}