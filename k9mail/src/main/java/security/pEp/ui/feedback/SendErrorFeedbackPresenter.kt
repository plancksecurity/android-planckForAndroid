package security.pEp.ui.feedback

import javax.inject.Inject

class SendErrorFeedbackPresenter @Inject constructor() {
    private lateinit var view: SendErrorFeedbackView
    private lateinit var sendFailedData: SendErrorFeedbackActivityData

    fun initialize(view: SendErrorFeedbackView, data: SendErrorFeedbackActivityData?) {
        this.view = view
        sendFailedData = data ?: let {
            view.finish()
            return
        }
        populateSendFailedData()
    }

    private fun populateSendFailedData() {
        view.populateSendFailedData(sendFailedData)
    }

    fun onOkButtonClicked() {
        view.finish()
    }
}