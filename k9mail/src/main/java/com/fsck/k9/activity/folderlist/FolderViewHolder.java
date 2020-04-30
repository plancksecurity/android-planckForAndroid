package com.fsck.k9.activity.folderlist;

import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;

import security.pEp.ui.resources.ResourcesProvider;
import timber.log.Timber;

class FolderViewHolder {
    private TextView folderName;
    private TextView folderStatus;
    private TextView newMessageCount;
    private TextView flaggedMessageCount;
    private View flaggedMessageCountIcon;
    private View newMessageCountWrapper;
    private View flaggedMessageCountWrapper;
    private RelativeLayout activeIcons;

    private Account account;
    private FontSizes fontsSizes;
    private ResourcesProvider resourcesProvider;

    protected void startView(View view, FontSizes fontsSizes, Account account, ResourcesProvider resourcesProvider) {
        folderName = view.findViewById(R.id.folder_name);
        newMessageCount = view.findViewById(R.id.new_message_count);
        flaggedMessageCount = view.findViewById(R.id.flagged_message_count);
        newMessageCountWrapper = view.findViewById(R.id.new_message_count_wrapper);
        flaggedMessageCountWrapper = view.findViewById(R.id.flagged_message_count_wrapper);
        flaggedMessageCountIcon = view.findViewById(R.id.flagged_message_count_icon);

        folderStatus = view.findViewById(R.id.folder_status);
        activeIcons = view.findViewById(R.id.active_icons);

        this.account = account;
        this.fontsSizes = fontsSizes;
        this.resourcesProvider = resourcesProvider;
    }

    public void bindView(FolderListAdapter adapter,
                         String folderStatusText,
                         FolderInfoHolder folder) {
        folderName.setText(folder.displayName);
        if (folderStatusText != null) {
            folderStatus.setText(folderStatusText);
            folderStatus.setVisibility(View.VISIBLE);
        } else {
            folderStatus.setVisibility(View.GONE);
        }

        if (folder.unreadMessageCount == -1) {
            folder.unreadMessageCount = 0;
            try {
                folder.unreadMessageCount = folder.folder.getUnreadMessageCount();
            } catch (Exception e) {
                Timber.e("Unable to get unreadMessageCount for %s:%s", account.getDescription(), folder.name);
            }
        }
        if (folder.unreadMessageCount > 0) {
            newMessageCount.setText(String.format("%d", folder.unreadMessageCount));
            newMessageCountWrapper.setOnClickListener(adapter.createUnreadSearch(account, folder));
        } else {
            newMessageCountWrapper.setVisibility(View.GONE);
        }

        if (folder.flaggedMessageCount == -1) {
            folder.flaggedMessageCount = 0;
            try {
                folder.flaggedMessageCount = folder.folder.getFlaggedMessageCount();
            } catch (Exception e) {
                Timber.e("Unable to get flaggedMessageCount for %s:%s", account.getDescription(), folder.name);
            }
        }

        if (K9.messageListStars() && folder.flaggedMessageCount > 0) {
            flaggedMessageCount.setText(String.format("%d", folder.flaggedMessageCount));
            flaggedMessageCountWrapper.setOnClickListener(adapter.createFlaggedSearch(account, folder));
            flaggedMessageCountWrapper.setVisibility(View.VISIBLE);
            flaggedMessageCountIcon.setBackgroundResource(resourcesProvider.getAttributeResource(R.attr.iconActionFlag));
        } else {
            flaggedMessageCountWrapper.setVisibility(View.GONE);
        }

        activeIcons.setOnClickListener(v ->
                FeedbackTools.showShortFeedback(folderName.getRootView(), R.string.tap_hint)
        );

        fontsSizes.setViewTextSize(folderName, fontsSizes.getFolderName());

        if (K9.wrapFolderNames()) {
            folderName.setEllipsize(null);
            folderName.setSingleLine(false);
        } else {
            folderName.setEllipsize(TextUtils.TruncateAt.START);
            folderName.setSingleLine(true);
        }
        fontsSizes.setViewTextSize(folderStatus, fontsSizes.getFolderStatus());
    }
}
