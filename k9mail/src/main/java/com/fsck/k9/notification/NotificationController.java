package com.fsck.k9.notification;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.fsck.k9.Account;
import com.fsck.k9.Clock;
import com.fsck.k9.Preferences;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalMessage;

import java.util.List;

public class NotificationController {
    private final CertificateErrorNotificationController certificateErrorNotificationController;
    private final AuthenticationErrorNotificationController authenticationErrorNotificationController;
    private final SyncNotificationController syncNotificationController;
    private final SendFailedNotificationController sendFailedNotificationController;
    private final NewMailNotificationController newMailNotificationController;

    private final NotificationChannelManager channelUtils;

    public static NotificationController newInstance(Context context) {
        Context appContext = context.getApplicationContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(appContext);
        return new NotificationController(appContext, notificationManager);
    }

    NotificationController(Context context, NotificationManagerCompat notificationManager) {
        this.channelUtils = new NotificationChannelManager(context, Preferences.getPreferences(context));
        NotificationResourceProvider notificationResourceProvider = new PlanckNotificationResourceProvider(context);
        NotificationHelper notificationHelper = new NotificationHelper(context, notificationManager, channelUtils, notificationResourceProvider);
        NotificationActionCreator actionBuilder = new NotificationActionCreator(context);
        certificateErrorNotificationController = new CertificateErrorNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        authenticationErrorNotificationController = new AuthenticationErrorNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        syncNotificationController = new SyncNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        sendFailedNotificationController = new SendFailedNotificationController(notificationHelper, actionBuilder, notificationResourceProvider);
        newMailNotificationController = initializeNewMailNotificationController(
                context,
                notificationManager,
                notificationResourceProvider,
                notificationHelper,
                actionBuilder
        );
    }

    @NonNull
    private NewMailNotificationController initializeNewMailNotificationController(
            Context context,
            NotificationManagerCompat notificationManager,
            NotificationResourceProvider notificationResourceProvider,
            NotificationHelper notificationHelper,
            NotificationActionCreator actionBuilder
    ) {
        NotificationContentCreator notificationContentCreator = new NotificationContentCreator(context, notificationResourceProvider);
        NotificationDataStore notificationDataStore = new NotificationDataStore();
        NotificationRepository notificationRepository = new NotificationRepository(notificationDataStore);
        BaseNotificationDataCreator baseNotificationDataCreator = new BaseNotificationDataCreator();
        SingleMessageNotificationDataCreator singleMessageNotificationDataCreator = new SingleMessageNotificationDataCreator();
        SummaryNotificationDataCreator summaryNotificationDataCreator = new SummaryNotificationDataCreator(singleMessageNotificationDataCreator);
        NewMailNotificationManager newMailNotificationManager = new NewMailNotificationManager(
                notificationContentCreator,
                notificationRepository,
                baseNotificationDataCreator,
                singleMessageNotificationDataCreator,
                summaryNotificationDataCreator,
                Clock.INSTANCE
        );
        LockScreenNotificationCreator lockScreenNotificationCreator = new LockScreenNotificationCreator(
                notificationHelper, notificationResourceProvider
        );
        SingleMessageNotificationCreator singleMessageNotificationCreator = new SingleMessageNotificationCreator(
                notificationHelper, actionBuilder, notificationResourceProvider, lockScreenNotificationCreator
        );
        SummaryNotificationCreator summaryNotificationCreator = new SummaryNotificationCreator(
                notificationHelper, actionBuilder, lockScreenNotificationCreator, singleMessageNotificationCreator, notificationResourceProvider
        );
        return new NewMailNotificationController(
                notificationManager,
                newMailNotificationManager,
                summaryNotificationCreator,
                singleMessageNotificationCreator
        );
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
        newMailNotificationController.addNewMailsNotification(account, messages);
    }

    public void removeNewMailNotification(Account account, MessageReference messageReference) {
        newMailNotificationController.removeNewMailNotification(account, messageReference);
    }

    public void clearNewMailNotifications(Account account) {
        newMailNotificationController.clearNewMailNotifications(account);
    }

    public void clearNewMailNotifications(Account account, String folderName) {
        newMailNotificationController.clearNewMailNotifications(account, folderName);
    }

    public void updateChannels() {
        channelUtils.updateChannels();
    }
}
