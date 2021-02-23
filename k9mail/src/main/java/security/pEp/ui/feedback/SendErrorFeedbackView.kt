package security.pEp.ui.feedback

interface SendErrorFeedbackView {
    fun finish()

    fun populateSendFailedData(data: SendErrorFeedbackActivityData)

    fun showMessageLoadError()
}