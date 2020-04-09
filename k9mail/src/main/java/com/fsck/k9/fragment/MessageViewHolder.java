package com.fsck.k9.fragment;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.DatabasePreviewType;
import com.fsck.k9.pEp.ui.PEpContactBadge;

import java.util.Locale;

import foundation.pEp.jniadapter.Rating;
import security.pEp.ui.PEpUIUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.fsck.k9.fragment.MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FOLDER_NAME_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_TYPE_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.UID_COLUMN;

public class MessageViewHolder implements View.OnClickListener {
    private final MessageListFragment fragment;
    private final FontSizes fontSizes;
    private final View view;


    private ImageView securityBadge;
    private CheckBox selectedCheckBox;
    private PEpContactBadge contactBadge;

    private TextView senderTV;
    private TextView threadCountTV;
    private TextView dateTV;

    private TextView subjectTV;

    private TextView previewTV;
    private CheckBox flaggedCB;
    private ImageView attachment;

    public int position = -1;
    private boolean isSelected, read;

    MessageViewHolder(MessageListFragment fragment,
                      FontSizes fontSizes,
                      View view) {
        this.fragment = fragment;
        this.view = view;
        this.fontSizes = fontSizes;
        bindViews();
    }

    private void bindViews() {
        securityBadge = view.findViewById(R.id.securityBadge);
        selectedCheckBox = view.findViewById(R.id.selectedCheckbox);
        contactBadge = view.findViewById(R.id.contactBadge);

        senderTV = view.findViewById(R.id.sender);
        threadCountTV = view.findViewById(R.id.threadCount);
        dateTV = view.findViewById(R.id.date);

        subjectTV = view.findViewById(R.id.subject);
        flaggedCB = view.findViewById(R.id.flaggedCheckbox);
        attachment = view.findViewById(R.id.attachmentIcon);

        previewTV = view.findViewById(R.id.preview);
    }

    @Override
    public void onClick(View view) {
        if (position != -1) {
            switch (view.getId()) {
                case R.id.selectedCheckbox:
                    fragment.toggleMessageSelectWithAdapterPosition(position);
                    break;
                case R.id.flaggedCheckbox:
                    fragment.toggleMessageFlagWithAdapterPosition(position);
                    break;
            }
        }
    }

    public void bind(Cursor cursor,
                     Address counterpartyAddress,
                     Rating pEpRating,
                     Account account,
                     CharSequence displayName,
                     CharSequence displayDate,
                     boolean isFlagged,
                     boolean isSelected,
                     boolean read,
                     Drawable statusHolder,
                     String subjectText,
                     int threadCount) {

        this.position = cursor.getPosition();
        this.isSelected = isSelected;
        this.read = read;

        updateThreadCount(threadCount);

        updateDate(displayDate);

        updateFlagCheckbox(isFlagged);

        updateSelectedCheckbox(cursor);

        updateSecurityBadge(pEpRating);

        updateContactBadge(counterpartyAddress);

        updateAttachment(cursor);

        updateNameAndSubject(displayName, statusHolder, subjectText);

        updatePreviewText(cursor);

        changeBackgroundColorIfActiveMessage(cursor, account);

        updateBackgroundColor();
    }

    private void updateNameAndSubject(CharSequence displayName, Drawable statusHolder, String subjectText) {
        if (K9.messageListSenderAboveSubject()) {
            fontSizes.setViewTextSize(senderTV, fontSizes.getMessageListSender());
            fontSizes.setViewTextSize(subjectTV, fontSizes.getMessageListSubject());
            senderTV.setText(displayName);
            senderTV.setCompoundDrawablesWithIntrinsicBounds(statusHolder, null, null, null);
            subjectTV.setText(subjectText);
        } else {
            fontSizes.setViewTextSize(senderTV, fontSizes.getMessageListSubject());
            fontSizes.setViewTextSize(subjectTV, fontSizes.getMessageListSender());
            subjectTV.setText(displayName);
            subjectTV.setCompoundDrawablesWithIntrinsicBounds(statusHolder, null, null, null);
            senderTV.setText(subjectText);
        }
    }

