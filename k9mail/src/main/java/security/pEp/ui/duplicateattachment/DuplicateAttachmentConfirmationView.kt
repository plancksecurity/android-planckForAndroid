package security.pEp.ui.duplicateattachment

interface DuplicateAttachmentConfirmationView {
    fun finish()

    fun displayOverwriteStage()

    fun displayRenameStage(
        canGoBack: Boolean,
        defaultFileName: String
    )
}