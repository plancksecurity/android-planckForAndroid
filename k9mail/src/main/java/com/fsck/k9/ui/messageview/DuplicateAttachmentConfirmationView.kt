package com.fsck.k9.ui.messageview

interface DuplicateAttachmentConfirmationView {
    fun finish()

    fun displayOverwriteScreen()

    fun displayRenameScreen(
        backOrCancel: Boolean,
        defaultFileName: String
    )
}