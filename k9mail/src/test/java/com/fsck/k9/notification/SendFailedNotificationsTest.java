package com.fsck.k9.notification;


//@RunWith(K9RobolectricTestRunner.class)
public class SendFailedNotificationsTest {
//    private static final int ACCOUNT_NUMBER = 1;
//    private static final String ACCOUNT_NAME = "TestAccount";
//
//
//    private NotificationManagerCompat notificationManager;
//    private Builder builder;
//    private Account account;
//    private SendFailedNotifications sendFailedNotifications;
//    private PendingIntent contentIntent;
//    private int notificationId;
//
//
//    @Before
//    public void setUp() throws Exception {
//        notificationManager = createFakeNotificationManager();
//        builder = createFakeNotificationBuilder();
//        NotificationController controller = createFakeNotificationController(notificationManager, builder);
//        account = createFakeAccount();
//        contentIntent = createFakeContentIntent();
//        NotificationActionCreator actionBuilder = createActionBuilder(contentIntent);
//        notificationId = NotificationIds.getSendFailedNotificationId(account);
//
//        sendFailedNotifications = new SendFailedNotifications(controller, actionBuilder);
//    }
//
//    @Test
//    public void testShowSendFailedNotification() throws Exception {
//        Exception exception = new Exception();
//
//        sendFailedNotifications.showSendFailedNotification(account, exception);
//
//        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
//        verify(builder).setSmallIcon(R.drawable.notification_icon_new_mail);
//        verify(builder).setTicker("Failed to send some messages");
//        verify(builder).setContentTitle("Failed to send some messages");
//        verify(builder).setContentText("Exception");
//        verify(builder).setContentIntent(contentIntent);
//        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//    }
//
//    @Test
//    public void testClearSendFailedNotification() throws Exception {
//        sendFailedNotifications.clearSendFailedNotification(account);
//
//        verify(notificationManager).cancel(notificationId);
//    }
//
//    private NotificationManagerCompat createFakeNotificationManager() {
//        return mock(NotificationManagerCompat.class);
//    }
//
//    private Builder createFakeNotificationBuilder() {
//        return MockHelper.mockBuilder(Builder.class);
//    }
//
//    private NotificationController createFakeNotificationController(NotificationManagerCompat notificationManager,
//            Builder builder) {
//        NotificationController controller = mock(NotificationController.class);
//        when(controller.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
//        when(controller.getNotificationManager()).thenReturn(notificationManager);
//        when(controller.createNotificationBuilder()).thenReturn(builder);
//        return controller;
//    }
//
//    private Account createFakeAccount() {
//        Account account = mock(Account.class);
//        when(account.getAccountNumber()).thenReturn(ACCOUNT_NUMBER);
//        when(account.getDescription()).thenReturn(ACCOUNT_NAME);
//
//        return account;
//    }
//
//    private PendingIntent createFakeContentIntent() {
//        return mock(PendingIntent.class);
//    }
//
//    private NotificationActionCreator createActionBuilder(PendingIntent contentIntent) {
//        NotificationActionCreator actionBuilder = mock(NotificationActionCreator.class);
//        when(actionBuilder.createViewFolderListPendingIntent(any(Account.class), anyInt())).thenReturn(contentIntent);
//        return actionBuilder;
//    }
}