package com.fsck.k9.notification;


import android.app.Notification;
import android.app.PendingIntent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.Account;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


//TODO remove AndroidJUnit4
@RunWith(AndroidJUnit4.class)
public class SendFailedNotificationsTest {
    private static final int ACCOUNT_NUMBER = 1;
    private static final String ACCOUNT_NAME = "TestAccount";


    private NotificationManagerCompat notificationManager;
    private Builder builder;
    private Account account;
    private SendFailedNotifications sendFailedNotifications;
    private PendingIntent contentIntent;
    private int notificationId;


    @Before
    public void setUp() {
        notificationManager = createFakeNotificationManager();
        builder = createFakeNotificationBuilder();
        NotificationController controller = createFakeNotificationController(notificationManager, builder);
        account = createFakeAccount();
        contentIntent = createFakeContentIntent();
        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);
        notificationId = NotificationIds.getSendFailedNotificationId(account);

        sendFailedNotifications = new SendFailedNotifications(controller, actionBuilder);
    }

    @Test
    public void testShowSendFailedNotification() {
        Exception exception = new Exception();

        sendFailedNotifications.showSendFailedNotification(account, exception);

        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(builder).setTicker("Failed to send some messages");
        verify(builder).setContentTitle("Failed to send some messages");
        verify(builder).setContentText("Exception");
        verify(builder).setContentIntent(contentIntent);
        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Test
    public void testClearSendFailedNotification() {
        sendFailedNotifications.clearSendFailedNotification(account);

        verify(notificationManager).cancel(notificationId);
    }

    private NotificationManagerCompat createFakeNotificationManager() {
        return mock(NotificationManagerCompat.class);
    }

    private Builder createFakeNotificationBuilder() {
        return MockHelper.mockBuilder(Builder.class);
    }

    private NotificationController createFakeNotificationController(NotificationManagerCompat notificationManager,
                                                                    Builder builder) {
        NotificationController controller = mock(NotificationController.class);
        when(controller.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
        when(controller.getNotificationManager()).thenReturn(notificationManager);
        when(controller.createNotificationBuilder(any(Account.class),
                any(NotificationChannelManager.ChannelType.class)))
                .thenReturn(builder);
        return controller;
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
        when(account.getDescription()).thenReturn(ACCOUNT_NAME);

        return account;
    }

    private PendingIntent createFakeContentIntent() {
        return mock(PendingIntent.class);
    }

    private NotificationActionCreator createActionBuilder(PendingIntent contentIntent) {
        NotificationActionCreator actionBuilder = mock(NotificationActionCreator.class);
        when(actionBuilder.createViewFolderListPendingIntent(any(Account.class), anyInt())).thenReturn(contentIntent);
        return actionBuilder;
    }
}
