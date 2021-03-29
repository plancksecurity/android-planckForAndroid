package com.fsck.k9.pEp.ui.listeners

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