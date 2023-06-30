package com.fsck.k9.notification;


import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.List;

public class NotificationController {
    private static final int NOTIFICATION_LED_ON_TIME = 500;
    private static final int NOTIFICATION_LED_OFF_TIME = 2000;
    private static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    private static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;
    static final int NOTIFICATION_LED_BLINK_SLOW = 0;

    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final CertificateErrorNotificationController certificateErrorNotificationController;
    private final AuthenticationErrorNotificationController authenticationErrorNotificationController;
    private final SyncNotificationController syncNotificationController;
    private final SendFailedNotificationController sendFailedNotificationController;
    private final NewMailNotifications newMailNotifications;

    private final NotificationChannelManager channelUtils;

    public static NotificationController newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        return new NotificationController(appContext, notificationManager);
    }

    NotificationController(Context context, NotificationManagerCompat notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.channelUtils = new NotificationChannelManager(context, Preferences.getPreferences(context));
        NotificationResourceProvider notificationResourceProvider = new PlanckNotificationResourceProvider(context);
        NotificationHelper notificationHelper = new NotificationHelper(context, notificationManager, channelUtils, notificationResourceProvider);
        NotificationActionCreator actionBuilder = new NotificationActionCreator(context);
        certificateErrorNotificationController = new CertificateErrorNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        authenticationErrorNotificationController = new AuthenticationErrorNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        syncNotificationController = new SyncNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        sendFailedNotificationController = new SendFailedNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        newMailNotifications = NewMailNotifications.newInstance(this, actionBuilder);
    }

    public void showCertificateErrorNotification(Account account, boolean incoming) {
        certificateErrorNotificationController.showCertificateErrorNotification(account, incoming);
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        certificateErrorNotificationController.clearCertificateErrorNotifications(account, incoming);
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotificationController.showAuthenticationErrorNotification(account, incoming);
    }

    public void clearAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotificationController.clearAuthenticationErrorNotification(account, incoming);
    }

    public void showSendingNotification(Account account) {
        syncNotificationController.showSendingNotification(account);
    }

    public void clearSendingNotification(Account account) {
        syncNotificationController.clearSendingNotification(account);
    }

    public void showSendFailedNotification(Account account, Exception exception) {
        sendFailedNotificationController.showSendFailedNotification(account, exception);
    }

    public void clearSendFailedNotification(Account account) {
        sendFailedNotificationController.clearSendFailedNotification(account);
    }

    public void showFetchingMailNotification(Account account, Folder folder) {
        syncNotificationController.showFetchingMailNotification(account, folder);
    }

    public void clearFetchingMailNotification(Account account) {
        syncNotificationController.clearFetchingMailNotification(account);
    }

    public void addNewMailsNotification(Account account, List<LocalMessage> messages, int previousUnreadMessageCount) {
        newMailNotifications.addNewMailsNotification(account, messages, previousUnreadMessageCount);
    }

    public void removeNewMailNotification(Account account, MessageReference messageReference) {
        newMailNotifications.removeNewMailNotification(account, messageReference);
    }

    public void clearNewMailNotifications(Account account) {
        newMailNotifications.clearNewMailNotifications(account);
    }

    public void clearNewMailNotifications(Account account, String folderName) {
        newMailNotifications.clearNewMailNotifications(account, folderName);
    }

    void configureNotification(NotificationCompat.Builder builder, String ringtone, long[] vibrationPattern,
            Integer ledColor, int ledSpeed, boolean ringAndVibrate) {

        if (K9.isQuietTime()) {
            return;
        }

        if (ringAndVibrate) {
            if (ringtone != null && !TextUtils.isEmpty(ringtone)) {
                builder.setSound(Uri.parse(ringtone));
            }

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern);
            }
        }

        if (ledColor != null) {
            int ledOnMS;
            int ledOffMS;
            if (ledSpeed == NOTIFICATION_LED_BLINK_SLOW) {
                ledOnMS = NOTIFICATION_LED_ON_TIME;
                ledOffMS = NOTIFICATION_LED_OFF_TIME;
            } else {
                ledOnMS = NOTIFICATION_LED_FAST_ON_TIME;
                ledOffMS = NOTIFICATION_LED_FAST_OFF_TIME;
            }

            builder.setLights(ledColor, ledOnMS, ledOffMS);
        }
    }

    public void updateChannels() {
        channelUtils.updateChannels();
    }

    String getAccountName(Account account) {
        String accountDescription = account.getDescription();
        return TextUtils.isEmpty(accountDescription) ? account.getEmail() : accountDescription;
    }

    Context getContext() {
        return context;
    }

    NotificationManagerCompat getNotificationManager() {
        return notificationManager;
    }

    NotificationCompat.Builder createNotificationBuilder(Account account, NotificationChannelManager.ChannelType channelType) {
        return new NotificationCompat.Builder(context,
                channelUtils.getChannelIdFor(account, channelType));
    }
}
