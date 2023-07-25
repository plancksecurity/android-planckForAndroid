package com.fsck.k9.notification;


import android.app.Notification;
import android.util.SparseArray;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.List;


/**
 * Handle notifications for new messages.
 * <p>
 * We call the notification shown on the device <em>summary notification</em>, even when there's only one new message.
 * Notifications on an Android Wear device are displayed as a stack of cards and that's why we call them <em>stacked
 * notifications</em>. We have to keep track of stacked notifications individually and recreate/update the summary
 * notification when one or more of the stacked notifications are added/removed.<br>
 * {@link NotificationData} keeps track of all data required to (re)create the actual system notifications.
 * </p>
 */
class NewMailNotifications {
    private static final int FIRST_POSITION = 0;
    private final NotificationContentCreator contentCreator;
    private final DeviceNotifications deviceNotifications;
    private final WearNotifications wearNotifications;
    private final SparseArray<NotificationData> notifications = new SparseArray<NotificationData>();
    private final Object lock = new Object();
    private final NotificationHelper notificationHelper;


    NewMailNotifications(
            NotificationContentCreator contentCreator,
            DeviceNotifications deviceNotifications,
            WearNotifications wearNotifications,
            NotificationHelper notificationHelper
    ) {
        this.deviceNotifications = deviceNotifications;
        this.wearNotifications = wearNotifications;
        this.contentCreator = contentCreator;
        this. notificationHelper = notificationHelper;
    }

    public static NewMailNotifications newInstance(
            NotificationController controller,
            NotificationActionCreator actionCreator,
            NotificationHelper notificationHelper,
            NotificationResourceProvider notificationResourceProvider
    ) {
        NotificationContentCreator contentCreator = new NotificationContentCreator(controller.getContext());
        WearNotifications wearNotifications = new WearNotifications(controller, actionCreator, notificationResourceProvider);
        DeviceNotifications deviceNotifications = DeviceNotifications.newInstance(
                controller, actionCreator, wearNotifications, notificationResourceProvider);
        return new NewMailNotifications(contentCreator, deviceNotifications, wearNotifications, notificationHelper);
    }

    void addNewMailNotification(Account account, LocalMessage message, int unreadMessageCount) {
        addNewMailNotification(account, message, unreadMessageCount, false);
    }

    void addNewMailsNotification(Account account, List<LocalMessage> messages, int unreadMessageCount) {
        for (int position = 0; position < messages.size(); position++) {
            LocalMessage message = messages.get(position);
            addNewMailNotification(account, message, unreadMessageCount, position != FIRST_POSITION);
        }
    }

    private void addNewMailNotification(Account account, LocalMessage message, int unreadMessageCount, boolean silent) {
        NotificationContent content = contentCreator.createFromMessage(account, message);

        synchronized (lock) {
            NotificationData notificationData = getOrCreateNotificationData(account, unreadMessageCount);
            AddNotificationResult result = notificationData.addNotificationContent(content);

            if (result.shouldCancelNotification()) {
                int notificationId = result.getNotificationId();
                cancelNotification(notificationId);
            }

            createStackedNotification(account, result.getNotificationHolder());
            createSummaryNotification(account, notificationData, silent);
        }
    }

    public void removeNewMailNotification(Account account, MessageReference messageReference) {
        synchronized (lock) {
            NotificationData notificationData = getNotificationData(account);
            if (notificationData == null) {
                return;
            }

            RemoveNotificationResult result = notificationData.removeNotificationForMessage(messageReference);
            if (result.isUnknownNotification()) {
                return;
            }

            cancelNotification(result.getNotificationId());

            if (result.shouldCreateNotification()) {
                createStackedNotification(account, result.getNotificationHolder());
            }

            updateSummaryNotification(account, notificationData);
        }
    }

    public void clearNewMailNotifications(Account account) {
        NotificationData notificationData;
        synchronized (lock) {
            notificationData = removeNotificationData(account);
        }

        if (notificationData == null) {
            return;
        }

        for (int notificationId : notificationData.getActiveNotificationIds()) {
            cancelNotification(notificationId);
        }

        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);
        cancelNotification(notificationId);
    }

    public void clearNewMailNotifications(Account account, String folderName) {
        synchronized (lock) {
            NotificationData notificationData = getNotificationData(account);
            if (notificationData == null) {
                return;
            }

            List<MessageReference> allReferences = notificationData.getAllMessageReferences();
            for(MessageReference reference : allReferences) {
                if(reference.getFolderName().equals(folderName)) {
                    removeNewMailNotification(account, reference);
                }
            }
        }
    }

    private NotificationData getOrCreateNotificationData(Account account, int unreadMessageCount) {
        NotificationData notificationData = getNotificationData(account);
        if (notificationData != null) {
            return notificationData;
        }

        int accountNumber = account.getAccountNumber();
        NotificationData newNotificationHolder = createNotificationData(account, unreadMessageCount);
        notifications.put(accountNumber, newNotificationHolder);

        return newNotificationHolder;
    }

    private NotificationData getNotificationData(Account account) {
        int accountNumber = account.getAccountNumber();
        return notifications.get(accountNumber);
    }

    private NotificationData removeNotificationData(Account account) {
        int accountNumber = account.getAccountNumber();
        NotificationData notificationData = notifications.get(accountNumber);
        notifications.remove(accountNumber);
        return notificationData;
    }

    NotificationData createNotificationData(Account account, int unreadMessageCount) {
        NotificationData notificationData = new NotificationData(account);
        notificationData.setUnreadMessageCount(unreadMessageCount);
        return notificationData;
    }

    private void cancelNotification(int notificationId) {
        notificationHelper.getNotificationManager().cancel(notificationId);
    }

    private void updateSummaryNotification(Account account, NotificationData notificationData) {
        if (notificationData.getNewMessagesCount() == 0) {
            clearNewMailNotifications(account);
        } else {
            createSummaryNotification(account, notificationData, true);
        }
    }

    private void createSummaryNotification(Account account, NotificationData notificationData, boolean silent) {
        Notification notification = deviceNotifications.buildSummaryNotification(account, notificationData, silent);
        int notificationId = NotificationIds.getNewMailSummaryNotificationId(account);

        notificationHelper.notify(account, notificationId, notification);
    }

    private void createStackedNotification(Account account, NotificationHolder holder) {
        if (isPrivacyModeEnabled()) {
            return;
        }

        Notification notification = wearNotifications.buildStackedNotification(account, holder);
        int notificationId = holder.notificationId;

        notificationHelper.notify(account, notificationId, notification);
    }

    private boolean isPrivacyModeEnabled() {
        return K9.getNotificationHideSubject() != NotificationHideSubject.NEVER;
    }
}
