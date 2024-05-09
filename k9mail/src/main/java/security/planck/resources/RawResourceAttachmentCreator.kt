package security.planck.resources

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import com.fsck.k9.activity.misc.Attachment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RawResourceAttachmentCreator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resourceToFile: RawResourceToFile
) {
    fun createAttachment(
        @RawRes resourceId: Int,
        fileName: String,
        contentType: String
    ): Attachment = runBlocking {
        withContext(Dispatchers.IO) {
            val invitationFile = resourceToFile.copyRawResourceToFile(resourceId, fileName)
            val packageName = context.packageName
            val resourceUri = Uri.parse("android.resource://$packageName/$resourceId")
            val myAttachment = Attachment.createAttachment(resourceUri, -1, contentType)
            val metadataAttachment = myAttachment.deriveWithMetadataLoaded(
                contentType,
                fileName,
                invitationFile.length()
            )
            metadataAttachment.deriveWithLoadComplete(invitationFile.absolutePath)
        }
    }
}