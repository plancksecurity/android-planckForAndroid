package com.fsck.k9.notification;


import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.pEp.infrastructure.exceptions.AppDidntEncryptMessageException;

import java.util.List;

public class NotificationController {
    private static final int NOTIFICATION_LED_ON_TIME = 500;
    private static final int NOTIFICATION_LED_OFF_TIME = 2000;
    private static final int NOTIFICATION_LED_FAST_ON_TIME = 100;
    private static final int NOTIFICATION_LED_FAST_OFF_TIME = 100;
    static final int NOTIFICATION_LED_BLINK_SLOW = 0;
    static final int NOTIFICATION_LED_BLINK_FAST = 1;
    static final int NOTIFICATION_LED_FAILURE_COLOR = 0xffff0000;


    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final CertificateErrorNotifications certificateErrorNotifications;
    private final AuthenticationErrorNotifications authenticationErrorNotifications;
    private final SyncNotifications syncNotifications;
    private final SendFailedNotifications sendFailedNotifications;
    private final NewMailNotifications newMailNotifications;

    private final NotificationChannelManager channelUtils;

    public static NotificationController newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        return new NotificationController(appContext, notificationManager);
    }

    public static boolean platformSupportsExtendedNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean platformSupportsLockScreenNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    NotificationController(Context context, NotificationManagerCompat notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
        this.channelUtils = new NotificationChannelManager(context, Preferences.getPreferences(context));

        NotificationActionCreator actionBuilder = new NotificationActionCreator(context);
        certificateErrorNotifications = new CertificateErrorNotifications(this);
        authenticationErrorNotifications = new AuthenticationErrorNotifications(this);
        syncNotifications = new SyncNotifications(this, actionBuilder);
        sendFailedNotifications = new SendFailedNotifications(this, actionBuilder);
        newMailNotifications = NewMailNotifications.newInstance(this, actionBuilder);
    }

    public void showCertificateErrorNotification(Account account, boolean incoming) {
        certificateErrorNotifications.showCertificateErrorNotification(account, incoming);
    }

    public void clearCertificateErrorNotifications(Account account, boolean incoming) {
        certificateErrorNotifications.clearCertificateErrorNotifications(account, incoming);
    }

    public void showAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotifications.showAuthenticationErrorNotification(account, incoming);
    }

    public void clearAuthenticationErrorNotification(Account account, boolean incoming) {
        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, incoming);
    }

    public void showSendingNotification(Account account) {
        syncNotifications.showSendingNotification(account);
    }

    public void clearSendingNotification(Account account) {
        syncNotifications.clearSendingNotification(account);
    }

    public void showSendFailedNotification(Account account, Exception exception, MessageReference messageReference) {
        sendFailedNotifications.showSendFailedNotification(account, exception, messageReference);
    }

    public void clearSendFailedNotification(Account account) {
        sendFailedNotifications.clearSendFailedNotification(account);
    }

    public void showFetchingMailNotification(Account account, Folder folder) {
        syncNotifications.showFetchingMailNotification(account, folder);
    }

    public void clearFetchingMailNotification(Account account) {
        syncNotifications.clearFetchingMailNotification(account);
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

    public void showAppDidntEncryptMessageNotification(Account account, AppDidntEncryptMessageException exception) {
        sendFailedNotifications.showAppDidntEncryptMessageNotification(account, exception);
    }

    public void clearAppDidntEncryptMessageNotification(Account account) {
        sendFailedNotifications.clearAppDidntEncryptMessageNotification(account);
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
