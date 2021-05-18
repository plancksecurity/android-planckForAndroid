package com.fsck.k9.ui.messageview.duplicateattachment

interface DuplicateAttachmentConfirmationView {
    fun finish()

    fun displayOverwriteScreen()

    fun displayRenameScreen(
        canGoBack: Boolean,
        defaultFileName: String
    )
}