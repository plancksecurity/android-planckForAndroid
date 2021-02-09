package security.pEp.ui.feedback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.fsck.k9.databinding.ActivityFeedbackBinding
import com.fsck.k9.mail.Message
import com.fsck.k9.pEp.manualsync.WizardActivity
import kotlinx.android.parcel.Parcelize
import javax.inject.Inject

class SendErrorFeedbackActivity : WizardActivity(), SendErrorFeedbackView {
    private lateinit var binding: ActivityFeedbackBinding

    @Inject
    lateinit var presenter: SendErrorFeedbackPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()

        val intentSendFailedData: SendErrorFeedbackActivityData? =
            intent.extras?.getParcelable(EXTRA_SEND_FAILED_DATA)
        presenter.initialize(this, intentSendFailedData)
    }

    override fun populateSendFailedData(data: SendErrorFeedbackActivityData) {
        binding.feedbackTitle.text = data.title
        with(binding.messageDataLayout) {
            val messageInfo = data.messageInfo
            messageInfo?.let { msgInfo ->
                messageSubject.text = msgInfo.subject
                messageFrom.text = msgInfo.from
                val recipients = msgInfo.recipients
                messageTo.text = recipients.to
                if (recipients.cc.isNotBlank()) {
                    ccRow.visibility = View.VISIBLE
                    messageCc.text = recipients.cc
                }
                if (recipients.bcc.isNotBlank()) {
                    bccRow.visibility = View.VISIBLE
                    messageBcc.text = recipients.bcc
                }
                messageCreationDate.text = msgInfo.date
            }
            messageNotSentCause.text = data.text
        }
    }

    private fun setupViews() {
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.okButton.setOnClickListener { presenter.onOkButtonClicked() }

        setUpFloatingWindow()
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    companion object {
        private const val EXTRA_SEND_FAILED_DATA = "sendFailedData"

        @JvmStatic
        fun createFeedbackActivityIntent(
            context: Context,
            feedbackTitle: String,
            feedbackText: String,
            message: Message? = null
        ): Intent {
            val intent = Intent(context, SendErrorFeedbackActivity::class.java)
            val sendFailedData = SendErrorFeedbackActivityData.create(
                feedbackTitle,
                feedbackText,
                message
            )
            intent.putExtra(EXTRA_SEND_FAILED_DATA, sendFailedData)
            return intent
        }
    }
}

@Parcelize
data class SendErrorFeedbackActivityData(
    val title: String,
    val text: String,
    val messageInfo: FeedbackMessageInfo?
) : Parcelable {
    companion object {
        @JvmStatic
        fun create(
            title: String,
            text: String,
            message: Message? = null
        ): SendErrorFeedbackActivityData {
            return SendErrorFeedbackActivityData(
                title,
                text,
                FeedbackMessageInfo.fromMessage(message)
            )
        }
    }
}