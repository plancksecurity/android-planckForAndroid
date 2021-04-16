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
import com.fsck.k9.pEp.infrastructure.exceptions.AppDidntEncryptMessageException;
import com.fsck.k9.pEp.infrastructure.exceptions.MessageRelatedException;

import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_BLINK_FAST;
import static com.fsck.k9.notification.NotificationController.NOTIFICATION_LED_FAILURE_COLOR;


class SendFailedNotifications {
    private final NotificationController controller;
    private final NotificationActionCreator actionBuilder;


    public SendFailedNotifications(NotificationController controller, NotificationActionCreator actionBuilder) {
        this.controller = controller;
        this.actionBuilder = actionBuilder;
    }

    public void showMessageRelatedProblemNotification(Account account, MessageRelatedException exception) {
        Context context = controller.getContext();
        int notificationId = NotificationIds.getMessageRelatedErrorNotificationId(account);

        String title = context.getString(
                (exception instanceof AppDidntEncryptMessageException)
                ? R.string.notification_failed_to_encrypt_title
                : R.string.notification_message_related_problem_title
        );

        String text = context.getString(R.string.notification_failed_to_encrypt_text);
        MessageReference messageReference = new MessageReference(account.getUuid(),
                account.getDraftsFolderName(), exception.getMessageReference().getUid(), Flag.X_PEP_WASNT_ENCRYPTED);
        PendingIntent folderListPendingIntent = actionBuilder.createMessageComposePendingIntent(messageReference, notificationId);
        NotificationCompat.Builder builder = prepareNotificationBuilder(account, title, text, folderListPendingIntent, notificationId);
        getNotificationManager().notify(notificationId, builder.build());
    }

    public void showSendFailedNotification(Account account, Exception exception, MessageReference message) {
        Context context = controller.getContext();
        String title = context.getString(R.string.send_failure_subject);
        String text = ExceptionHelper.getRootCauseMessage(exception);

        int notificationId = NotificationIds.getSendFailedNotificationId(account);
        PendingIntent folderListPendingIntent = actionBuilder.createViewOutboxFolderWithErrorFeedbackIntent(
                account, notificationId, title, text, message);

        NotificationCompat.Builder builder = prepareNotificationBuilder(account, title, text, folderListPendingIntent, notificationId);
        addSendPendingMessagesAction(context, builder, account, notificationId);
        getNotificationManager().notify(notificationId, builder.build());
    }

    private NotificationCompat.Builder prepareNotificationBuilder(Account account, String title, String text,
                                                                  PendingIntent pendingIntent, int notificationId) {
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
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ERROR);

        controller.configureNotification(builder, null, null, NOTIFICATION_LED_FAILURE_COLOR,
                NOTIFICATION_LED_BLINK_FAST, true);
        return builder;
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

    public void clearMessageRelatedProblemNotification(Account account) {
        int notificationId = NotificationIds.getMessageRelatedErrorNotificationId(account);
        getNotificationManager().cancel(notificationId);
    }

    private int getSendFailedNotificationIcon() {
        //TODO: Use a different icon for send failure notifications
        return R.drawable.notification_icon_new_mail;
    }

    private int getSendPendingMessagesActionIcon() {
        return R.drawable.notification_action_send_pending;
    }

    private NotificationManagerCompat getNotificationManager() {
        return controller.getNotificationManager();
    }
}
