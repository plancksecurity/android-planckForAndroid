package security.planck.ui.calendar

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fsck.k9.mail.internet.MimeUtility
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.provider.AttachmentTempFileProvider
import javax.inject.Inject

class CalendarInviteIntentCreator @Inject constructor() {

    fun getOpenCalendarIntent(context: Context, attachment: AttachmentViewInfo): Intent {
        val intentDataUri =
            AttachmentTempFileProvider.createTempUriForContentUri(context, attachment.internalUri)
        val displayName: String = attachment.displayName
        val inferredMimeType = MimeUtility.getMimeTypeByExtension(displayName)
        return createViewIntentForAttachmentProviderUri(intentDataUri, inferredMimeType)
    }

    private fun createViewIntentForAttachmentProviderUri(
        contentUri: Uri,
        mimeType: String
    ): Intent {
        val uri = AttachmentTempFileProvider.getMimeTypeUri(contentUri, mimeType)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addUiIntentFlags(intent)
        return intent
    }

    private fun addUiIntentFlags(intent: Intent) {
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        )
    }
}
