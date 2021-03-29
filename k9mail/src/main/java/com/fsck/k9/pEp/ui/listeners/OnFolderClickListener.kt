package com.fsck.k9.pEp.ui.listeners

import com.fsck.k9.mailstore.LocalFolder

interface OnFolderClickListener {
    fun onClick(folder: LocalFolder)
    fun onClick(position: Int)
}