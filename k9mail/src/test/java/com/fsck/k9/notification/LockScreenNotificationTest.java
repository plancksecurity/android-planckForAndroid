package com.fsck.k9.notification;


import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9.LockScreenNotificationVisibility;
import com.fsck.k9.MockHelper;
import com.fsck.k9.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO remove AndroidJUnit4
@RunWith(AndroidJUnit4.class)
public class LockScreenNotificationTest {
    private static final String ACCOUNT_NAME = "Hugo";
    private static final int NEW_MESSAGE_COUNT = 3;
    private static final int UNREAD_MESSAGE_COUNT = 4;

    private Builder builder;
    private Builder publicBuilder;
    private LockScreenNotification lockScreenNotification;
    private NotificationData notificationData;


    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        builder = createFakeNotificationBuilder();
        publicBuilder = createFakeNotificationBuilder();
        NotificationController controller = createFakeController(context, publicBuilder);
        Account account = createFakeAccount();
        notificationData = createFakeNotificationData(account);
        lockScreenNotification = new LockScreenNotification(controller);
    }

    @Test
    public void configureLockScreenNotification_NOTHING() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.NOTHING);

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_SECRET);
    }

    @Test
    public void configureLockScreenNotification_APP_NAME() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.APP_NAME);

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
    }

    @Test
    public void configureLockScreenNotification_EVERYTHING() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.EVERYTHING);

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    @Test
    public void configureLockScreenNotification_SENDERS_withSingleMessage() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.SENDERS);
        String senderName = "alice@example.com";
        NotificationContent content = createNotificationContent(senderName);
        NotificationHolder holder = new NotificationHolder(42, content);
        when(notificationData.getNewMessagesCount()).thenReturn(1);
        when(notificationData.getUnreadMessageCount()).thenReturn(1);
        when(notificationData.getHolderForLatestNotification()).thenReturn(holder);

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(publicBuilder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(publicBuilder).setNumber(1);
        verify(publicBuilder).setContentTitle("1 new message");
        verify(publicBuilder).setContentText(senderName);
        verify(builder).setPublicVersion(publicBuilder.build());
    }

    @Test
    public void configureLockScreenNotification_SENDERS_withMultipleMessages() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.SENDERS);
        NotificationContent content1 = createNotificationContent("alice@example.com");
        NotificationContent content2 = createNotificationContent("Bob <bob@example.com>");
        NotificationContent content3 = createNotificationContent("\"Peter Lustig\" <peter@example.com>");
        when(notificationData.getNewMessagesCount()).thenReturn(NEW_MESSAGE_COUNT);
        when(notificationData.getUnreadMessageCount()).thenReturn(UNREAD_MESSAGE_COUNT);
        when(notificationData.getContentForSummaryNotification()).thenReturn(
                Arrays.asList(content1, content2, content3));

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(publicBuilder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(publicBuilder).setNumber(UNREAD_MESSAGE_COUNT);
        verify(publicBuilder).setContentTitle(NEW_MESSAGE_COUNT + " new messages");
        verify(publicBuilder).setContentText(
                "alice@example.com, Bob <bob@example.com>, \"Peter Lustig\" <peter@example.com>");
        verify(builder).setPublicVersion(publicBuilder.build());
    }

    @Test
    public void configureLockScreenNotification_SENDERS_makeSureWeGetEnoughSenderNames() {
        assertTrue(NotificationData.MAX_NUMBER_OF_MESSAGES_FOR_SUMMARY_NOTIFICATION >=
                LockScreenNotification.MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION);
    }

    @Test
    public void createCommaSeparatedListOfSenders_withMoreSendersThanShouldBeDisplayed() {
        NotificationContent content1 = createNotificationContent("alice@example.com");
        NotificationContent content2 = createNotificationContent("bob@example.com");
        NotificationContent content3 = createNotificationContent("cloe@example.com");
        NotificationContent content4 = createNotificationContent("dagobert@example.com");
        NotificationContent content5 = createNotificationContent("ed@example.com");
        NotificationContent content6 = createNotificationContent("fiona@example.com");

        String result = lockScreenNotification.createCommaSeparatedListOfSenders(
                Arrays.asList(content1, content2, content3, content4, content5, content6));

        assertEquals(
                "alice@example.com, bob@example.com, cloe@example.com, dagobert@example.com, ed@example.com", result);
    }

    @Test
    public void configureLockScreenNotification_MESSAGE_COUNT() {
        K9.setLockScreenNotificationVisibility(LockScreenNotificationVisibility.MESSAGE_COUNT);
        when(notificationData.getNewMessagesCount()).thenReturn(NEW_MESSAGE_COUNT);
        when(notificationData.getUnreadMessageCount()).thenReturn(UNREAD_MESSAGE_COUNT);

        lockScreenNotification.configureLockScreenNotification(builder, notificationData);

        verify(publicBuilder).setSmallIcon(R.drawable.notification_icon_new_mail);
        verify(publicBuilder).setNumber(UNREAD_MESSAGE_COUNT);
        verify(publicBuilder).setContentTitle(NEW_MESSAGE_COUNT + " new messages");
        verify(publicBuilder).setContentText(ACCOUNT_NAME);
        verify(builder).setPublicVersion(publicBuilder.build());
    }

    private Account createFakeAccount() {
        Account account = mock(Account.class);
        when(account.getDescription()).thenReturn(ACCOUNT_NAME);
        return account;
    }

    private Builder createFakeNotificationBuilder() {
        Builder builder = MockHelper.mockBuilder(Builder.class);
        when(builder.build()).thenReturn(mock(Notification.class));
        return builder;
    }

    private NotificationController createFakeController(Context context, Builder builder) {
        NotificationController controller = mock(NotificationController.class);
        when(controller.getContext()).thenReturn(context);
        when(controller.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
        when(controller.createNotificationBuilder(any(Account.class),
                any(NotificationChannelManager.ChannelType.class)))
                .thenReturn(builder);

        return controller;
    }

    private NotificationData createFakeNotificationData(Account account) {
        NotificationData notificationData = mock(NotificationData.class);
        when(notificationData.getAccount()).thenReturn(account);

        return notificationData;
    }

    private NotificationContent createNotificationContent(String sender) {
        return new NotificationContent(null, sender, null, null, null, false);
    }
}
