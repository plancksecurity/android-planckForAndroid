package com.fsck.k9.activity.folderlist

import android.view.View
import com.fsck.k9.activity.MessageList
import com.fsck.k9.search.LocalSearch

class FolderClickListener constructor(private val search: LocalSearch) : View.OnClickListener {

    override fun onClick(v: View) {
        MessageList.actionDisplaySearch(v.context, search, true, false, true)
    }

}