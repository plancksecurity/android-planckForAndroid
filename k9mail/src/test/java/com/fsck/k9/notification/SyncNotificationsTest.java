package com.fsck.k9.notification;


//@RunWith(K9RobolectricTestRunner.class)
public class SyncNotificationsTest {
//    private static final int ACCOUNT_NUMBER = 1;
//    private static final String ACCOUNT_NAME = "TestAccount";
//    private static final String FOLDER_NAME = "Inbox";
//
//
//    private NotificationManagerCompat notificationManager;
//    private Builder builder;
//    private Account account;
//    private SyncNotifications syncNotifications;
//    private PendingIntent contentIntent;
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
//
//        syncNotifications = new SyncNotifications(controller, actionBuilder);
//    }
//
//    @Test
//    public void testShowSendingNotification() throws Exception {
//        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
//
//        syncNotifications.showSendingNotification(account);
//
//        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
//        verify(builder).setSmallIcon(R.drawable.ic_notify_check_mail);
//        verify(builder).setTicker("Sending mail: " + ACCOUNT_NAME);
//        verify(builder).setContentTitle("Sending mail");
//        verify(builder).setContentText(ACCOUNT_NAME);
//        verify(builder).setContentIntent(contentIntent);
//        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//    }
//
//    @Test
//    public void testClearSendingNotification() throws Exception {
//        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
//
//        syncNotifications.clearSendingNotification(account);
//
//        verify(notificationManager).cancel(notificationId);
//    }
//
//    @Test
//    public void testGetFetchingMailNotificationId() throws Exception {
//        Folder folder = createFakeFolder();
//        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
//
//        syncNotifications.showFetchingMailNotification(account, folder);
//
//        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
//        verify(builder).setSmallIcon(R.drawable.ic_notify_check_mail);
//        verify(builder).setTicker("Checking mail: " + ACCOUNT_NAME + ":" + FOLDER_NAME);
//        verify(builder).setContentTitle("Checking mail");
//        verify(builder).setContentText(ACCOUNT_NAME + ":" + FOLDER_NAME);
//        verify(builder).setContentIntent(contentIntent);
//        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//    }
//
//    @Test
//    public void testClearSendFailedNotification() throws Exception {
//        int notificationId = NotificationIds.getFetchingMailNotificationId(account);
//
//        syncNotifications.clearFetchingMailNotification(account);
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
//        when(controller.getAccountName(any(Account.class))).thenReturn(ACCOUNT_NAME);
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
//        when(actionBuilder.createViewFolderPendingIntent(any(Account.class), anyString(), anyInt()))
//                .thenReturn(contentIntent);
//        return actionBuilder;
//    }
//
//    private Folder createFakeFolder() {
//        Folder folder = mock(Folder.class);
//        when(folder.getName()).thenReturn(FOLDER_NAME);
//        return folder;
//    }
}
