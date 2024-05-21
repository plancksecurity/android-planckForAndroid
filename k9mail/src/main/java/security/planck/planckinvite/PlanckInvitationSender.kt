package security.planck.planckinvite

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.misc.Attachment
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.message.MessageBuilder
import com.fsck.k9.message.SimpleMessageBuilder
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.planck.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import security.planck.messaging.MessagingRepository
import security.planck.resources.RawResourceAttachmentCreator
import security.planck.resources.RawResources
import java.util.Date
import javax.inject.Inject

private const val INVITATION_ATTACHMENT_NAME = "email_gradient"
private const val INVITATION_ATTACHMENT_MIME_TYPE = "image/png"
private const val INVITATION_PARAGRAPH_1_PLACE_HOLDER = "PARAGRAPH_1"
private const val INVITATION_PARAGRAPH_2_PLACE_HOLDER = "PARAGRAPH_2"
private const val INVITATION_BUTTON_TEXT_PLACE_HOLDER = "BUTTON_TEXT"
private const val INVITATION_PARAGRAPH_3_PLACE_HOLDER = "PARAGRAPH_3"

class PlanckInvitationSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rawResources: RawResources,
    private val invitationAttachmentCreator: RawResourceAttachmentCreator,
    private val messagingRepository: MessagingRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val uiScope = CoroutineScope(dispatcherProvider.main())


    fun sendPlanckInvitesToUnsecureRecipients(
        recipients: List<Address>,
        sender: Identity,
        account: Account,
    ) {
        uiScope.launch {
            val messageBuilder = createMessageBuilderToSendPlanckInvites(
                recipients,
                sender,
                account
            )
            messagingRepository.buildAndSendMessage(messageBuilder, account)
        }
    }
    private suspend fun createMessageBuilderToSendPlanckInvites(
        recipients: List<Address>,
        sender: Identity,
        account: Account,
    ): MessageBuilder {
        val builder: MessageBuilder = SimpleMessageBuilder.newInstance()
        val invitationTemplate: String = rawResources.readTextFileFromRaw(R.raw.planck_invite)
        val invitationText = invitationTemplate.replace(
            INVITATION_PARAGRAPH_1_PLACE_HOLDER,
            context.getString(R.string.planck_invite_paragraph_1, sender.email)
        ).replace(
            INVITATION_PARAGRAPH_2_PLACE_HOLDER,
            context.getString(R.string.planck_invite_paragraph_2)
        ).replace(
            INVITATION_BUTTON_TEXT_PLACE_HOLDER,
            context.getString(R.string.planck_invite_button_text)
        ).replace(
            INVITATION_PARAGRAPH_3_PLACE_HOLDER,
            context.getString(R.string.planck_invite_paragraph_3, sender.email)
        )
        val invitationAttachments: Map<String, Attachment> = createInvitationAttachment()

        return builder.setSubject(context.getString(R.string.planck_invite_title))
            .setSentDate(Date())
            .setHideTimeZone(K9.hideTimeZone())
            .setTo(recipients)
            .setIdentity(sender)
            .setMessageFormat(SimpleMessageFormat.HTML)
            .setText(invitationText)
            .setInlineAttachments(invitationAttachments)
            .setSignature(account.signature)
            .setSignatureBeforeQuotedText(account.isSignatureBeforeQuotedText)
            .html()
    }

    private suspend fun createInvitationAttachment(): Map<String, Attachment> {
        val invitationAttachment: Attachment = invitationAttachmentCreator.createAttachment(
            R.raw.email_gradient,
            INVITATION_ATTACHMENT_NAME,
            INVITATION_ATTACHMENT_MIME_TYPE
        )
        val inlineAttachments: MutableMap<String, Attachment> = HashMap(1)
        inlineAttachments[invitationAttachment.name] = invitationAttachment
        return inlineAttachments
    }
}