package com.fsck.k9.pEp.ui.listeners

import com.fsck.k9.mailstore.LocalFolder

interface FolderClickListener {
    fun onClick(folder: LocalFolder)
}

inline fun folderClickListener(
        crossinline onClick: (folder: LocalFolder) -> Unit = { _ -> }
): FolderClickListener {
    return object : FolderClickListener {
        override fun onClick(folder: LocalFolder) {
            onClick.invoke(folder)
        }

    }
}