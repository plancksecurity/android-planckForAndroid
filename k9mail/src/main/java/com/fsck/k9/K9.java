
package com.fsck.k9;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.fsck.k9.Account.SortType;
import com.fsck.k9.account.AndroidAccountOAuth2TokenStore;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.UpgradeDatabases;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.ssl.LocalKeyStore;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.Poller;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerApplicationComponent;
import com.fsck.k9.pEp.infrastructure.modules.ApplicationModule;
import com.fsck.k9.pEp.ui.keysync.PEpAddDevice;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.provider.UnreadWidgetProvider;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailService;
import com.fsck.k9.service.ShutdownReceiver;
import com.fsck.k9.service.StorageGoneReceiver;
import com.karumi.dexter.Dexter;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.pEp.jniadapter.AndroidHelper;
import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Sync;
import org.pEp.jniadapter.SyncHandshakeSignal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

@ReportsCrashes(mailTo = "crashreport@prettyeasyprivacy.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class K9 extends Application {
    public static final int POLLING_INTERVAL = 1000;
    private Poller poller;
    private boolean needsFastPoll;
    private boolean isPollingMessages;
    public static final boolean DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE = false;
    public PEpProvider pEpProvider, pEpSyncProvider;
    private boolean ispEpSyncEnabled = BuildConfig.WITH_KEY_SYNC;
    private Account currentAccount;
    private ApplicationComponent component;

    /**
     * Components that are interested in knowing when the K9 instance is
     * available and ready (Android invokes Application.onCreate() after other
     * components') should implement this interface and register using
     * {@link K9#registerApplicationAware(ApplicationAware)}.
     */
    public static interface ApplicationAware {
        /**
         * Called when the Application instance is available and ready.
         *
         * @param application
         *            The application instance. Never <code>null</code>.
         * @throws Exception
         */
        void initializeComponent(Application application);
    }

    public static Application app = null;
    public static AndroidAccountOAuth2TokenStore oAuth2TokenStore = null;
    public static File tempDirectory;
    public static final String LOG_TAG = "k9";

    /**
     * Name of the {@link SharedPreferences} file used to store the last known version of the
     * accounts' databases.
     *
     * <p>
     * See {@link UpgradeDatabases} for a detailed explanation of the database upgrade process.
     * </p>
     */
    private static final String DATABASE_VERSION_CACHE = "database_version_cache";

    /**
     * Key used to store the last known database version of the accounts' databases.
     *
     * @see #DATABASE_VERSION_CACHE
     */
    private static final String KEY_LAST_ACCOUNT_DATABASE_VERSION = "last_account_database_version";

    /**
     * Components that are interested in knowing when the K9 instance is
     * available and ready.
     *
     * @see ApplicationAware
     */
    private static final List<ApplicationAware> observers = new ArrayList<ApplicationAware>();

    /**
     * This will be {@code true} once the initialization is complete and {@link #notifyObservers()}
     * was called.
     * Afterwards calls to {@link #registerApplicationAware(com.fsck.k9.K9.ApplicationAware)} will
     * immediately call {@link com.fsck.k9.K9.ApplicationAware#initializeComponent(K9)} for the
     * supplied argument.
     */
    private static boolean sInitialized = false;

    public enum BACKGROUND_OPS {
        ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    private static String language = "";
    private static Theme theme = Theme.LIGHT;
    private static Theme messageViewTheme = Theme.USE_GLOBAL;
    private static Theme composerTheme = Theme.USE_GLOBAL;
    private static boolean useFixedMessageTheme = true;

    private static final FontSizes fontSizes = new FontSizes();

    private static BACKGROUND_OPS backgroundOps = BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC;
    /**
     * Some log messages can be sent to a file, so that the logs
     * can be read using unprivileged access (eg. Terminal Emulator)
     * on the phone, without adb.  Set to null to disable
     */
    public static final String logFile = null;
    //public static final String logFile = Environment.getExternalStorageDirectory() + "/k9mail/debug.log";

    /**
     * If this is enabled, various development settings will be enabled
     * It should NEVER be on for Market builds
     * Right now, it just governs strictmode
     **/
    public static boolean DEVELOPER_MODE = BuildConfig.DEVELOPER_MODE;


    /**
     * If this is enabled there will be additional logging information sent to
     * Log.d, including protocol dumps.
     * Controlled by Preferences at run-time
     */
    public static boolean DEBUG = false;

    /**
     * If this is enabled than logging that normally hides sensitive information
     * like passwords will show that information.
     */
    public static boolean DEBUG_SENSITIVE = false;

    /**
     * Can create messages containing stack traces that can be forwarded
     * to the development team.
     *
     * Feature is enabled when DEBUG == true
     */
    public static final String ERROR_FOLDER_NAME = "K9mail-errors";

    /**
     * A reference to the {@link SharedPreferences} used for caching the last known database
     * version.
     *
     * @see #checkCachedDatabaseVersion()
     * @see #setDatabasesUpToDate(boolean)
     */
    private static SharedPreferences sDatabaseVersionCache;

    private static boolean mAnimations = true;

    private static boolean mConfirmDelete = false;
    private static boolean mConfirmDiscardMessage = true;
    private static boolean mConfirmDeleteStarred = false;
    private static boolean mConfirmSpam = false;
    private static boolean mConfirmDeleteFromNotification = true;
    private static boolean mConfirmMarkAllRead = true;

    private static NotificationHideSubject sNotificationHideSubject = NotificationHideSubject.NEVER;

    /**
     * Controls when to hide the subject in the notification area.
     */
    public enum NotificationHideSubject {
        ALWAYS,
        WHEN_LOCKED,
        NEVER
    }

    private static NotificationQuickDelete sNotificationQuickDelete = NotificationQuickDelete.NEVER;

    /**
     * Controls behaviour of delete button in notifications.
     */
    public enum NotificationQuickDelete {
        ALWAYS,
        FOR_SINGLE_MSG,
        NEVER
    }

    private static LockScreenNotificationVisibility sLockScreenNotificationVisibility =
        LockScreenNotificationVisibility.MESSAGE_COUNT;

    public enum LockScreenNotificationVisibility {
        EVERYTHING,
        SENDERS,
        MESSAGE_COUNT,
        APP_NAME,
        NOTHING
    }

    /**
     * Controls when to use the message list split view.
     */
    public enum SplitViewMode {
        ALWAYS,
        NEVER,
        WHEN_IN_LANDSCAPE
    }

    private static boolean mMessageListCheckboxes = true;
    private static boolean mMessageListStars = true;
    private static int mMessageListPreviewLines = 2;

    private static boolean mShowCorrespondentNames = true;
    private static boolean mMessageListSenderAboveSubject = false;
    private static boolean mShowContactName = false;
    private static boolean mChangeContactNameColor = false;
    private static int mContactNameColor = 0xff00008f;
    private static boolean sShowContactPicture = true;
    private static boolean mMessageViewFixedWidthFont = false;
    private static boolean mMessageViewReturnToList = false;
    private static boolean mMessageViewShowNext = false;

    private static boolean mGesturesEnabled = true;
    private static boolean mUseVolumeKeysForNavigation = false;
    private static boolean mUseVolumeKeysForListNavigation = false;
    private static boolean mStartIntegratedInbox = false;
    private static boolean mMeasureAccounts = true;
    private static boolean mCountSearchMessages = true;
    private static boolean mHideSpecialAccounts = false;
    private static boolean mAutofitWidth;
    private static boolean mQuietTimeEnabled = false;
    private static boolean mNotificationDuringQuietTimeEnabled = true;
    private static String mQuietTimeStarts = null;
    private static String mQuietTimeEnds = null;
    private static String mAttachmentDefaultPath = "";
    private static boolean mWrapFolderNames = false;
    private static boolean mHideUserAgent = true;
    private static boolean mHideTimeZone = false;

    private static SortType mSortType;
    private static Map<SortType, Boolean> mSortAscending = new HashMap<SortType, Boolean>();

    private static boolean sUseBackgroundAsUnreadIndicator = true;
    private static boolean sThreadedViewEnabled = true;
    private static SplitViewMode sSplitViewMode = SplitViewMode.NEVER;
    private static boolean sColorizeMissingContactPictures = DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE;

    private static boolean sMessageViewArchiveActionVisible = false;
    private static boolean sMessageViewDeleteActionVisible = true;
    private static boolean sMessageViewMoveActionVisible = false;
    private static boolean sMessageViewCopyActionVisible = false;
    private static boolean sMessageViewSpamActionVisible = false;
    private static String pEpExtraAccounts = "";
    private static boolean pEpUseKeyserver = false;
    private static boolean pEpPassiveMode = false;
    private static boolean pEpSubjectUnprotected = false;
    private static boolean pEpForwardWarningEnabled = false;



    private static int sPgpInlineDialogCounter;
    private static int sPgpSignOnlyDialogCounter;


    /**
     * @see #areDatabasesUpToDate()
     */
    private static boolean sDatabasesUpToDate = false;

    /**
     * For use when displaying that no folder is selected
     */
    public static final String FOLDER_NONE = "-NONE-";

    public static final String LOCAL_UID_PREFIX = "K9LOCAL:";

    public static final String REMOTE_UID_PREFIX = "K9REMOTE:";

    public static final String IDENTITY_HEADER = K9MailLib.IDENTITY_HEADER;

    /**
     * Specifies how many messages will be shown in a folder by default. This number is set
     * on each new folder and can be incremented with "Load more messages..." by the
     * VISIBLE_LIMIT_INCREMENT
     */
    public static final int DEFAULT_VISIBLE_LIMIT = 25;

    /**
     * The maximum size of an attachment we're willing to download (either View or Save)
     * Attachments that are base64 encoded (most) will be about 1.375x their actual size
     * so we should probably factor that in. A 5MB attachment will generally be around
     * 6.8MB downloaded but only 5MB saved.
     */
    public static final int MAX_ATTACHMENT_DOWNLOAD_SIZE = (128 * 1024 * 1024);


    /* How many times should K-9 try to deliver a message before giving up
     * until the app is killed and restarted
     */

    public static final int MAX_SEND_ATTEMPTS = 5;

    /**
     * Max time (in millis) the wake lock will be held for when background sync is happening
     */
    public static final int WAKE_LOCK_TIMEOUT = 600000;

    public static final int MANUAL_WAKE_LOCK_TIMEOUT = 120000;

    public static final int PUSH_WAKE_LOCK_TIMEOUT = K9MailLib.PUSH_WAKE_LOCK_TIMEOUT;

    public static final int MAIL_SERVICE_WAKE_LOCK_TIMEOUT = 60000;

    public static final int BOOT_RECEIVER_WAKE_LOCK_TIMEOUT = 60000;


    public static class Intents {

        public static class EmailReceived {
            public static final String ACTION_EMAIL_RECEIVED = BuildConfig.APPLICATION_ID + ".intent.action.EMAIL_RECEIVED";
            public static final String ACTION_EMAIL_DELETED = BuildConfig.APPLICATION_ID + ".intent.action.EMAIL_DELETED";
            public static final String ACTION_REFRESH_OBSERVER = BuildConfig.APPLICATION_ID + ".intent.action.REFRESH_OBSERVER";
            public static final String EXTRA_ACCOUNT = BuildConfig.APPLICATION_ID + ".intent.extra.ACCOUNT";
            public static final String EXTRA_FOLDER = BuildConfig.APPLICATION_ID + ".intent.extra.FOLDER";
            public static final String EXTRA_SENT_DATE = BuildConfig.APPLICATION_ID + ".intent.extra.SENT_DATE";
            public static final String EXTRA_FROM = BuildConfig.APPLICATION_ID + ".intent.extra.FROM";
            public static final String EXTRA_TO = BuildConfig.APPLICATION_ID + ".intent.extra.TO";
            public static final String EXTRA_CC = BuildConfig.APPLICATION_ID + ".intent.extra.CC";
            public static final String EXTRA_BCC = BuildConfig.APPLICATION_ID + ".intent.extra.BCC";
            public static final String EXTRA_SUBJECT = BuildConfig.APPLICATION_ID + ".intent.extra.SUBJECT";
            public static final String EXTRA_FROM_SELF = BuildConfig.APPLICATION_ID + ".intent.extra.FROM_SELF";
        }

        public static class Share {
            /*
             * We don't want to use EmailReceived.EXTRA_FROM ("com.fsck.k9.intent.extra.FROM")
             * because of different semantics (String array vs. string with comma separated
             * email addresses)
             */
            public static final String EXTRA_FROM = BuildConfig.APPLICATION_ID + ".intent.extra.SENDER";
        }
    }

    /**
     * Called throughout the application when the number of accounts has changed. This method
     * enables or disables the Compose activity, the boot receiver and the service based on
     * whether any accounts are configured.
     */
    public static void setServicesEnabled(Context context) {
        int acctLength = Preferences.getPreferences(context).getAvailableAccounts().size();

        setServicesEnabled(context, acctLength > 0, null);

    }

    private static void setServicesEnabled(Context context, boolean enabled, Integer wakeLockId) {

        PackageManager pm = context.getPackageManager();

        if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * If no accounts now exist but the service is still enabled we're about to disable it
             * so we'll reschedule to kill off any existing alarms.
             */
            MailService.actionReset(context, wakeLockId);
        }
        Class<?>[] classes = { MessageCompose.class, BootReceiver.class, MailService.class };

        for (Class<?> clazz : classes) {

            boolean alreadyEnabled = pm.getComponentEnabledSetting(new ComponentName(context, clazz)) ==
                                     PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

            if (enabled != alreadyEnabled) {
                pm.setComponentEnabledSetting(
                    new ComponentName(context, clazz),
                    enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            }
        }

        if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            /*
             * And now if accounts do exist then we've just enabled the service and we want to
             * schedule alarms for the new accounts.
             */
            MailService.actionReset(context, wakeLockId);
        }

    }

    /**
     * Register BroadcastReceivers programmaticaly because doing it from manifest
     * would make K-9 auto-start. We don't want auto-start because the initialization
     * sequence isn't safe while some events occur (SD card unmount).
     */
    protected void registerReceivers() {
        final StorageGoneReceiver receiver = new StorageGoneReceiver();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");

        final BlockingQueue<Handler> queue = new SynchronousQueue<Handler>();

        // starting a new thread to handle unmount events
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    queue.put(new Handler());
                } catch (InterruptedException e) {
                    Log.e(K9.LOG_TAG, "", e);
                }
                Looper.loop();
            }

        }, "Unmount-thread").start();

        try {
            final Handler storageGoneHandler = queue.take();
            registerReceiver(receiver, filter, null, storageGoneHandler);
            Log.i(K9.LOG_TAG, "Registered: unmount receiver");
        } catch (InterruptedException e) {
            Log.e(K9.LOG_TAG, "Unable to register unmount receiver", e);
        }

        registerReceiver(new ShutdownReceiver(), new IntentFilter(Intent.ACTION_SHUTDOWN));
        Log.i(K9.LOG_TAG, "Registered: shutdown receiver");
    }

    public static void save(StorageEditor editor) {
        editor.putBoolean("enableDebugLogging", K9.DEBUG);
        editor.putBoolean("enableSensitiveLogging", K9.DEBUG_SENSITIVE);
        editor.putString("backgroundOperations", K9.backgroundOps.name());
        editor.putBoolean("animations", mAnimations);
        editor.putBoolean("gesturesEnabled", mGesturesEnabled);
        editor.putBoolean("useVolumeKeysForNavigation", mUseVolumeKeysForNavigation);
        editor.putBoolean("useVolumeKeysForListNavigation", mUseVolumeKeysForListNavigation);
        editor.putBoolean("autofitWidth", mAutofitWidth);
        editor.putBoolean("quietTimeEnabled", mQuietTimeEnabled);
        editor.putBoolean("notificationDuringQuietTimeEnabled", mNotificationDuringQuietTimeEnabled);
        editor.putString("quietTimeStarts", mQuietTimeStarts);
        editor.putString("quietTimeEnds", mQuietTimeEnds);

        editor.putBoolean("startIntegratedInbox", mStartIntegratedInbox);
        editor.putBoolean("measureAccounts", mMeasureAccounts);
        editor.putBoolean("countSearchMessages", mCountSearchMessages);
        editor.putBoolean("messageListSenderAboveSubject", mMessageListSenderAboveSubject);
        editor.putBoolean("hideSpecialAccounts", mHideSpecialAccounts);
        editor.putBoolean("messageListStars", mMessageListStars);
        editor.putInt("messageListPreviewLines", mMessageListPreviewLines);
        editor.putBoolean("messageListCheckboxes", mMessageListCheckboxes);
        editor.putBoolean("showCorrespondentNames", mShowCorrespondentNames);
        editor.putBoolean("showContactName", mShowContactName);
        editor.putBoolean("showContactPicture", sShowContactPicture);
        editor.putBoolean("changeRegisteredNameColor", mChangeContactNameColor);
        editor.putInt("registeredNameColor", mContactNameColor);
        editor.putBoolean("messageViewFixedWidthFont", mMessageViewFixedWidthFont);
        editor.putBoolean("messageViewReturnToList", mMessageViewReturnToList);
        editor.putBoolean("messageViewShowNext", mMessageViewShowNext);
        editor.putBoolean("wrapFolderNames", mWrapFolderNames);
        editor.putBoolean("hideUserAgent", mHideUserAgent);
        editor.putBoolean("hideTimeZone", mHideTimeZone);

        editor.putString("language", language);
        editor.putInt("theme", theme.ordinal());
        editor.putInt("messageViewTheme", messageViewTheme.ordinal());
        editor.putInt("messageComposeTheme", composerTheme.ordinal());
        editor.putBoolean("fixedMessageViewTheme", useFixedMessageTheme);

        editor.putBoolean("confirmDelete", mConfirmDelete);
        editor.putBoolean("confirmDiscardMessage", mConfirmDiscardMessage);
        editor.putBoolean("confirmDeleteStarred", mConfirmDeleteStarred);
        editor.putBoolean("confirmSpam", mConfirmSpam);
        editor.putBoolean("confirmDeleteFromNotification", mConfirmDeleteFromNotification);
        editor.putBoolean("confirmMarkAllRead", mConfirmMarkAllRead);

        editor.putString("sortTypeEnum", mSortType.name());
        editor.putBoolean("sortAscending", mSortAscending.get(mSortType));

        editor.putString("notificationHideSubject", sNotificationHideSubject.toString());
        editor.putString("notificationQuickDelete", sNotificationQuickDelete.toString());
        editor.putString("lockScreenNotificationVisibility", sLockScreenNotificationVisibility.toString());

        editor.putString("attachmentdefaultpath", mAttachmentDefaultPath);
        editor.putBoolean("useBackgroundAsUnreadIndicator", sUseBackgroundAsUnreadIndicator);
        editor.putBoolean("threadedView", sThreadedViewEnabled);
        editor.putString("splitViewMode", sSplitViewMode.name());
        editor.putBoolean("colorizeMissingContactPictures", sColorizeMissingContactPictures);

        editor.putBoolean("messageViewArchiveActionVisible", sMessageViewArchiveActionVisible);
        editor.putBoolean("messageViewDeleteActionVisible", sMessageViewDeleteActionVisible);
        editor.putBoolean("messageViewMoveActionVisible", sMessageViewMoveActionVisible);
        editor.putBoolean("messageViewCopyActionVisible", sMessageViewCopyActionVisible);
        editor.putBoolean("messageViewSpamActionVisible", sMessageViewSpamActionVisible);

        editor.putInt("pgpInlineDialogCounter", sPgpInlineDialogCounter);
        editor.putInt("pgpSignOnlyDialogCounter", sPgpSignOnlyDialogCounter);

        editor.putString("pEpExtraAccounts", pEpExtraAccounts);
        editor.putBoolean("pEpUseKeyserver", pEpUseKeyserver);
        editor.putBoolean("pEpPassiveMode", pEpPassiveMode);
        editor.putBoolean("pEpSubjectUnprotected", pEpSubjectUnprotected);
        editor.putBoolean("pEpForwardWarningEnabled", pEpForwardWarningEnabled);

        fontSizes.save(editor);
    }

    @Override
    public void onCreate() {
        pEpInitEnvironment();

        if (K9.DEVELOPER_MODE) {
            StrictMode.enableDefaults();
        }

        PRNGFixes.apply();

        super.onCreate();

        initializeInjector();

        ACRA.init(this);
        pEpSetupUiEngineSession();
        app = this;
        Globals.setContext(this);
        oAuth2TokenStore = new AndroidAccountOAuth2TokenStore(this);

        K9MailLib.setDebugStatus(new K9MailLib.DebugStatus() {
            @Override public boolean enabled() {
                return DEBUG;
            }

            @Override public boolean debugSensitive() {
                return DEBUG_SENSITIVE;
            }
        });

        checkCachedDatabaseVersion();

        Preferences prefs = Preferences.getPreferences(this);
        loadPrefs(prefs);

        /*
         * We have to give MimeMessage a temp directory because File.createTempFile(String, String)
         * doesn't work in Android and MimeMessage does not have access to a Context.
         */
        BinaryTempFileBody.setTempDirectory(getCacheDir());

        LocalKeyStore.setKeyStoreLocation(getDir("KeyStore", MODE_PRIVATE).toString());

        /*
         * Enable background sync of messages
         */

        setServicesEnabled(this);
        registerReceivers();

        MessagingController.getInstance(this).addListener(new MessagingListener() {
            private void broadcastIntent(String action, Account account, String folder, Message message) {
                Uri uri = Uri.parse("email://messages/" + account.getAccountNumber() + "/" + Uri.encode(folder) + "/" + Uri.encode(message.getUid()));
                Intent intent = new Intent(action, uri);
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_ACCOUNT, account.getDescription());
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_FOLDER, folder);
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_SENT_DATE, message.getSentDate());
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_FROM, Address.toString(message.getFrom()));
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_TO, Address.toString(message.getRecipients(Message.RecipientType.TO)));
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_CC, Address.toString(message.getRecipients(Message.RecipientType.CC)));
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_BCC, Address.toString(message.getRecipients(Message.RecipientType.BCC)));
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_SUBJECT, message.getSubject());
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_FROM_SELF, account.isAnIdentity(message.getFrom()));
                K9.this.sendBroadcast(intent);
                if (K9.DEBUG)
                    Log.d(K9.LOG_TAG, "Broadcasted: action=" + action
                          + " account=" + account.getDescription()
                          + " folder=" + folder
                          + " message uid=" + message.getUid()
                         );
            }

            private void updateUnreadWidget() {
                try {
                    UnreadWidgetProvider.updateUnreadCount(K9.this);
                } catch (Exception e) {
                    if (K9.DEBUG) {
                        Log.e(LOG_TAG, "Error while updating unread widget(s)", e);
                    }
                }
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
                updateUnreadWidget();
            }

            @Override
            public void messageDeleted(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
                updateUnreadWidget();
            }

            @Override
            public void synchronizeMailboxNewMessage(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_RECEIVED, account, folder, message);
                updateUnreadWidget();
            }

            @Override
            public void folderStatusChanged(Account account, String folderName,
                    int unreadMessageCount) {

                updateUnreadWidget();

                // let observers know a change occurred
                Intent intent = new Intent(K9.Intents.EmailReceived.ACTION_REFRESH_OBSERVER, null);
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_ACCOUNT, account.getDescription());
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_FOLDER, folderName);
                K9.this.sendBroadcast(intent);

            }

        });

        notifyObservers();
        Dexter.initialize(this);
    }

    private void pEpInitEnvironment() {
        AndroidHelper.setup(this);
        if (ispEpSyncEnabled) {
            initSync();
            setupFastPoller();
        }
    }

    public PEpProvider getpEpSyncProvider() {
        if (ispEpSyncEnabled) return pEpSyncProvider;
        else return pEpProvider;
    }

    private void initSync() {
        pEpSyncProvider = PEpProviderFactory.createAndSetupProvider(this);
        pEpSyncProvider.setSyncHandshakeCallback(new Sync.notifyHandshakeCallback() {

            @Override
            public void notifyHandshake(Identity myself, Identity partner, SyncHandshakeSignal signal) {
                Log.e("pEp", "notifyHandshake: " + signal.name());
                switch (signal) {
                    case SyncNotifyUndefined:
                        break;
                    case SyncNotifyInitAddOurDevice:
                    case SyncNotifyInitAddOtherDevice:
                    case SyncNotifyInitFormGroup:
                        goToAddDevice(myself, partner, signal, getString(R.string.pep_add_device_ask_trustwords));
                        break;
                    case SyncNotifyInitMoveOurDevice:
                        goToAddDevice(myself, partner, signal, getString(R.string.pep_add_device_ask_move_trustwords));
                        break;
                    case SyncNotifyTimeout:
                        //Close handshake
                        new Handler(Looper.getMainLooper()).post(()
                                -> Toast.makeText(K9.this, R.string.pep_keysync_timeout, Toast.LENGTH_SHORT).show());
                        Intent broadcastIntent = new Intent();
                        K9.this.sendOrderedBroadcast(broadcastIntent, null);
                        break;
                    case SyncNotifyAcceptedDeviceAdded:
                    case SyncNotifyAcceptedGroupCreated:
                    case SyncNotifyAcceptedDeviceMoved:
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(K9.this, R.string.pep_device_group, Toast.LENGTH_LONG).show());
                        break;
                }

            }
        });

        pEpSyncProvider.setSyncSendMessageCallback(new Sync.MessageToSendCallback() {
            @Override
            public void messageToSend(org.pEp.jniadapter.Message pEpMessage) {
                try {

                    MessagingController messagingController = MessagingController.getInstance(K9.this);
                    Log.i(AndroidHelper.TAG, "messageToSend: ");

                    loadCurrentAccount(pEpMessage, messagingController);
                    Message message = pEpSyncProvider.getMimeMessage(pEpMessage);
                    message.setFlag(Flag.X_PEP_SYNC_MESSAGE_TO_SEND, true);
                    messagingController.sendMessage(currentAccount, message, null);

                    Log.e("PEPJNI", "messageToSend: " + pEpMessage.getShortmsg());
                    Log.e("PEPJNI", "messageToSend: " + pEpMessage.getLongmsg());
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        });

        pEpSyncProvider.setFastPollingCallback(new Sync.NeedsFastPollCallback() {
            @Override
            public void needsFastPollCallFromC(Boolean fast_poll_needed) {
                needsFastPoll = fast_poll_needed;
                if (needsFastPoll) {
                    poller.startPolling();
                } else {
                    poller.stopPolling();
                }
            }
        });

        pEpSyncProvider.startSync();
    }

    private void goToAddDevice(Identity myself, Identity partner, SyncHandshakeSignal signal, String explanation) {
        Log.i("PEPJNI", "showHandshake: " + signal.name() + " " + myself.toString() + "\n::\n" + partner.toString());

        String trust = pEpSyncProvider.trustwords(myself, partner, language, true);
        Context context = K9.this.getApplicationContext();
        Intent syncTrustowordsActivity = PEpAddDevice.getActionRequestHandshake(context, trust, myself, partner, explanation);
        syncTrustowordsActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 22, syncTrustowordsActivity, 0);
        try {
            pendingIntent.send();
        }
        catch (PendingIntent.CanceledException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void loadCurrentAccount(org.pEp.jniadapter.Message pEpMessage, MessagingController messagingController) {
        List<Account> accounts = Preferences.getPreferences(K9.this).getAccounts();
        currentAccount = null;
        for (Account account : accounts) {
            currentAccount = messagingController.checkAccount(pEpMessage, account);
            if (currentAccount != null) {
                break;
            }
        }
    }

    private void pEpSetupUiEngineSession() {
        registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    /**
     * Loads the last known database version of the accounts' databases from a
     * {@code SharedPreference}.
     *
     * <p>
     * If the stored version matches {@link LocalStore#DB_VERSION} we know that the databases are
     * up to date.<br>
     * Using {@code SharedPreferences} should be a lot faster than opening all SQLite databases to
     * get the current database version.
     * </p><p>
     * See {@link UpgradeDatabases} for a detailed explanation of the database upgrade process.
     * </p>
     *
     * @see #areDatabasesUpToDate()
     */
    public void checkCachedDatabaseVersion() {
        sDatabaseVersionCache = getSharedPreferences(DATABASE_VERSION_CACHE, MODE_PRIVATE);

        int cachedVersion = sDatabaseVersionCache.getInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, 0);

        if (cachedVersion >= LocalStore.DB_VERSION) {
            K9.setDatabasesUpToDate(false);
        }
    }

    /**
     * Load preferences into our statics.
     *
     * If you're adding a preference here, odds are you'll need to add it to
     * {@link com.fsck.k9.preferences.GlobalSettings}, too.
     *
     * @param prefs Preferences to load
     */
    public static void loadPrefs(Preferences prefs) {
        Storage storage = prefs.getStorage();
        DEBUG = storage.getBoolean("enableDebugLogging", BuildConfig.DEVELOPER_MODE);
        DEBUG_SENSITIVE = storage.getBoolean("enableSensitiveLogging", false);
        mAnimations = storage.getBoolean("animations", true);
        mGesturesEnabled = storage.getBoolean("gesturesEnabled", false);
        mUseVolumeKeysForNavigation = storage.getBoolean("useVolumeKeysForNavigation", false);
        mUseVolumeKeysForListNavigation = storage.getBoolean("useVolumeKeysForListNavigation", false);
        mStartIntegratedInbox = storage.getBoolean("startIntegratedInbox", false);
        mMeasureAccounts = storage.getBoolean("measureAccounts", true);
        mCountSearchMessages = storage.getBoolean("countSearchMessages", true);
        mHideSpecialAccounts = storage.getBoolean("hideSpecialAccounts", false);
        mMessageListSenderAboveSubject = storage.getBoolean("messageListSenderAboveSubject", false);
        mMessageListCheckboxes = storage.getBoolean("messageListCheckboxes", false);
        mMessageListStars = storage.getBoolean("messageListStars", true);
        mMessageListPreviewLines = storage.getInt("messageListPreviewLines", 2);

        mAutofitWidth = storage.getBoolean("autofitWidth", true);

        mQuietTimeEnabled = storage.getBoolean("quietTimeEnabled", false);
        mNotificationDuringQuietTimeEnabled = storage.getBoolean("notificationDuringQuietTimeEnabled", true);
        mQuietTimeStarts = storage.getString("quietTimeStarts", "21:00");
        mQuietTimeEnds = storage.getString("quietTimeEnds", "7:00");

        mShowCorrespondentNames = storage.getBoolean("showCorrespondentNames", true);
        mShowContactName = storage.getBoolean("showContactName", false);
        sShowContactPicture = storage.getBoolean("showContactPicture", true);
        mChangeContactNameColor = storage.getBoolean("changeRegisteredNameColor", false);
        mContactNameColor = storage.getInt("registeredNameColor", 0xff00008f);
        mMessageViewFixedWidthFont = storage.getBoolean("messageViewFixedWidthFont", false);
        mMessageViewReturnToList = storage.getBoolean("messageViewReturnToList", false);
        mMessageViewShowNext = storage.getBoolean("messageViewShowNext", false);
        mWrapFolderNames = storage.getBoolean("wrapFolderNames", false);
        mHideUserAgent = storage.getBoolean("hideUserAgent", true);
        mHideTimeZone = storage.getBoolean("hideTimeZone", false);

        mConfirmDelete = storage.getBoolean("confirmDelete", false);
        mConfirmDiscardMessage = storage.getBoolean("confirmDiscardMessage", true);
        mConfirmDeleteStarred = storage.getBoolean("confirmDeleteStarred", false);
        mConfirmSpam = storage.getBoolean("confirmSpam", false);
        mConfirmDeleteFromNotification = storage.getBoolean("confirmDeleteFromNotification", true);
        mConfirmMarkAllRead = storage.getBoolean("confirmMarkAllRead", true);

        try {
            String value = storage.getString("sortTypeEnum", Account.DEFAULT_SORT_TYPE.name());
            mSortType = SortType.valueOf(value);
        } catch (Exception e) {
            mSortType = Account.DEFAULT_SORT_TYPE;
        }

        boolean sortAscending = storage.getBoolean("sortAscending", Account.DEFAULT_SORT_ASCENDING);
        mSortAscending.put(mSortType, sortAscending);

        String notificationHideSubject = storage.getString("notificationHideSubject", null);
        if (notificationHideSubject == null) {
            // If the "notificationHideSubject" setting couldn't be found, the app was probably
            // updated. Look for the old "keyguardPrivacy" setting and map it to the new enum.
            sNotificationHideSubject = (storage.getBoolean("keyguardPrivacy", false)) ?
                    NotificationHideSubject.WHEN_LOCKED : NotificationHideSubject.NEVER;
        } else {
            sNotificationHideSubject = NotificationHideSubject.valueOf(notificationHideSubject);
        }

        String notificationQuickDelete = storage.getString("notificationQuickDelete", null);
        if (notificationQuickDelete != null) {
            sNotificationQuickDelete = NotificationQuickDelete.valueOf(notificationQuickDelete);
        }

        String lockScreenNotificationVisibility = storage.getString("lockScreenNotificationVisibility", null);
        if(lockScreenNotificationVisibility != null) {
            sLockScreenNotificationVisibility = LockScreenNotificationVisibility.valueOf(lockScreenNotificationVisibility);
        }

        String splitViewMode = storage.getString("splitViewMode", null);
        if (splitViewMode != null) {
            sSplitViewMode = SplitViewMode.valueOf(splitViewMode);
        }
        pEpExtraAccounts = storage.getString("pEpExtraAccounts", null);
        pEpUseKeyserver = storage.getBoolean("pEpUseKeyserver", false);
        pEpPassiveMode = storage.getBoolean("pEpPassiveMode", false);
        pEpSubjectUnprotected = storage.getBoolean("pEpSubjectUnprotected", false);
        pEpForwardWarningEnabled = storage.getBoolean("pEpForwardWarningEnabled", false);

        mAttachmentDefaultPath = storage.getString("attachmentdefaultpath",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        sUseBackgroundAsUnreadIndicator = storage.getBoolean("useBackgroundAsUnreadIndicator", true);
        sThreadedViewEnabled = storage.getBoolean("threadedView", true);
        fontSizes.load(storage);

        try {
            setBackgroundOps(BACKGROUND_OPS.valueOf(storage.getString(
                    "backgroundOperations",
                    BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC.name())));
        } catch (Exception e) {
            setBackgroundOps(BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC);
        }

        sColorizeMissingContactPictures = storage.getBoolean("colorizeMissingContactPictures", DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE);

        sMessageViewArchiveActionVisible = storage.getBoolean("messageViewArchiveActionVisible", false);
        sMessageViewDeleteActionVisible = storage.getBoolean("messageViewDeleteActionVisible", true);
        sMessageViewMoveActionVisible = storage.getBoolean("messageViewMoveActionVisible", false);
        sMessageViewCopyActionVisible = storage.getBoolean("messageViewCopyActionVisible", false);
        sMessageViewSpamActionVisible = storage.getBoolean("messageViewSpamActionVisible", false);

        sPgpInlineDialogCounter = storage.getInt("pgpInlineDialogCounter", 0);
        sPgpSignOnlyDialogCounter = storage.getInt("pgpSignOnlyDialogCounter", 0);

        K9.setK9Language(storage.getString("language", ""));

        int themeValue = storage.getInt("theme", Theme.LIGHT.ordinal());
        // We used to save the resource ID of the theme. So convert that to the new format if
        // necessary.
        if (themeValue == Theme.DARK.ordinal() || themeValue == android.R.style.Theme) {
            K9.setK9Theme(Theme.DARK);
        } else {
            K9.setK9Theme(Theme.LIGHT);
        }

        themeValue = storage.getInt("messageViewTheme", Theme.USE_GLOBAL.ordinal());
        K9.setK9MessageViewThemeSetting(Theme.values()[themeValue]);
        themeValue = storage.getInt("messageComposeTheme", Theme.USE_GLOBAL.ordinal());
        K9.setK9ComposerThemeSetting(Theme.values()[themeValue]);
        K9.setUseFixedMessageViewTheme(storage.getBoolean("fixedMessageViewTheme", true));
    }

    /**
     * since Android invokes Application.onCreate() only after invoking all
     * other components' onCreate(), here is a way to notify interested
     * component that the application is available and ready
     */
    protected void notifyObservers() {
        synchronized (observers) {
            for (final ApplicationAware aware : observers) {
                if (K9.DEBUG) {
                    Log.v(K9.LOG_TAG, "Initializing observer: " + aware);
                }
                try {
                    aware.initializeComponent(this);
                } catch (Exception e) {
                    Log.w(K9.LOG_TAG, "Failure when notifying " + aware, e);
                }
            }

            sInitialized = true;
            observers.clear();
        }
    }

    /**
     * Register a component to be notified when the {@link K9} instance is ready.
     *
     * @param component
     *            Never <code>null</code>.
     */
    public static void registerApplicationAware(final ApplicationAware component) {
        synchronized (observers) {
            if (sInitialized) {
                component.initializeComponent(K9.app);
            } else if (!observers.contains(component)) {
                observers.add(component);
            }
        }
    }

    public static String getK9Language() {
        return language;
    }

    public static void setK9Language(String nlanguage) {
        language = nlanguage;
    }

    /**
     * Possible values for the different theme settings.
     *
     * <p><strong>Important:</strong>
     * Do not change the order of the items! The ordinal value (position) is used when saving the
     * settings.</p>
     */
    public enum Theme {
        LIGHT,
        DARK,
        USE_GLOBAL
    }

    public static int getK9ThemeResourceId(Theme themeId) {
        return (themeId == Theme.LIGHT) ? R.style.Theme_K9_Light_NoActionBar : R.style.Theme_K9_Dark;
    }

    public static int getK9ThemeResourceId() {
        return getK9ThemeResourceId(theme);
    }

    public static Theme getK9MessageViewTheme() {
        return messageViewTheme == Theme.USE_GLOBAL ? theme : messageViewTheme;
    }

    public static Theme getK9MessageViewThemeSetting() {
        return messageViewTheme;
    }

    public static Theme getK9ComposerTheme() {
        return composerTheme == Theme.USE_GLOBAL ? theme : composerTheme;
    }

    public static Theme getK9ComposerThemeSetting() {
        return composerTheme;
    }

    public static Theme getK9Theme() {
        return theme;
    }

    public static void setK9Theme(Theme ntheme) {
        if (ntheme != Theme.USE_GLOBAL) {
            theme = ntheme;
        }
    }

    public static void setK9MessageViewThemeSetting(Theme nMessageViewTheme) {
        messageViewTheme = nMessageViewTheme;
    }

    public static boolean useFixedMessageViewTheme() {
        return useFixedMessageTheme;
    }

    public static void setK9ComposerThemeSetting(Theme compTheme) {
        composerTheme = compTheme;
    }

    public static void setUseFixedMessageViewTheme(boolean useFixed) {
        useFixedMessageTheme = useFixed;
        if (!useFixedMessageTheme && messageViewTheme == Theme.USE_GLOBAL) {
            messageViewTheme = theme;
        }
    }

    public static BACKGROUND_OPS getBackgroundOps() {
        return backgroundOps;
    }

    public static boolean setBackgroundOps(BACKGROUND_OPS backgroundOps) {
        BACKGROUND_OPS oldBackgroundOps = K9.backgroundOps;
        K9.backgroundOps = backgroundOps;
        return backgroundOps != oldBackgroundOps;
    }

    public static boolean setBackgroundOps(String nbackgroundOps) {
        return setBackgroundOps(BACKGROUND_OPS.valueOf(nbackgroundOps));
    }

    public static boolean gesturesEnabled() {
        return mGesturesEnabled;
    }

    public static void setGesturesEnabled(boolean gestures) {
        mGesturesEnabled = gestures;
    }

    public static boolean useVolumeKeysForNavigationEnabled() {
        return mUseVolumeKeysForNavigation;
    }

    public static void setUseVolumeKeysForNavigation(boolean volume) {
        mUseVolumeKeysForNavigation = volume;
    }

    public static boolean useVolumeKeysForListNavigationEnabled() {
        return mUseVolumeKeysForListNavigation;
    }

    public static void setUseVolumeKeysForListNavigation(boolean enabled) {
        mUseVolumeKeysForListNavigation = enabled;
    }

    public static boolean autofitWidth() {
        return mAutofitWidth;
    }

    public static void setAutofitWidth(boolean autofitWidth) {
        mAutofitWidth = autofitWidth;
    }

    public static boolean getQuietTimeEnabled() {
        return mQuietTimeEnabled;
    }

    public static void setQuietTimeEnabled(boolean quietTimeEnabled) {
        mQuietTimeEnabled = quietTimeEnabled;
    }

    public static boolean isNotificationDuringQuietTimeEnabled() {
        return mNotificationDuringQuietTimeEnabled;
    }

    public static void setNotificationDuringQuietTimeEnabled(boolean notificationDuringQuietTimeEnabled) {
        mNotificationDuringQuietTimeEnabled = notificationDuringQuietTimeEnabled;
    }

    public static String getQuietTimeStarts() {
        return mQuietTimeStarts;
    }

    public static void setQuietTimeStarts(String quietTimeStarts) {
        mQuietTimeStarts = quietTimeStarts;
    }

    public static String getQuietTimeEnds() {
        return mQuietTimeEnds;
    }

    public static void setQuietTimeEnds(String quietTimeEnds) {
        mQuietTimeEnds = quietTimeEnds;
    }


    public static boolean isQuietTime() {
        if (!mQuietTimeEnabled) {
            return false;
        }

        Time time = new Time();
        time.setToNow();
        Integer startHour = Integer.parseInt(mQuietTimeStarts.split(":")[0]);
        Integer startMinute = Integer.parseInt(mQuietTimeStarts.split(":")[1]);
        Integer endHour = Integer.parseInt(mQuietTimeEnds.split(":")[0]);
        Integer endMinute = Integer.parseInt(mQuietTimeEnds.split(":")[1]);

        Integer now = (time.hour * 60) + time.minute;
        Integer quietStarts = startHour * 60 + startMinute;
        Integer quietEnds =  endHour * 60 + endMinute;

        // If start and end times are the same, we're never quiet
        if (quietStarts.equals(quietEnds)) {
            return false;
        }


        // 21:00 - 05:00 means we want to be quiet if it's after 9 or before 5
        if (quietStarts > quietEnds) {
            // if it's 22:00 or 03:00 but not 8:00
            if (now >= quietStarts || now <= quietEnds) {
                return true;
            }
        }

        // 01:00 - 05:00
        else {

            // if it' 2:00 or 4:00 but not 8:00 or 0:00
            if (now >= quietStarts && now <= quietEnds) {
                return true;
            }
        }

        return false;
    }



    public static boolean startIntegratedInbox() {
        return mStartIntegratedInbox;
    }

    public static void setStartIntegratedInbox(boolean startIntegratedInbox) {
        mStartIntegratedInbox = startIntegratedInbox;
    }

    public static boolean showAnimations() {
        return mAnimations;
    }

    public static void setAnimations(boolean animations) {
        mAnimations = animations;
    }

    public static int messageListPreviewLines() {
        return mMessageListPreviewLines;
    }

    public static void setMessageListPreviewLines(int lines) {
        mMessageListPreviewLines = lines;
    }

    public static boolean messageListCheckboxes() {
        return mMessageListCheckboxes;
    }

    public static void setMessageListCheckboxes(boolean checkboxes) {
        mMessageListCheckboxes = checkboxes;
    }

    public static boolean messageListStars() {
        return mMessageListStars;
    }

    public static void setMessageListStars(boolean stars) {
        mMessageListStars = stars;
    }

    public static boolean showCorrespondentNames() {
        return mShowCorrespondentNames;
    }

     public static boolean messageListSenderAboveSubject() {
         return mMessageListSenderAboveSubject;
     }

    public static void setMessageListSenderAboveSubject(boolean sender) {
         mMessageListSenderAboveSubject = sender;
    }
    public static void setShowCorrespondentNames(boolean showCorrespondentNames) {
        mShowCorrespondentNames = showCorrespondentNames;
    }

    public static boolean showContactName() {
        return mShowContactName;
    }

    public static void setShowContactName(boolean showContactName) {
        mShowContactName = showContactName;
    }

    public static boolean changeContactNameColor() {
        return mChangeContactNameColor;
    }

    public static void setChangeContactNameColor(boolean changeContactNameColor) {
        mChangeContactNameColor = changeContactNameColor;
    }

    public static int getContactNameColor() {
        return mContactNameColor;
    }

    public static void setContactNameColor(int contactNameColor) {
        mContactNameColor = contactNameColor;
    }

    public static boolean messageViewFixedWidthFont() {
        return mMessageViewFixedWidthFont;
    }

    public static void setMessageViewFixedWidthFont(boolean fixed) {
        mMessageViewFixedWidthFont = fixed;
    }

    public static boolean messageViewReturnToList() {
        return mMessageViewReturnToList;
    }

    public static void setMessageViewReturnToList(boolean messageViewReturnToList) {
        mMessageViewReturnToList = messageViewReturnToList;
    }

    public static boolean messageViewShowNext() {
        return mMessageViewShowNext;
    }

    public static void setMessageViewShowNext(boolean messageViewShowNext) {
        mMessageViewShowNext = messageViewShowNext;
    }

    public static FontSizes getFontSizes() {
        return fontSizes;
    }

    public static boolean measureAccounts() {
        return mMeasureAccounts;
    }

    public static void setMeasureAccounts(boolean measureAccounts) {
        mMeasureAccounts = measureAccounts;
    }

    public static boolean countSearchMessages() {
        return mCountSearchMessages;
    }

    public static void setCountSearchMessages(boolean countSearchMessages) {
        mCountSearchMessages = countSearchMessages;
    }

    public static boolean isHideSpecialAccounts() {
        return mHideSpecialAccounts;
    }

    public static void setHideSpecialAccounts(boolean hideSpecialAccounts) {
        mHideSpecialAccounts = hideSpecialAccounts;
    }

    public static boolean confirmDelete() {
        return mConfirmDelete;
    }

    public static void setConfirmDelete(final boolean confirm) {
        mConfirmDelete = confirm;
    }

    public static boolean confirmDeleteStarred() {
        return mConfirmDeleteStarred;
    }

    public static void setConfirmDeleteStarred(final boolean confirm) {
        mConfirmDeleteStarred = confirm;
    }

    public static boolean confirmSpam() {
        return mConfirmSpam;
    }

    public static boolean confirmDiscardMessage() {
        return mConfirmDiscardMessage;
    }

    public static void setConfirmSpam(final boolean confirm) {
        mConfirmSpam = confirm;
    }

    public static void setConfirmDiscardMessage(final boolean confirm) {
        mConfirmDiscardMessage = confirm;
    }

    public static boolean confirmDeleteFromNotification() {
        return mConfirmDeleteFromNotification;
    }

    public static void setConfirmDeleteFromNotification(final boolean confirm) {
        mConfirmDeleteFromNotification = confirm;
    }

    public static boolean confirmMarkAllRead() {
        return mConfirmMarkAllRead;
    }

    public static void setConfirmMarkAllRead(final boolean confirm) {
        mConfirmMarkAllRead = confirm;
    }

    public static NotificationHideSubject getNotificationHideSubject() {
        return sNotificationHideSubject;
    }

    public static void setNotificationHideSubject(final NotificationHideSubject mode) {
        sNotificationHideSubject = mode;
    }

    public static NotificationQuickDelete getNotificationQuickDeleteBehaviour() {
        return sNotificationQuickDelete;
    }

    public static void setNotificationQuickDeleteBehaviour(final NotificationQuickDelete mode) {
        sNotificationQuickDelete = mode;
    }

    public static LockScreenNotificationVisibility getLockScreenNotificationVisibility() {
        return sLockScreenNotificationVisibility;
    }

    public static void setLockScreenNotificationVisibility(final LockScreenNotificationVisibility visibility) {
        sLockScreenNotificationVisibility = visibility;
    }

    public static boolean wrapFolderNames() {
        return mWrapFolderNames;
    }
    public static void setWrapFolderNames(final boolean state) {
        mWrapFolderNames = state;
    }

    public static boolean hideUserAgent() {
        return mHideUserAgent;
    }
    public static void setHideUserAgent(final boolean state) {
        mHideUserAgent = state;
    }

    public static boolean hideTimeZone() {
        return mHideTimeZone;
    }
    public static void setHideTimeZone(final boolean state) {
        mHideTimeZone = state;
    }

    public static String getAttachmentDefaultPath() {
        return mAttachmentDefaultPath;
    }

    public static void setAttachmentDefaultPath(String attachmentDefaultPath) {
        K9.mAttachmentDefaultPath = attachmentDefaultPath;
    }

    public static synchronized SortType getSortType() {
        return mSortType;
    }

    public static synchronized void setSortType(SortType sortType) {
        mSortType = sortType;
    }

    public static synchronized boolean isSortAscending(SortType sortType) {
        if (mSortAscending.get(sortType) == null) {
            mSortAscending.put(sortType, sortType.isDefaultAscending());
        }
        return mSortAscending.get(sortType);
    }

    public static synchronized void setSortAscending(SortType sortType, boolean sortAscending) {
        mSortAscending.put(sortType, sortAscending);
    }

    public static synchronized boolean useBackgroundAsUnreadIndicator() {
        return sUseBackgroundAsUnreadIndicator;
    }

    public static synchronized void setUseBackgroundAsUnreadIndicator(boolean enabled) {
        sUseBackgroundAsUnreadIndicator = enabled;
    }

    public static synchronized boolean isThreadedViewEnabled() {
        return sThreadedViewEnabled;
    }

    public static synchronized void setThreadedViewEnabled(boolean enable) {
        sThreadedViewEnabled = enable;
    }

    public static synchronized SplitViewMode getSplitViewMode() {
        return sSplitViewMode;
    }

    public static synchronized void setSplitViewMode(SplitViewMode mode) {
        sSplitViewMode = mode;
    }

    public static boolean showContactPicture() {
        return sShowContactPicture;
    }

    public static void setShowContactPicture(boolean show) {
        sShowContactPicture = show;
    }

    public static boolean isColorizeMissingContactPictures() {
        return sColorizeMissingContactPictures;
    }

    public static void setColorizeMissingContactPictures(boolean enabled) {
        sColorizeMissingContactPictures = enabled;
    }

    public static boolean isMessageViewArchiveActionVisible() {
        return sMessageViewArchiveActionVisible;
    }

    public static void setMessageViewArchiveActionVisible(boolean visible) {
        sMessageViewArchiveActionVisible = visible;
    }

    public static boolean isMessageViewDeleteActionVisible() {
        return sMessageViewDeleteActionVisible;
    }

    public static void setMessageViewDeleteActionVisible(boolean visible) {
        sMessageViewDeleteActionVisible = visible;
    }

    public static boolean isMessageViewMoveActionVisible() {
        return sMessageViewMoveActionVisible;
    }

    public static void setMessageViewMoveActionVisible(boolean visible) {
        sMessageViewMoveActionVisible = visible;
    }

    public static boolean isMessageViewCopyActionVisible() {
        return sMessageViewCopyActionVisible;
    }

    public static void setMessageViewCopyActionVisible(boolean visible) {
        sMessageViewCopyActionVisible = visible;
    }

    public static boolean isMessageViewSpamActionVisible() {
        return sMessageViewSpamActionVisible;
    }

    public static void setMessageViewSpamActionVisible(boolean visible) {
        sMessageViewSpamActionVisible = visible;
    }

    public static int getPgpInlineDialogCounter() {
        return sPgpInlineDialogCounter;
    }

    public static void setPgpInlineDialogCounter(int pgpInlineDialogCounter) {
        K9.sPgpInlineDialogCounter = pgpInlineDialogCounter;
    }

    public static int getPgpSignOnlyDialogCounter() {
        return sPgpSignOnlyDialogCounter;
    }

    public static void setPgpSignOnlyDialogCounter(int pgpSignOnlyDialogCounter) {
        K9.sPgpSignOnlyDialogCounter = pgpSignOnlyDialogCounter;
    }

    /**
     * Check if we already know whether all databases are using the current database schema.
     *
     * <p>
     * This method is only used for optimizations. If it returns {@code true} we can be certain that
     * getting a {@link LocalStore} instance won't trigger a schema upgrade.
     * </p>
     *
     * @return {@code true}, if we know that all databases are using the current database schema.
     *         {@code false}, otherwise.
     */
    public static synchronized boolean areDatabasesUpToDate() {
        return sDatabasesUpToDate;
    }



    /**
     * Remember that all account databases are using the most recent database schema.
     *
     * @param save
     *         Whether or not to write the current database version to the
     *         {@code SharedPreferences} {@link #DATABASE_VERSION_CACHE}.

     * @see #areDatabasesUpToDate()
     */
    public static synchronized void setDatabasesUpToDate(boolean save) {
        sDatabasesUpToDate = true;

        if (save) {
            Editor editor = sDatabaseVersionCache.edit();
            editor.putInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, LocalStore.DB_VERSION);
            editor.commit();
        }
    }

    private ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        int activityCount = 0;
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            if (activityCount == 0) {
//                if (activity instanceof K9Activity) pEpSyncProvider.setSyncHandshakeCallback((Sync.showHandshakeCallback) activity);
                pEpProvider = PEpProviderFactory.createAndSetupProvider(getApplicationContext());
            }
            ++activityCount;
        }

        @Override
        public void onActivityStarted(Activity activity) {

        }

        @Override
        public void onActivityResumed(Activity activity) {

        }

        @Override
        public void onActivityPaused(Activity activity) {

        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            --activityCount;
            if (activityCount == 0) {
                pEpProvider.close();
                pEpProvider = null;
            }
        }
    };

    public PEpProvider getpEpProvider() { return pEpProvider;}
    public static void setPEpExtraAccounts(String text) {
        pEpExtraAccounts = text;
    }

    public static String getPEpExtraAccounts() {
        return pEpExtraAccounts;
    }

    public void setPEpUseKeyserver(boolean use) {
        pEpUseKeyserver = use;
        if (use) {
            pEpProvider.startKeyserverLookup();
        } else{
            pEpProvider.stopKeyserverLookup();
        }

    }

    public static boolean getPEpUseKeyserver() {
        return pEpUseKeyserver;
    }


    public static boolean getPEpPassiveMode() {
        return pEpPassiveMode;
    }

    public void setPEpPassiveMode(boolean enabled) {
        K9.pEpPassiveMode = enabled;
        pEpProvider.setPassiveModeEnabled(enabled);
        MessagingController.getInstance(this).setPassiveModeEnabled(enabled);

    }

    public static boolean ispEpSubjectUnprotected() {
        return pEpSubjectUnprotected;
    }

    public void setpEpSubjectUnprotected(boolean pEpSubjectUnprotected) {
        K9.pEpSubjectUnprotected = pEpSubjectUnprotected;
        pEpProvider.setSubjectUnprotected(pEpSubjectUnprotected);
        MessagingController.getInstance(this).setSubjectUnprotected(pEpSubjectUnprotected);
    }


    public static boolean ispEpForwardWarningEnabled() {
        return pEpForwardWarningEnabled;
    }
    public void setpEpForwardWarningEnabled(boolean pEpForwardWarningEnabled) {
        K9.pEpForwardWarningEnabled = pEpForwardWarningEnabled;
    }

    private void initializeInjector() {
        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    public ApplicationComponent getComponent() {
        return component;
    }

    private void setupFastPoller() {
        if (poller == null) {
            poller = new Poller(new Handler());
            poller.init(POLLING_INTERVAL, this::polling);
        } else {
            poller.stopPolling();
        }
        poller.startPolling();
    }

    private void polling() {
        if (needsFastPoll && !isPollingMessages) {
            isPollingMessages = true;
            MessagingController messagingController = MessagingController.getInstance(this);
            messagingController.checkMail(K9.this, true, true, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {
                    isPollingMessages = false;
                }

                @Override
                public void onError(Throwable throwable) {
                    isPollingMessages = false;
                }
            });
        }
    }

    public boolean ispEpSyncEnabled() {
        return ispEpSyncEnabled;
    }

    public void setIspEpSyncEnabled(boolean ispEpSyncEnabled) {
        this.ispEpSyncEnabled = ispEpSyncEnabled;
    }
}
