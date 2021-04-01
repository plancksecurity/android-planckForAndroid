package com.fsck.k9.activity.accountlist

import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.*
import com.fsck.k9.helper.SizeFormatter
import com.fsck.k9.pEp.ui.listeners.IndexedFolderClickListener
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.search.SearchAccount
import security.pEp.ui.resources.ResourcesProvider

class AccountViewHolder(
    private val view: View,
    private val onFolderClickListener: IndexedFolderClickListener,
    private val listener: AccountClickListener,
    private val resourcesProvider: ResourcesProvider,
    private val fontSizes: FontSizes
) : RecyclerView.ViewHolder(view) {

    private var description: TextView = view.findViewById(R.id.description)
    private var email: TextView = view.findViewById(R.id.email)
    private var newMessageCount: TextView = view.findViewById(R.id.new_message_count)
    private var flaggedMessageCount: TextView = view.findViewById(R.id.flagged_message_count)
    private var flaggedMessageCountIcon: View = view.findViewById(R.id.flagged_message_count_icon)
    private var newMessageCountWrapper: View = view.findViewById(R.id.new_message_count_wrapper)
    private var flaggedMessageCountWrapper: View =
        view.findViewById(R.id.flagged_message_count_wrapper)
    private var activeIcons: RelativeLayout = view.findViewById(R.id.active_icons)
    private var folders: ImageButton = view.findViewById(R.id.folders)
    private var settings: ImageButton = view.findViewById(R.id.account_settings)
    private var descriptionUnreadMessages: TextView =
        view.findViewById(R.id.description_unread_messages)
    private var accountsDescriptionLayout =
        view.findViewById<View>(R.id.accounts_description_layout)

    fun bind(account: BaseAccount, stats: AccountStats?) {
        accountsDescriptionLayout.setOnClickListener { onFolderClickListener.onClick(position) }

        bindEmail(stats, account)

        bindDescription(account)

        bindUnreadCount(stats, account)

        fontSizes.setViewTextSize(this.description, fontSizes.accountName)
        fontSizes.setViewTextSize(email, fontSizes.accountDescription)

        bindFolders(account)

        settings.drawable.alpha = 255
        settings.setOnClickListener { listener.settingsClicked(account) }
    }

    private fun bindUnreadCount(stats: AccountStats?, account: BaseAccount) {
        if (stats != null) {
            val unreadMessageCount: Int = stats.unreadMessageCount
            descriptionUnreadMessages.text = String.format("%d", unreadMessageCount)
            newMessageCount.text = String.format("%d", unreadMessageCount)

            flaggedMessageCount.text = String.format("%d", stats.flaggedMessageCount)
            flaggedMessageCountWrapper.visibility =
                when {
                    K9.messageListStars() && stats.flaggedMessageCount > 0 -> View.VISIBLE
                    else -> View.GONE
                }

            flaggedMessageCountWrapper.setOnClickListener { listener.flaggedClicked(account) }
            newMessageCountWrapper.setOnClickListener { listener.unreadClicked(account) }

            activeIcons.setOnClickListener {
                FeedbackTools.showShortFeedback(view, view.context.getString(R.string.tap_hint))
            }

        } else {
            newMessageCountWrapper.visibility = View.GONE
            flaggedMessageCountWrapper.visibility = View.GONE
        }

        flaggedMessageCountIcon.setBackgroundResource(resourcesProvider.getAttributeResource(R.attr.iconFlagButton))
    }

    private fun bindDescription(account: BaseAccount) {
        var description: String? = account.description
        if (description == null || description.isEmpty()) {
            description = account.email
        }

        this.description.text = description
    }

    private fun bindFolders(account: BaseAccount) {
        if (account is SearchAccount) {
            folders.visibility = View.GONE
        } else {
            folders.visibility = View.VISIBLE
            folders.drawable.alpha = 255
            folders.setOnClickListener { listener.foldersClicked(account) }
        }
    }

    private fun bindEmail(stats: AccountStats?, account: BaseAccount) {
        if (stats != null && account is Account && stats.size >= 0) {
            email.text = SizeFormatter.formatSize(view.context, stats.size)
            email.visibility = View.VISIBLE
        } else {
            if (account.email == account.description) {
                email.visibility = View.GONE
            } else {
                email.visibility = View.VISIBLE
                email.text = account.email
            }
        }
    }

    fun setAlpha(alpha: Float) {
        view.alpha = alpha
    }
}