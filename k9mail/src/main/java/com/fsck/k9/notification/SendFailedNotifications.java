package com.fsck.k9.notification;


import android.app.PendingIntent;
import android.content.Context;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.ExceptionHelper;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.pEp.infrastructure.exceptions.AppDidntEncryptMessageException;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_FAILURE_COLOR;


class SendFailedNotifications {
    private final NotificationController controller;
    private final NotificationActionCreator actionBuilder;


    public SendFailedNotifications(NotificationController controller, NotificationActionCreator actionBuilder) {
        this.controller = controller;
        this.actionBuilder = actionBuilder;
    }

    public void showSendFailedNotification(Account account, Exception exception, Message message) {
        Context context = controller.getContext();
        String title = context.getString(R.string.send_failure_subject);
        String text = ExceptionHelper.getRootCauseMessage(exception);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        PendingIntent folderListPendingIntent;
        if (exception instanceof AppDidntEncryptMessageException) {
            title = context.getString(R.string.notification_failed_to_encrypt_title);
            text = context.getString(R.string.notification_failed_to_encrypt_text);
            AppDidntEncryptMessageException cannotEncryptEx = (AppDidntEncryptMessageException) exception;
            MessageReference messageReference = new MessageReference(account.getUuid(), account.getDraftsFolderName(), cannotEncryptEx.getMimeMessage().getUid(), Flag.X_PEP_WASNT_ENCRYPTED);
            folderListPendingIntent = actionBuilder.createMessageComposePendingIntent(messageReference, notificationId);
        } else {
            folderListPendingIntent = actionBuilder.createViewOutboxFolderWithErrorFeedbackIntent(
                    account, notificationId, title, text, message);
        }

        NotificationCompat.Builder builder = controller
                .createNotificationBuilder(account, NotificationChannelManager.ChannelType.MISCELLANEOUS)
                .setSmallIcon(getSendFailedNotificationIcon())
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker(title)
                .setContentTitle(title)
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(text)
                )
                .setContentIntent(folderListPendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        if(!(exception instanceof AppDidntEncryptMessageException)) {
            addSendPendingMessagesAction(context, builder, account, notificationId);
        }

        controller.configureNotification(builder, null, null, NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);

        getNotificationManager().notify(notificationId, builder.build());
    }

    private void addSendPendingMessagesAction(Context context, NotificationCompat.Builder builder, Account account, int notificationId) {
        int icon = getSendPendingMessagesActionIcon();
        String title = context.getString(R.string.messageview_decrypt_retry);
        PendingIntent sendPendingMessagesPendingIntent =
                actionBuilder.createSendPendingMessagesPendingIntent(account, notificationId);

        builder.addAction(icon, title, sendPendingMessagesPendingIntent);
    }

    public void clearSendFailedNotification(Account account) {
        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    private int getSendFailedNotificationIcon() {
        //TODO: Use a different icon for send failure notifications
        return R.drawable.notification_icon_new_mail;
    }

    private int getSendPendingMessagesActionIcon() {
        return R.drawable.ic_send_dark;
    }

    private NotificationManagerCompat getNotificationManager() {
        return controller.getNotificationManager();
    }
}
