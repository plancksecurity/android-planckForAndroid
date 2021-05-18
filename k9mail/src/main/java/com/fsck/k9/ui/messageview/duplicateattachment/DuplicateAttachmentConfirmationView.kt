package com.fsck.k9.ui.messageview.duplicateattachment

interface DuplicateAttachmentConfirmationView {
    fun finish()

    fun displayOverwriteStage()

    fun displayRenameStage(
        canGoBack: Boolean,
        defaultFileName: String
    )
}