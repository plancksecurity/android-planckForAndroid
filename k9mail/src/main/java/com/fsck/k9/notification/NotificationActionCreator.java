package com.fsck.k9.notification;


import static com.fsck.k9.helper.PendingIntentCompat.FLAG_IMMUTABLE;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.TaskStackBuilder;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.FolderList;
import com.fsck.k9.activity.MessageList;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.activity.NotificationDeleteConfirmation;
import com.fsck.k9.activity.compose.MessageActions;
import com.fsck.k9.activity.setup.AccountSetupBasics;
import com.fsck.k9.search.LocalSearch;

import java.util.List;


/**
 * This class contains methods to create the {@link PendingIntent}s for the actions of new mail notifications.
 * <p/>
 * <strong>Note:</strong>
 * We need to take special care to ensure the {@code PendingIntent}s are unique as defined in the documentation of
 * {@link PendingIntent}. Otherwise selecting a notification action might perform the action on the wrong message.
 * <p/>
 * We use the notification ID as {@code requestCode} argument to ensure each notification/action pair gets a unique
 * {@code PendingIntent}.
 */
class NotificationActionCreator {
    private final Context context;


    public NotificationActionCreator(Context context) {
        this.context = context;
    }

    PendingIntent getEditServerSettingsIntent(Account account, boolean incoming) {
        Intent editServerSettingsIntent = incoming ?
                AccountSetupBasics.intentActionEditIncomingSettings(context, account.getUuid()) :
                AccountSetupBasics.intentActionEditOutgoingSettings(context, account.getUuid());

        return PendingIntent.getActivity(context, account.getAccountNumber(), editServerSettingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createMessageComposePendingIntent(MessageReference messageReference) {
        TaskStackBuilder stack = buildMessageComposeBackStack(messageReference);
        return stack.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }
    public PendingIntent createViewMessagePendingIntent(MessageReference messageReference) {
        TaskStackBuilder stack = buildMessageViewBackStack(messageReference);
        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createViewFolderPendingIntent(Account account, String folderName) {
        TaskStackBuilder stack = buildMessageListBackStack(account, folderName);
        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createViewMessagesPendingIntent(Account account, List<MessageReference> messageReferences) {

        TaskStackBuilder stack;
        if (account.goToUnreadMessageSearch()) {
            stack = buildUnreadBackStack(account);
        } else {
            String folderName = getFolderNameOfAllMessages(messageReferences);

            if (folderName == null) {
                stack = buildFolderListBackStack(account);
            } else {
                stack = buildMessageListBackStack(account, folderName);
            }
        }

        return stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createViewFolderListPendingIntent(Account account) {
        TaskStackBuilder stack = buildFolderListBackStack(account);
        return stack.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

    public PendingIntent createDismissAllMessagesPendingIntent(Account account) {
        Intent intent = NotificationActionService.createDismissAllMessagesIntent(context, account);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createDismissMessagePendingIntent(MessageReference messageReference) {

        Intent intent = NotificationActionService.createDismissMessageIntent(context, messageReference);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createReplyPendingIntent(MessageReference messageReference) {
        Intent intent = MessageActions.getActionReplyIntent(context, messageReference);

        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createMarkMessageAsReadPendingIntent(MessageReference messageReference) {
        Intent intent = NotificationActionService.createMarkMessageAsReadIntent(context, messageReference);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createMarkAllAsReadPendingIntent(Account account, List<MessageReference> messageReferences) {
        return getMarkAsReadPendingIntent(account, messageReferences, context,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent getMarkAllAsReadPendingIntent(Account account, List<MessageReference> messageReferences) {
        return getMarkAsReadPendingIntent(account, messageReferences, context,
                PendingIntent.FLAG_NO_CREATE | FLAG_IMMUTABLE);
    }

    private PendingIntent getMarkAsReadPendingIntent(Account account, List<MessageReference> messageReferences, Context context, int flags) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createMarkAllAsReadIntent(context, accountUuid, messageReferences);

        return PendingIntent.getService(context, 0, intent, flags);
    }

    public PendingIntent createDeleteMessagePendingIntent(MessageReference messageReference) {
        if (K9.confirmDeleteFromNotification()) {
            return createDeleteConfirmationPendingIntent(messageReference);
        } else {
            return createDeleteServicePendingIntent(messageReference);
        }
    }

    private PendingIntent createDeleteServicePendingIntent(MessageReference messageReference) {
        Intent intent = NotificationActionService.createDeleteMessageIntent(context, messageReference);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    private PendingIntent createDeleteConfirmationPendingIntent(MessageReference messageReference) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReference);

        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createDeleteAllPendingIntent(Account account, List<MessageReference> messageReferences) {
        if (K9.confirmDeleteFromNotification()) {
            return getDeleteAllConfirmationPendingIntent(messageReferences,
                    PendingIntent.FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
        } else {
            return getDeleteAllServicePendingIntent(account, messageReferences,
                    PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
        }
    }

    public PendingIntent getDeleteAllPendingIntent(Account account, List<MessageReference> messageReferences) {
        if (K9.confirmDeleteFromNotification()) {
            return getDeleteAllConfirmationPendingIntent(messageReferences,
                    PendingIntent.FLAG_NO_CREATE | FLAG_IMMUTABLE);
        } else {
            return getDeleteAllServicePendingIntent(account, messageReferences,
                    PendingIntent.FLAG_NO_CREATE | FLAG_IMMUTABLE);
        }
    }

    private PendingIntent getDeleteAllConfirmationPendingIntent(List<MessageReference> messageReferences,
            int flags) {
        Intent intent = NotificationDeleteConfirmation.getIntent(context, messageReferences);

        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    private PendingIntent getDeleteAllServicePendingIntent(Account account, List<MessageReference> messageReferences,
            int flags) {
        String accountUuid = account.getUuid();
        Intent intent = NotificationActionService.createDeleteAllMessagesIntent(
                context, accountUuid, messageReferences);

        return PendingIntent.getService(context, 0, intent, flags);
    }

    public PendingIntent createArchiveMessagePendingIntent(MessageReference messageReference) {
        Intent intent = NotificationActionService.createArchiveMessageIntent(context, messageReference);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createArchiveAllPendingIntent(Account account, List<MessageReference> messageReferences) {
        Intent intent = NotificationActionService.createArchiveAllIntent(context, account, messageReferences);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    public PendingIntent createMarkMessageAsSpamPendingIntent(MessageReference messageReference) {
        Intent intent = NotificationActionService.createMarkMessageAsSpamIntent(context, messageReference);

        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    private TaskStackBuilder buildAccountsBackStack() {
        TaskStackBuilder stack = TaskStackBuilder.create(context);
        if (!skipAccountsInBackStack()) {
            Intent intent = new Intent(context, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_STARTUP, false);

            stack.addNextIntent(intent);
        }
        return stack;
    }

    private TaskStackBuilder buildFolderListBackStack(Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();

        Intent intent = FolderList.actionHandleAccountIntent(context, account, false);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildUnreadBackStack(final Account account) {
        TaskStackBuilder stack = buildAccountsBackStack();

        LocalSearch search = SettingsActivity.Companion.createUnreadSearch(context, account);
        Intent intent = MessageList.intentDisplaySearch(context, search, true, false, false);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageListBackStack(Account account, String folderName) {
        TaskStackBuilder stack = skipFolderListInBackStack(account, folderName) ?
                buildAccountsBackStack() : buildFolderListBackStack(account);

        LocalSearch search = new LocalSearch(folderName);
        search.addAllowedFolder(folderName);
        search.addAccountUuid(account.getUuid());
        Intent intent = MessageList.intentDisplaySearch(context, search, false, true, true);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageViewBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        String folderName = message.getFolderName();
        TaskStackBuilder stack = buildMessageListBackStack(account, folderName);

        Intent intent = MessageList.actionDisplayMessageIntent(context, message);

        stack.addNextIntent(intent);

        return stack;
    }

    private TaskStackBuilder buildMessageComposeBackStack(MessageReference message) {
        Account account = Preferences.getPreferences(context).getAccount(message.getAccountUuid());
        String folderName = message.getFolderName();
        TaskStackBuilder stack = buildMessageListBackStack(account, folderName);
        Intent intent = MessageCompose.actionEditDraftIntent(context, message);
        stack.addNextIntent(intent);
        return stack;
    }

    private String getFolderNameOfAllMessages(List<MessageReference> messageReferences) {
        MessageReference firstMessage = messageReferences.get(0);
        String folderName = firstMessage.getFolderName();

        for (MessageReference messageReference : messageReferences) {
            if (!TextUtils.equals(folderName, messageReference.getFolderName())) {
                return null;
            }
        }

        return folderName;
    }

    private boolean skipFolderListInBackStack(Account account, String folderName) {
        return folderName != null && folderName.equals(account.getAutoExpandFolderName());
    }

    private boolean skipAccountsInBackStack() {
        return Preferences.getPreferences(context).getAccounts().size() == 1;
    }
}
