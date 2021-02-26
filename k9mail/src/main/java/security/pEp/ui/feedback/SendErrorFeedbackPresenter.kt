package security.pEp.ui.feedback

import android.content.Intent
import android.content.IntentSender
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageLoaderHelper
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper
import javax.inject.Inject

class SendErrorFeedbackPresenter @Inject constructor(
    private val preferences: Preferences,
    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper
) {
    private lateinit var view: SendErrorFeedbackView
    private lateinit var sendFailedData: SendErrorFeedbackActivityData
    private lateinit var account: Account
    private lateinit var localMessage: LocalMessage
    private lateinit var title: String
    private lateinit var text: String
    private lateinit var controller: MessagingController

    fun initialize(
        view: SendErrorFeedbackView,
        controller: MessagingController,
        accountUuid: String,
        title: String,
        text: String,
        messageReferenceString: String
    ) {
        this.view = view
        this.controller = controller

        if (accountUuid.isEmpty() || title.isEmpty() || text.isEmpty() || messageReferenceString.isEmpty()) {
            view.finish()
            return
        }

        account = preferences.getAccount(accountUuid)
        this.title = title
        this.text = text
        val messageReference = MessageReference.parse(messageReferenceString)
        messageReference?.let {
            loadMessage(it)
        }
    }

    private fun loadMessage(messageReference: MessageReference) {
        simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(
            messageReference,
            messageLoadedCallback
        )
    }

    private fun populateSendFailedData() {
        sendFailedData = SendErrorFeedbackActivityData.create(
            account,
            title,
            text,
            localMessage
        )
        view.populateSendFailedData(sendFailedData)
    }

    fun onCloseButtonClicked() {
        view.finish()
    }

    fun sendPendingMessages() {
        controller.sendPendingMessages(account, null)
        view.finish()
    }

    private val messageLoadedCallback = object : MessageLoaderHelper.MessageLoaderCallbacks {
        override fun onMessageDataLoadFinished(message: LocalMessage) {
            localMessage = message
            populateSendFailedData()
        }

        override fun onMessageDataLoadFailed() {
            view.showMessageLoadError()
        }

        override fun onMessageViewInfoLoadFinished(messageViewInfo: MessageViewInfo?) {}

        override fun onMessageViewInfoLoadFailed(messageViewInfo: MessageViewInfo?) {}

        override fun setLoadingProgress(current: Int, max: Int) {}

        override fun startIntentSenderForMessageLoaderHelper(
            si: IntentSender?,
            requestCode: Int,
            fillIntent: Intent?,
            flagsMask: Int,
            flagValues: Int,
            extraFlags: Int
        ) {
        }

        override fun onDownloadErrorMessageNotFound() {}

        override fun onDownloadErrorNetworkError() {}
    }
}