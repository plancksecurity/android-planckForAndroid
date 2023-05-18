package security.planck.ui.message_compose

import com.fsck.k9.planck.ui.infrastructure.MessageAction
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener

class PlanckFabMenuPresenter(private val view: PlanckFabMenuView) {
    var open = false
    lateinit var listener: OnMessageOptionsListener

    fun init() {
        view.showInitialState()
    }

    fun onMainActionClicked() {
        if (open) {
            view.closeMenu()
            open = false
        } else {
            onReplyClicked()
        }
    }

    fun onLongClicked() {
        if (open) {
            view.closeMenu()
            open = false
        } else {
            view.openMenu()
            open = true
        }
    }

    fun onForwardClicked() {
        listener.OnMessageOptionsListener(MessageAction.FORWARD)
    }

    fun onReplyAllClicked() {
        listener.OnMessageOptionsListener(MessageAction.REPLY_ALL)
    }

    fun onReplyClicked() {
        listener.OnMessageOptionsListener(MessageAction.REPLY)
    }


}