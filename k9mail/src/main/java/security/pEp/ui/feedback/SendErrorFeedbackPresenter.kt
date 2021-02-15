package security.pEp.ui.feedback

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import javax.inject.Inject

class SendErrorFeedbackPresenter @Inject constructor(private val preferences: Preferences) {
    private lateinit var view: SendErrorFeedbackView
    private lateinit var sendFailedData: SendErrorFeedbackActivityData
    private lateinit var account: Account

    fun initialize(view: SendErrorFeedbackView, data: SendErrorFeedbackActivityData?) {
        this.view = view
        sendFailedData = data ?: let {
            view.finish()
            return
        }
        account = preferences.getAccount(data.accountUuid)
        populateSendFailedData()
    }

    private fun populateSendFailedData() {
        view.populateSendFailedData(sendFailedData)
    }

    fun onCloseButtonClicked() {
        view.finish()
    }

    fun sendPendingMessages() {
        MessagingController.getInstance().sendPendingMessages(account, null)
        view.finish()
    }
}