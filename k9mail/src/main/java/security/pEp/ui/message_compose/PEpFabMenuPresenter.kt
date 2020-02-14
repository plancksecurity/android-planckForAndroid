package security.pEp.ui.message_compose

import com.fsck.k9.pEp.ui.infrastructure.MessageAction
import com.fsck.k9.pEp.ui.listeners.OnMessageOptionsListener

class PEpFabMenuPresenter(private val view: PEpFabMenuView) {
    var open = false
    lateinit var listener: OnMessageOptionsListener

    init {
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