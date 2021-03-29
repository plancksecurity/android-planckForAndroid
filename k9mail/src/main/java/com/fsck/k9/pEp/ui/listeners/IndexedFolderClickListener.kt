package com.fsck.k9.pEp.ui.listeners

import com.fsck.k9.mailstore.LocalFolder

interface IndexedFolderClickListener {
    fun onClick(position: Int)
}

inline fun indexedFolderClickListener(
        crossinline onClick: (position: Int) -> Unit = { _ -> }
): IndexedFolderClickListener {
    return object : IndexedFolderClickListener {
        override fun onClick(position: Int) {
            onClick.invoke(position)
        }

    }
}

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