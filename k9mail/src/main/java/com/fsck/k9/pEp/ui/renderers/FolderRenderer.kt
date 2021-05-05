package com.fsck.k9.pEp.ui.renderers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.pEp.models.FolderModel
import com.fsck.k9.pEp.ui.listeners.FolderClickListener
import com.pedrogomez.renderers.Renderer
import kotlinx.android.synthetic.main.folder_navigation_list_item.view.*
import kotlinx.coroutines.*

class FolderRenderer : Renderer<FolderModel>() {

    lateinit var folderName: TextView
    lateinit var folderNewMessages: TextView
    private lateinit var onFolderClickListener: FolderClickListener


    override fun setUpView(rootView: View) {

    }

    override fun hookListeners(rootView: View) {

    }

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): View {
        val inflatedView = inflater.inflate(R.layout.folder_navigation_list_item, parent, false)
        folderName = inflatedView.folder_name
        folderNewMessages = inflatedView.folder_new_messages
        inflatedView.folder_layout.setOnClickListener { onFolderClicked() }
        return inflatedView
    }

    override fun render() {
        folderName.text =
                FolderInfoHolder.getDisplayName(context, content.account, content.localFolder.name)
        renderUnreadMessages(content.unreadCount)
    }

    private suspend fun getUnreadMessageCount(): Int = withContext(Dispatchers.IO) {
        content.localFolder.unreadMessageCount
    }

    private fun renderUnreadMessages(unreadMessageCount: Int) {
        when {
            unreadMessageCount > 0 -> {
                folderNewMessages.visibility = View.VISIBLE
                folderNewMessages.text = unreadMessageCount.toString()
            }
            else -> folderNewMessages.visibility = View.GONE
        }
    }

    fun setFolderClickListener(onFolderClickListener: FolderClickListener) {
        this.onFolderClickListener = onFolderClickListener
    }

    private fun onFolderClicked() {
        onFolderClickListener.onClick(content.localFolder)
    }
}