    private void updateDate(CharSequence displayDate) {
        fontSizes.setViewTextSize(dateTV, fontSizes.getMessageListDate());
        dateTV.setText(displayDate);
    }


    private void changeBackgroundColorIfActiveMessage(Cursor cursor, Account account) {
        if (fragment.activeMessage != null) {
            String uid = cursor.getString(UID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);

            if (account.getUuid().equals(fragment.activeMessage.getAccountUuid()) &&
                    folderName.equals(fragment.activeMessage.getFolderName()) &&
                    uid.equals(fragment.activeMessage.getUid())) {
                int color = fragment.getColorFromAttributeResource(R.attr.messageListActiveItemBackgroundColor);

                view.setBackgroundColor(color);
            }
        }
    }

    private void updatePreviewText(Cursor cursor) {
        fontSizes.setViewTextSize(previewTV, fontSizes.getMessageListPreview());
        String preview = getPreview(cursor);
        previewTV.setText(preview);
        previewTV.setMaxLines(Math.max(K9.messageListPreviewLines(), 1));
    }

    private void updateBackgroundColor() {
        int attribute = isSelected ? R.attr.messageListSelectedBackgroundColor :
                read && K9.useBackgroundAsUnreadIndicator() ?
                        R.attr.messageListReadItemBackgroundColor :
                        R.attr.messageListUnreadItemBackgroundColor;

        int color = fragment.getColorFromAttributeResource(attribute);

        view.setBackgroundColor(color);
    }

    private void updateAttachment(Cursor cursor) {
        boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);
        if (hasAttachments)
            attachment.setVisibility(VISIBLE);
    }

    private void updateFlagCheckbox(boolean isFlagged) {
        flaggedCB.setVisibility(K9.messageListStars() ? VISIBLE : GONE);
        flaggedCB.setOnClickListener(this);
        flaggedCB.setChecked(isFlagged);
    }

    private void updateSelectedCheckbox(Cursor cursor) {
        if (fragment.checkboxes) {
            selectedCheckBox.setVisibility(VISIBLE);
            selectedCheckBox.setOnClickListener(this);
            long uniqueId = cursor.getLong(fragment.uniqueIdColumn);
            boolean isSelected = fragment.selected.contains(uniqueId);
            selectedCheckBox.setChecked(isSelected);
        } else {
            selectedCheckBox.setVisibility(GONE);
        }
    }

    private void updateSecurityBadge(Rating pEpRating) {
        if (fragment.getContext() != null) {
            Drawable pepSecurityBadge = PEpUIUtils.getDrawableForMessageList(fragment.getContext(), pEpRating);
            securityBadge.setVisibility(pepSecurityBadge != null ? VISIBLE : GONE);
            if (pepSecurityBadge != null)
                securityBadge.setImageDrawable(pepSecurityBadge);

        }
    }

    private void updateThreadCount(int count) {
        fontSizes.setViewTextSize(threadCountTV, fontSizes.getMessageListSubject());
        if (count > 1) {
            threadCountTV.setText(String.format(Locale.getDefault(), "%d", count));
            threadCountTV.setVisibility(VISIBLE);
        } else {
            threadCountTV.setVisibility(GONE);
        }
    }

    private void updateContactBadge(Address counterpartyAddress) {
        if (!K9.showContactPicture()) {
            contactBadge.setVisibility(GONE);
        } else if (counterpartyAddress != null) {
            Utility.setContactForBadge(contactBadge, counterpartyAddress);
            //   contactBadge.setPadding(0, 0, 0, 0);
            fragment.contactsPictureLoader.setContactPicture(contactBadge, counterpartyAddress);
        } else {
            contactBadge.assignContactUri(null);
            contactBadge.setImageResource(R.drawable.ic_contact_picture);
        }
    }

    private String getPreview(Cursor cursor) {
        String previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN);
        DatabasePreviewType previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);

        switch (previewType) {
            case NONE:
            case ERROR: {
                return "";
            }
            case ENCRYPTED: {
                return fragment.getString(R.string.preview_encrypted);
            }
            case TEXT: {
                return cursor.getString(PREVIEW_COLUMN);
            }
        }

        throw new AssertionError("Unknown preview type: " + previewType);
    }
}


