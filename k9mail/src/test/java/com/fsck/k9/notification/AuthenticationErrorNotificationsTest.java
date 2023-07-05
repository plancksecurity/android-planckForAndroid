package com.fsck.k9.notification;


//@RunWith(K9RobolectricTestRunner.class)
public class AuthenticationErrorNotificationsTest {
//    private static final boolean INCOMING = true;
//    private static final boolean OUTGOING = false;
//    private static final int ACCOUNT_NUMBER = 1;
//    private static final String ACCOUNT_NAME = "TestAccount";
//
//
//    private NotificationManagerCompat notificationManager;
//    private NotificationCompat.Builder builder;
//    private NotificationController controller;
//    private Account account;
//    private AuthenticationErrorNotifications authenticationErrorNotifications;
//    private PendingIntent contentIntent;
//
//
//    @Before
//    public void setUp() throws Exception {
//        notificationManager = createFakeNotificationManager();
//        builder = createFakeNotificationBuilder();
//        controller = createFakeNotificationController(notificationManager, builder);
//        account = createFakeAccount();
//        contentIntent = createFakeContentIntent();
//
//        authenticationErrorNotifications = new TestAuthenticationErrorNotifications();
//    }
//
//    @Test
//    public void showAuthenticationErrorNotification_withIncomingServer_shouldCreateNotification() throws Exception {
//        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);
//
//        authenticationErrorNotifications.showAuthenticationErrorNotification(account, INCOMING);
//
//        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
//        assertAuthenticationErrorNotificationContents();
//    }
//
//    @Test
//    public void clearAuthenticationErrorNotification_withIncomingServer_shouldCancelNotification() throws Exception {
//        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, INCOMING);
//
//        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, INCOMING);
//
//        verify(notificationManager).cancel(notificationId);
//    }
//
//    @Test
//    public void showAuthenticationErrorNotification_withOutgoingServer_shouldCreateNotification() throws Exception {
//        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);
//
//        authenticationErrorNotifications.showAuthenticationErrorNotification(account, OUTGOING);
//
//        verify(notificationManager).notify(eq(notificationId), any(Notification.class));
//        assertAuthenticationErrorNotificationContents();
//    }
//
//    @Test
//    public void clearAuthenticationErrorNotification_withOutgoingServer_shouldCancelNotification() throws Exception {
//        int notificationId = NotificationIds.getAuthenticationErrorNotificationId(account, OUTGOING);
//
//        authenticationErrorNotifications.clearAuthenticationErrorNotification(account, OUTGOING);
//
//        verify(notificationManager).cancel(notificationId);
//    }
//
//    private void assertAuthenticationErrorNotificationContents() {
//        verify(builder).setSmallIcon(R.drawable.notification_icon_warning);
//        verify(builder).setTicker("Authentication failed");
//        verify(builder).setContentTitle("Authentication failed");
//        verify(builder).setContentText("Authentication failed for " + ACCOUNT_NAME + ". Update your server settings.");
//        verify(builder).setContentIntent(contentIntent);
//        verify(builder).setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//    }
//
//    private NotificationManagerCompat createFakeNotificationManager() {
//        return mock(NotificationManagerCompat.class);
//    }
//
//    private Builder createFakeNotificationBuilder() {
//        return MockHelper.mockBuilder(NotificationCompat.Builder.class);
//    }
//
//    private NotificationController createFakeNotificationController(NotificationManagerCompat notificationManager,
//            NotificationCompat.Builder builder) {
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
//
//    class TestAuthenticationErrorNotifications extends AuthenticationErrorNotifications {
//        public TestAuthenticationErrorNotifications() {
//            super(controller);
//        }
//
//        @Override
//        PendingIntent createContentIntent(Context context, Account account, boolean incoming) {
//            return contentIntent;
//        }
//    }
}