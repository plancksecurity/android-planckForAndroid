package com.fsck.k9.activity.folderlist

import android.text.TextUtils
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.FontSizes
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import security.pEp.ui.resources.ResourcesProvider
import timber.log.Timber

class FolderViewHolder(view: View,
                       private val fontSizes: FontSizes,
                       private val account: Account,
                       private val resourcesProvider: ResourcesProvider) {

    private var folderName: TextView = view.findViewById(R.id.folder_name)
    private var folderStatus: TextView = view.findViewById(R.id.folder_status)
    private var newMessageCount: TextView = view.findViewById(R.id.new_message_count)
    private var flaggedMessageCount: TextView = view.findViewById(R.id.flagged_message_count)
    private var flaggedMessageCountIcon: View = view.findViewById(R.id.flagged_message_count_icon)
    private var newMessageCountWrapper: View = view.findViewById(R.id.new_message_count_wrapper)
    private var flaggedMessageCountWrapper: View = view.findViewById(R.id.flagged_message_count_wrapper)
    private var activeIcons: RelativeLayout = view.findViewById(R.id.active_icons)

    fun bindView(adapter: FolderListAdapter,
                 folderStatusText: String?,
                 folder: FolderInfoHolder) {

        folderName.text = folder.displayName

        if (folderStatusText != null) {
            folderStatus.text = folderStatusText
            folderStatus.visibility = View.VISIBLE
        } else {
            folderStatus.visibility = View.GONE
        }
        if (folder.unreadMessageCount == -1) {
            folder.unreadMessageCount = 0
            try {
                runBlocking {
                    folder.unreadMessageCount = getUnreadMessageCount(folder)
                }
            } catch (e: Exception) {
                Timber.e("Unable to get unreadMessageCount for${account.description} ${folder.name}")
            }
        }
        if (folder.unreadMessageCount > 0) {
            newMessageCount.text = folder.unreadMessageCount.toString()
            newMessageCountWrapper.setOnClickListener(adapter.createUnreadSearch(account, folder))
        } else {
            newMessageCountWrapper.visibility = View.GONE
        }
        if (folder.flaggedMessageCount == -1) {
            folder.flaggedMessageCount = 0
            try {
                runBlocking {
                    folder.flaggedMessageCount = getFlaggedMessageCount(folder)
                }
            } catch (e: Exception) {
                Timber.e("Unable to get flaggedMessageCount for${account.description} ${folder.name}")
            }
        }
        if (K9.messageListStars() && folder.flaggedMessageCount > 0) {
            flaggedMessageCount.text = folder.flaggedMessageCount.toString()
            flaggedMessageCountWrapper.setOnClickListener(adapter.createFlaggedSearch(account, folder))
            flaggedMessageCountWrapper.visibility = View.VISIBLE
            flaggedMessageCountIcon.setBackgroundResource(resourcesProvider.getAttributeResource(R.attr.iconActionFlag))
        } else {
            flaggedMessageCountWrapper.visibility = View.GONE
        }
        activeIcons.setOnClickListener { FeedbackTools.showShortFeedback(folderName.rootView, R.string.tap_hint) }
        fontSizes.setViewTextSize(folderName, fontSizes.folderName)
        if (K9.wrapFolderNames()) {
            folderName.ellipsize = null
            folderName.isSingleLine = false
        } else {
            folderName.ellipsize = TextUtils.TruncateAt.START
            folderName.isSingleLine = true
        }
        fontSizes.setViewTextSize(folderStatus, fontSizes.folderStatus)
    }

    private suspend fun getUnreadMessageCount(folder: FolderInfoHolder): Int = withContext(Dispatchers.IO) {
        folder.folder.unreadMessageCount
    }

    private suspend fun getFlaggedMessageCount(folder: FolderInfoHolder): Int = withContext(Dispatchers.IO) {
        folder.folder.flaggedMessageCount
    }

}