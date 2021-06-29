package security.pEp.ui.duplicateattachment

interface DuplicationAttachmentConfirmationListener {
    fun attachmentNameConfirmed(newName: String)
    fun overwriteAttachmentName()
}