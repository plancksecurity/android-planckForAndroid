package com.fsck.k9.activity.folderlist

import com.fsck.k9.activity.FolderInfoHolder

interface FolderFilterListener {

    fun publishResults(filteredFolders: MutableList<FolderInfoHolder>)

}