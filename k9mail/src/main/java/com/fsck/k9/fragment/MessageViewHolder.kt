package com.fsck.k9.fragment

import android.database.Cursor
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.FontSizes
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.DatabasePreviewType
import com.fsck.k9.pEp.ui.PEpContactBadge
import foundation.pEp.jniadapter.Rating
import security.pEp.ui.PEpUIUtils.getDrawableForMessageList
import java.util.*

class MessageViewHolder internal constructor(private val fragment: MessageListFragment,
                                             private val fontSizes: FontSizes,
                                             private val view: View) : View.OnClickListener {
    private var privacyBadge: ImageView? = null
    private var selectedCheckBox: CheckBox? = null
    private var contactBadge: PEpContactBadge? = null
    private var senderTV: TextView? = null
    private var threadCountTV: TextView? = null
    private var dateTV: TextView? = null
    private var subjectTV: TextView? = null
    private var previewTV: TextView? = null
    private var flaggedCB: CheckBox? = null
    private var attachment: ImageView? = null
    private var position = -1
    private var isSelected = false
    private var read = false

    private fun bindViews() {
        privacyBadge = view.findViewById(R.id.privacyBadge)
        selectedCheckBox = view.findViewById(R.id.selectedCheckbox)
        contactBadge = view.findViewById(R.id.contactBadge)
        senderTV = view.findViewById(R.id.sender)
        threadCountTV = view.findViewById(R.id.threadCount)
        dateTV = view.findViewById(R.id.date)
        subjectTV = view.findViewById(R.id.subject)
        flaggedCB = view.findViewById(R.id.flaggedCheckbox)
        attachment = view.findViewById(R.id.attachmentIcon)
        previewTV = view.findViewById(R.id.preview)
    }

    override fun onClick(view: View) {
        if (position != -1) {
            when (view.id) {
                R.id.selectedCheckbox -> fragment.toggleMessageSelectWithAdapterPosition(position)
                R.id.flaggedCheckbox -> fragment.toggleMessageFlagWithAdapterPosition(position)
            }
        }
    }

    fun bind(cursor: Cursor,
             counterpartyAddress: Address?,
             pEpRating: Rating,
             account: Account,
             displayName: CharSequence,
             displayDate: CharSequence,
             isFlagged: Boolean,
             isSelected: Boolean,
             read: Boolean,
             statusHolder: Drawable?,
             subjectText: String,
             threadCount: Int) {
        position = cursor.position
        this.isSelected = isSelected
        this.read = read
        updateThreadCount(threadCount)
        updateDate(displayDate)
        updateFlagCheckbox(isFlagged)
        updateSelectedCheckbox(cursor)
        updatePrivacyBadge(account, pEpRating)
        updateContactBadge(counterpartyAddress)
        updateAttachment(cursor)
        updateNameAndSubject(displayName, statusHolder, subjectText)
        updatePreviewText(cursor)
        changeBackgroundColorIfActiveMessage(cursor, account)
        updateBackgroundColor()
    }

    private fun updateNameAndSubject(displayName: CharSequence, statusHolder: Drawable?, subjectText: String) {
        if (K9.messageListSenderAboveSubject()) {
            fontSizes.setViewTextSize(senderTV, fontSizes.messageListSender)
            fontSizes.setViewTextSize(subjectTV, fontSizes.messageListSubject)
            senderTV?.text = displayName
            senderTV?.setCompoundDrawablesWithIntrinsicBounds(statusHolder, null, null, null)
            subjectTV?.text = subjectText
        } else {
            fontSizes.setViewTextSize(senderTV, fontSizes.messageListSubject)
            fontSizes.setViewTextSize(subjectTV, fontSizes.messageListSender)
            subjectTV?.text = displayName
            subjectTV?.setCompoundDrawablesWithIntrinsicBounds(statusHolder, null, null, null)
            senderTV?.text = subjectText
        }
    }

    private fun updateDate(displayDate: CharSequence) {
        fontSizes.setViewTextSize(dateTV, fontSizes.messageListDate)
        dateTV?.text = displayDate
    }

    private fun changeBackgroundColorIfActiveMessage(cursor: Cursor, account: Account) {
        if (fragment.activeMessage != null) {
            val uid = cursor.getString(MLFProjectionInfo.UID_COLUMN)
            val folderName = cursor.getString(MLFProjectionInfo.FOLDER_NAME_COLUMN)
            if (account.uuid == fragment.activeMessage.accountUuid && folderName == fragment.activeMessage.folderName && uid == fragment.activeMessage.uid) {
                val color = fragment.getColorFromAttributeResource(R.attr.messageListActiveItemBackgroundColor)
                view.setBackgroundColor(color)
            }
        }
    }

    private fun updatePreviewText(cursor: Cursor) {
        fontSizes.setViewTextSize(previewTV, fontSizes.messageListPreview)
        val preview = getPreview(cursor)
        previewTV?.text = preview
        previewTV?.maxLines = Math.max(K9.messageListPreviewLines(), 1)
    }

    private fun updateBackgroundColor() {
        val attribute = if (isSelected) R.attr.messageListSelectedBackgroundColor else if (read && K9.useBackgroundAsUnreadIndicator()) R.attr.messageListReadItemBackgroundColor else R.attr.messageListUnreadItemBackgroundColor
        val color = fragment.getColorFromAttributeResource(attribute)
        view.setBackgroundColor(color)
    }

    private fun updateAttachment(cursor: Cursor) {
        val hasAttachments = cursor.getInt(MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN) > 0
        if (hasAttachments) attachment?.visibility = View.VISIBLE
    }

    private fun updateFlagCheckbox(isFlagged: Boolean) {
        flaggedCB?.visibility = if (K9.messageListStars()) View.VISIBLE else View.GONE
        flaggedCB?.setOnClickListener(this)
        flaggedCB?.isChecked = isFlagged
    }

    private fun updateSelectedCheckbox(cursor: Cursor) {
        if (fragment.checkboxes) {
            selectedCheckBox?.visibility = View.VISIBLE
            selectedCheckBox?.setOnClickListener(this)
            val uniqueId = cursor.getLong(fragment.uniqueIdColumn)
            val isSelected = fragment.selected.contains(uniqueId)
            selectedCheckBox?.isChecked = isSelected
        } else {
            selectedCheckBox?.visibility = View.GONE
        }
    }

    private fun updatePrivacyBadge(account: Account, pEpRating: Rating) {
        if (fragment.context != null) {
            if (!account.ispEpPrivacyProtected()) {
                privacyBadge?.visibility = View.GONE
            } else {
                val pEpPrivacyDrawable = getDrawableForMessageList(fragment.context!!, pEpRating)
                privacyBadge?.visibility = if (pEpPrivacyDrawable != null) View.VISIBLE else View.GONE
                if (pEpPrivacyDrawable != null) privacyBadge?.setImageDrawable(pEpPrivacyDrawable)
            }
        }
    }

    private fun updateThreadCount(count: Int) {
        fontSizes.setViewTextSize(threadCountTV, fontSizes.messageListSubject)
        if (count > 1) {
            threadCountTV?.text = String.format(Locale.getDefault(), "%d", count)
            threadCountTV?.visibility = View.VISIBLE
        } else {
            threadCountTV?.visibility = View.GONE
        }
    }

    private fun updateContactBadge(counterpartyAddress: Address?) {
        if (fragment.contactsPictureLoader == null) {
            contactBadge?.visibility = View.GONE
        } else if (counterpartyAddress != null) {
            Utility.setContactForBadge(contactBadge, counterpartyAddress)
            //   contactBadge.setPadding(0, 0, 0, 0);
            //   contactBadge.setPadding(0, 0, 0, 0);
            contactBadge?.let {
                fragment.contactsPictureLoader.setContactPicture(it, counterpartyAddress)
            }
        } else {
            contactBadge?.assignContactUri(null)
            contactBadge?.setImageResource(R.drawable.ic_contact_picture)
        }
    }

    private fun getPreview(cursor: Cursor): String {
        val previewTypeString = cursor.getString(MLFProjectionInfo.PREVIEW_TYPE_COLUMN)
        val previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString)
        return when (previewType) {
            DatabasePreviewType.NONE, DatabasePreviewType.ERROR -> {
                ""
            }
            DatabasePreviewType.ENCRYPTED -> {
                fragment.getString(R.string.preview_encrypted)
            }
            DatabasePreviewType.TEXT -> {
                cursor.getString(MLFProjectionInfo.PREVIEW_COLUMN)
            }
        }
        throw AssertionError("Unknown preview type: $previewType")
    }

    init {
        bindViews()
    }
}