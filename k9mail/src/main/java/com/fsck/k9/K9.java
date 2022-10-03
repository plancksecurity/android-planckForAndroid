package com.fsck.k9;


import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import androidx.multidex.MultiDexApplication;
import androidx.work.WorkManager;

import com.fsck.k9.Account.SortType;
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.UpgradeDatabases;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.helper.AppUpdater;
import com.fsck.k9.job.K9JobManager;
import com.fsck.k9.job.MailSyncWorkerManager;
import com.fsck.k9.job.PusherRefreshWorkerManager;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.ssl.LocalKeyStore;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.pEp.LangUtils;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.infrastructure.Poller;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerApplicationComponent;
import com.fsck.k9.pEp.manualsync.ImportWizardFrompEp;

import security.pEp.mdm.ManageableSetting;
import security.pEp.mdm.ManageableSettingKt;
import security.pEp.mdm.MediaKey;
import security.pEp.network.ConnectionMonitor;
import com.fsck.k9.pEp.ui.activities.SplashScreen;
import com.fsck.k9.pEp.ui.tools.AppTheme;
import com.fsck.k9.pEp.ui.tools.Theme;
import com.fsck.k9.pEp.ui.tools.ThemeManager;
import com.fsck.k9.power.DeviceIdleManager;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.provider.UnreadWidgetProvider;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailServiceLegacy;
import com.fsck.k9.service.ShutdownReceiver;
import com.fsck.k9.service.StorageGoneReceiver;
import com.fsck.k9.widget.list.MessageListWidgetProvider;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import foundation.pEp.jniadapter.AndroidHelper;
import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Sync;
import foundation.pEp.jniadapter.SyncHandshakeSignal;
import security.pEp.sync.KeySyncCleaner;
import security.pEp.ui.passphrase.PassphraseActivity;
import security.pEp.ui.passphrase.PassphraseRequirementType;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

@ReportsCrashes(mailTo = "crashreport@pep.security",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class K9 extends MultiDexApplication {
    public static final int POLLING_INTERVAL = 2000;
    private Poller poller;
    private boolean needsFastPoll = false;
    private boolean isPollingMessages;
    private boolean showingKeyimportDialog = false;
    public static final boolean DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE = false;
    public PEpProvider pEpProvider, pEpSyncProvider;
    private Account currentAccount;
    private ApplicationComponent component;
    private ConnectionMonitor connectivityMonitor = new ConnectionMonitor();
    private boolean pEpSyncEnvironmentInitialized;
    private static boolean allowpEpSyncNewDevices = !BuildConfig.IS_ENTERPRISE;
    private static boolean enableEchoProtocol = true;
    private static Set<MediaKey> mediaKeys;

    public static K9JobManager jobManager;

    public static Set<String> getMasterKeys() {
        return pEpExtraKeys;
    }

    public static void setMasterKeys(Set<String> keys) {
        pEpExtraKeys = keys;
    }

    public boolean isBatteryOptimizationAsked() {
        return batteryOptimizationAsked;
    }

    private boolean batteryOptimizationAsked;

    public void batteryOptimizationAsked() {
        batteryOptimizationAsked = true;
    }

    public void enableFastPolling() {
        needsFastPoll = true;
    }

    public void disableFastPolling() {
        needsFastPoll = false;
    }


    public static final int VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS = 63;
    public static final int DEFAULT_CONTACT_NAME_COLOR = 0xff00008f;

    public static String password = null;


    /**
     * Components that are interested in knowing when the K9 instance is
     * available and ready (Android invokes Application.onCreate() after other
     * components') should implement this interface and register using
     * {@link K9#registerApplicationAware(ApplicationAware)}.
     */
    public interface ApplicationAware {
        /**
         * Called when the Application instance is available and ready.
         *
         * @param application The application instance. Never <code>null</code>.
         * @throws Exception
         */
        void initializeComponent(Application application);
    }

    public static Application app = null;
    public static File tempDirectory;
    public static final String LOG_TAG = "k9pEp";

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
     * immediately call {@link com.fsck.k9.K9.ApplicationAware#initializeComponent(Application)} for the
     * supplied argument.
     */
    private static boolean sInitialized = false;

    public enum BACKGROUND_OPS {
        ALWAYS, NEVER, WHEN_CHECKED_AUTO_SYNC
    }

    private static String language = "";

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

    private static boolean mMessageListCheckboxes = false;
    private static boolean mMessageListStars = true;
    private static int mMessageListPreviewLines = 2;

    private static boolean mShowCorrespondentNames = true;
    private static boolean mMessageListSenderAboveSubject = BuildConfig.IS_ENTERPRISE;
    private static boolean mShowContactName = BuildConfig.IS_ENTERPRISE;
    private static boolean mChangeContactNameColor = false;
    private static int mContactNameColor = DEFAULT_CONTACT_NAME_COLOR;
    private static boolean sShowContactPicture = true;
    private static boolean mMessageViewFixedWidthFont = false;
    private static boolean mMessageViewReturnToList = false;
    private static boolean mMessageViewShowNext = true;

    private static boolean mGesturesEnabled = true;
    private static boolean mUseVolumeKeysForNavigation = false;
    private static boolean mUseVolumeKeysForListNavigation = false;
    private static boolean mStartIntegratedInbox = false;
    private static boolean mMeasureAccounts = false;
    private static boolean mCountSearchMessages = true;
    private static boolean mAutofitWidth = false;
    private static boolean mQuietTimeEnabled = false;
    private static boolean mNotificationDuringQuietTimeEnabled = true;
    private static String mQuietTimeStarts = "21:00";
    private static String mQuietTimeEnds = "7:00";
    private static String mAttachmentDefaultPath = "";
    private static boolean mWrapFolderNames = false;
    private static boolean mHideUserAgent = true;
    private static boolean mHideTimeZone = false;

    private static SortType mSortType = Account.DEFAULT_SORT_TYPE;
    private static Map<SortType, Boolean> mSortAscending = new HashMap<SortType, Boolean>();

    private static boolean sUseBackgroundAsUnreadIndicator = false;
    private static boolean sThreadedViewEnabled = true;
    private static SplitViewMode sSplitViewMode = SplitViewMode.NEVER;
    private static boolean sColorizeMissingContactPictures = DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE;

    private static boolean sMessageViewArchiveActionVisible = false;
    private static boolean sMessageViewDeleteActionVisible = true;
    private static boolean sMessageViewMoveActionVisible = false;
    private static boolean sMessageViewCopyActionVisible = false;
    private static boolean sMessageViewSpamActionVisible = false;
    private static String pEpExtraAccounts = "";
    //private static boolean pEpUseKeyserver = false;
    private static boolean pEpPassiveMode = false;
    private static boolean pEpSubjectProtection = true;
    private static boolean pEpForwardWarningEnabled =
            BuildConfig.IS_ENTERPRISE;
    private static boolean pEpSyncEnabled = true;
    private static boolean shallRequestPermissions = true;
    private static boolean usingpEpSyncFolder = true;
    private static boolean pEpUsePassphraseForNewKeys = false;
    private static long appVersionCode = -1;
    private boolean grouped = false;
    private static Set<String> pEpExtraKeys = Collections.emptySet();


    private static int sPgpInlineDialogCounter;
    private static int sPgpSignOnlyDialogCounter;

    private static String pEpNewKeysPassphrase;
    private static ManageableSetting<Boolean> pEpUseTrustwords =
            new ManageableSetting<>(!BuildConfig.IS_ENTERPRISE, true);

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
    public static final int DEFAULT_VISIBLE_LIMIT = BuildConfig.IS_ENTERPRISE ? 250 : 100;

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
        Context appContext = context.getApplicationContext();
        int acctLength = Preferences.getPreferences(appContext).getAvailableAccounts().size();
        boolean enable = acctLength > 0;

        setServicesEnabled(appContext, enable);

        //updateDeviceIdleReceiver(appContext, enable);
    }

    private static void updateDeviceIdleReceiver(Context context, boolean enable) {
        DeviceIdleManager deviceIdleManager = DeviceIdleManager.getInstance(context);
        if (enable) {
            deviceIdleManager.registerReceiver();
        } else {
            deviceIdleManager.unregisterReceiver();
        }
    }

    private static void setServicesEnabled(Context context, boolean enabled) {

        PackageManager pm = context.getPackageManager();

//        if (!enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
//            /*
//             * If no accounts now exist but the service is still enabled we're about to disable it
//             * so we'll reschedule to kill off any existing alarms.
//             */
//            MailService.actionReset(context, wakeLockId);
//        }

        Class<?>[] classes = {MessageCompose.class, BootReceiver.class, MailServiceLegacy.class};

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

//        if (enabled && pm.getComponentEnabledSetting(new ComponentName(context, MailService.class)) ==
//                PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
//            /*
//             * And now if accounts do exist then we've just enabled the service and we want to
//             * schedule alarms for the new accounts.
//             */
//            MailService.actionReset(context, wakeLockId);
//        }

        if (enabled) {
            jobManager.scheduleAllMailJobs();
        }
    }

    /**
     * Register BroadcastReceivers programmatically because doing it from manifest
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
                    Timber.e(e);
                }
                Looper.loop();
            }

        }, "Unmount-thread").start();

        try {
            final Handler storageGoneHandler = queue.take();
            registerReceiver(receiver, filter, null, storageGoneHandler);
            Timber.i("Registered: unmount receiver");
        } catch (InterruptedException e) {
            Timber.e(e, "Unable to register unmount receiver");
        }

        registerReceiver(new ShutdownReceiver(), new IntentFilter(Intent.ACTION_SHUTDOWN));
        Timber.i("Registered: shutdown receiver");
    }

    public static void save(StorageEditor editor) {
        editor.putBoolean("enableDebugLogging", K9.isDebug());
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
        //editor.putBoolean("hideHostnameWhenConnecting", hideHostnameWhenConnecting);


        editor.putString("language", language);
        editor.putInt("theme", ThemeManager.getAppTheme().ordinal());
        editor.putInt("messageViewTheme", ThemeManager.getK9MessageViewTheme().ordinal());
        editor.putInt("messageComposeTheme", ThemeManager.getK9ComposerTheme().ordinal());
        editor.putBoolean("fixedMessageViewTheme", ThemeManager.getUseFixedMessageViewTheme());

        editor.putBoolean("confirmDelete", mConfirmDelete);
        editor.putBoolean("confirmDiscardMessage", mConfirmDiscardMessage);
        editor.putBoolean("confirmDeleteStarred", mConfirmDeleteStarred);
        editor.putBoolean("confirmSpam", mConfirmSpam);
        editor.putBoolean("confirmDeleteFromNotification", mConfirmDeleteFromNotification);
        editor.putBoolean("confirmMarkAllRead", mConfirmMarkAllRead);

        editor.putString("sortTypeEnum", mSortType.name());
        editor.putBoolean(
                "sortAscending",
                mSortAscending.containsKey(mSortType)
                        ? mSortAscending.get(mSortType)
                        : mSortType.isDefaultAscending()
        );

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
        //editor.putBoolean("pEpUseKeyserver", pEpUseKeyserver);
        editor.putBoolean("pEpPassiveMode", pEpPassiveMode);
        editor.putBoolean("pEpSubjectProtection", pEpSubjectProtection);
        editor.putBoolean("pEpForwardWarningEnabled", pEpForwardWarningEnabled);
        editor.putBoolean("pEpEnableSync", pEpSyncEnabled);
        editor.putBoolean("shallRequestPermissions", shallRequestPermissions);

        editor.putBoolean("pEpSyncFolder", usingpEpSyncFolder);
        editor.putLong("appVersionCode", appVersionCode);
        editor.putBoolean("pEpUsePassphraseForNewKeys", pEpUsePassphraseForNewKeys);
        editor.putPassphrase(pEpNewKeysPassphrase);
        editor.putString(
                "pEpUseTrustwords",
                ManageableSettingKt.encodeBooleanToString(pEpUseTrustwords)
        );
        editor.putBoolean("allowpEpSyncNewDevices", allowpEpSyncNewDevices);
        editor.putBoolean("enableEchoProtocol", enableEchoProtocol);
        editor.putString("mediaKeys", serializeMediaKeys());

        fontSizes.save(editor);
    }

    @Override
    public void onCreate() {
        AndroidHelper.setup(this);

        if (K9.DEVELOPER_MODE) {
            StrictMode.enableDefaults();
        }

        super.onCreate();
        app = this;
        Globals.setContext(this);

        initializeInjector();

        ACRA.init(this);
        component.provisioningManager().startProvisioning();
    }

    public void finalizeSetup() {
        pEpSetupUiEngineSession();
        DI.start(this);
        K9MailLib.setDebugStatus(new K9MailLib.DebugStatus() {
            @Override
            public boolean enabled() {
                return DEBUG;
            }

            @Override
            public boolean debugSensitive() {
                return DEBUG_SENSITIVE;
            }
        });

        checkCachedDatabaseVersion();

        Preferences prefs = component.preferences();
        loadPrefs(prefs);

        /*
         * We have to give MimeMessage a temp directory because File.createTempFile(String, String)
         * doesn't work in Android and MimeMessage does not have access to a Context.
         */
        BinaryTempFileBody.setTempDirectory(getCacheDir());
        clearBodyCacheIfAppUpgrade();

        LocalKeyStore.setKeyStoreLocation(
                component.pEpSystemFileLocator().getKeyStoreFolder().toString()
        );

        MessagingController messagingController = MessagingController.getInstance(this);
        // Perform engine provisioning just after its initialization in MessagingController
        component.provisioningManager().performInitializedEngineProvisioning();

        initJobManager(prefs, messagingController);

        /*
         * Enable background sync of messages
         */

        setServicesEnabled(this);
        startConnectivityMonitor();
        registerReceivers();

        MessagingController.getInstance(this).addListener(new SimpleMessagingListener() {
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

                Timber.d("Broadcasted: action=%s account=%s folder=%s message uid=%s",
                        action,
                        account.getDescription(),
                        folder,
                        message.getUid());
            }


            private void updateUnreadWidget() {
                try {
                    UnreadWidgetProvider.updateUnreadCount(K9.this);
                } catch (Exception e) {
                    Timber.e(e, "Error while updating unread widget(s)");
                }
            }

            private void updateMailListWidget() {
                try {
                    MessageListWidgetProvider.triggerMessageListWidgetUpdate(K9.this);
                } catch (RuntimeException e) {
                    if (BuildConfig.DEBUG) {
                        throw e;
                    } else {
                        Timber.e(e, "Error while updating message list widget");
                    }
                }
            }

            @Override
            public void synchronizeMailboxRemovedMessage(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
                updateUnreadWidget();
                updateMailListWidget();
            }

            @Override
            public void messageDeleted(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_DELETED, account, folder, message);
                updateUnreadWidget();
                updateMailListWidget();
            }

            @Override
            public void synchronizeMailboxNewMessage(Account account, String folder, Message message) {
                broadcastIntent(K9.Intents.EmailReceived.ACTION_EMAIL_RECEIVED, account, folder, message);
                updateUnreadWidget();
                updateMailListWidget();
            }

            @Override
            public void folderStatusChanged(Account account, String folderName,
                                            int unreadMessageCount) {

                updateUnreadWidget();
                updateMailListWidget();

                // let observers know a change occurred
                Intent intent = new Intent(K9.Intents.EmailReceived.ACTION_REFRESH_OBSERVER, null);
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_ACCOUNT, account.getDescription());
                intent.putExtra(K9.Intents.EmailReceived.EXTRA_FOLDER, folderName);
                K9.this.sendBroadcast(intent);

            }

        });

        refreshFoldersForAllAccounts();
        //pEpInitSyncEnvironment();
        setupFastPoller();

        notifyObservers();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();
            batteryOptimizationAsked = powerManager.isIgnoringBatteryOptimizations(packageName);
        }
    }

    private void clearBodyCacheIfAppUpgrade() {
        AppUpdater appUpdater = new AppUpdater(this, getCacheDir());
        appUpdater.clearBodyCacheIfAppUpgrade();
    }

    private void refreshFoldersForAllAccounts() {
        List<Account> accounts = Preferences.getPreferences(this.getApplicationContext()).getAccounts();
        for (Account account : accounts) {
            MessagingController.getInstance(this).listFolders(account, true, null);
        }
    }

    private void initJobManager(Preferences prefs, MessagingController messagingController) {
        WorkManager workManager = WorkManager.getInstance(this);
        MailSyncWorkerManager mailSyncWorkerManager = new MailSyncWorkerManager(workManager);
        PusherRefreshWorkerManager pusherRefreshWorkerManager =
                new PusherRefreshWorkerManager(workManager, this, messagingController);

        jobManager = new K9JobManager(
                workManager, prefs, mailSyncWorkerManager, pusherRefreshWorkerManager
        );
    }

    public boolean ispEpSyncEnvironmentInitialized() {
        return pEpSyncEnvironmentInitialized;
    }

    public void pEpInitSyncEnvironment() {
        pEpSyncEnvironmentInitialized = true;
        if (pEpSyncProvider == null) {
            pEpSyncProvider = PEpProviderFactory.createAndSetupProvider(this);
        }
//        for (Account account : prefs.getAccounts()) {
//            pEpSyncProvider.myself(PEpUtils.createIdentity(new Address(account.getEmail(), account.getName()), this));
//        }

        KeySyncCleaner.queueAutoConsumeMessages();

        if (Preferences.getPreferences(this.getApplicationContext()).getAccounts().size() > 0) {
            if (pEpSyncEnabled) {
                initSync();
            }
        } else {
            Log.e("pEpEngine-app", "There is no accounts set up, not trying to start sync");
        }
    }

    public PEpProvider getpEpSyncProvider() {
        if (pEpSyncEnabled) {
            return pEpSyncProvider;
        } else {
            return pEpProvider;
        }
    }

    private void initSync() {

        PEpUtils.updateSyncAccountsConfig(this);
        if (!pEpSyncProvider.isSyncRunning()) {
            pEpSyncProvider.startSync();
        }
//        }
    }

    private void goToAddDevice(Identity myself, Identity partner, SyncHandshakeSignal signal, boolean formingGroup) {
        Timber.i("PEPJNI", "showHandshake: " + signal.name() + " " + myself.toString() + "\n::\n" + partner.toString());

        Context context = K9.this.getApplicationContext();
        Intent syncTrustowordsActivity = ImportWizardFrompEp.createActionStartKeySyncIntent(context, myself, partner, signal, formingGroup);
        syncTrustowordsActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(syncTrustowordsActivity);
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
        if (cachedVersion < VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS) {
            migrateOpenPgpGlobalToAccountSettings();
        }
    }

    private void migrateOpenPgpGlobalToAccountSettings() {
        Preferences preferences = Preferences.getPreferences(this);
        Storage storage = preferences.getStorage();

        String openPgpProvider = storage.getString("openPgpProvider", null);
        boolean openPgpSupportSignOnly = storage.getBoolean("openPgpSupportSignOnly", false);

        for (Account account : preferences.getAccounts()) {
            account.setOpenPgpProvider(openPgpProvider);
            account.setOpenPgpHideSignOnly(!openPgpSupportSignOnly);
            account.save(preferences);
        }

        storage.edit()
                .remove("openPgpProvider")
                .remove("openPgpSupportSignOnly")
                .commit();
    }

    /**
     * Load preferences into our statics.
     * <p>
     * If you're adding a preference here, odds are you'll need to add it to
     * {@link com.fsck.k9.preferences.GlobalSettings}, too.
     *
     * @param prefs Preferences to load
     */
    public static void loadPrefs(Preferences prefs) {
        Storage storage = prefs.getStorage();
        setDebug(storage.getBoolean("enableDebugLogging", BuildConfig.DEVELOPER_MODE));
        DEBUG_SENSITIVE = storage.getBoolean("enableSensitiveLogging", false);
        mAnimations = storage.getBoolean("animations", true);
        mGesturesEnabled = storage.getBoolean("gesturesEnabled", false);
        mUseVolumeKeysForNavigation = storage.getBoolean("useVolumeKeysForNavigation", false);
        mUseVolumeKeysForListNavigation = storage.getBoolean("useVolumeKeysForListNavigation", false);
        mStartIntegratedInbox = storage.getBoolean("startIntegratedInbox", false);
        mMeasureAccounts = storage.getBoolean("measureAccounts", false);
        mCountSearchMessages = storage.getBoolean("countSearchMessages", true);
        mMessageListSenderAboveSubject = storage.getBoolean(
                "messageListSenderAboveSubject",
                BuildConfig.IS_ENTERPRISE
        );
        mMessageListCheckboxes = storage.getBoolean("messageListCheckboxes", false);
        mMessageListStars = storage.getBoolean("messageListStars", true);
        mMessageListPreviewLines = storage.getInt("messageListPreviewLines", 2);

        mAutofitWidth = storage.getBoolean("autofitWidth", false);

        mQuietTimeEnabled = storage.getBoolean("quietTimeEnabled", false);
        mNotificationDuringQuietTimeEnabled = storage.getBoolean("notificationDuringQuietTimeEnabled", true);
        mQuietTimeStarts = storage.getString("quietTimeStarts", "21:00");
        mQuietTimeEnds = storage.getString("quietTimeEnds", "7:00");

        mShowCorrespondentNames = storage.getBoolean("showCorrespondentNames", true);
        mShowContactName = storage.getBoolean("showContactName", BuildConfig.IS_ENTERPRISE);
        sShowContactPicture = storage.getBoolean("showContactPicture", true);
        mChangeContactNameColor = storage.getBoolean("changeRegisteredNameColor", false);
        mContactNameColor = storage.getInt("registeredNameColor", DEFAULT_CONTACT_NAME_COLOR);
        mMessageViewFixedWidthFont = storage.getBoolean("messageViewFixedWidthFont", false);
        boolean returnToList = storage.getBoolean("messageViewReturnToList", false);
        boolean showNext = storage.getBoolean("messageViewShowNext", true);
        setAfterMessageDeleteBehavior(returnToList, showNext);
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
        if (lockScreenNotificationVisibility != null) {
            sLockScreenNotificationVisibility = LockScreenNotificationVisibility.valueOf(lockScreenNotificationVisibility);
        }

        String splitViewMode = storage.getString("splitViewMode", null);
        if (splitViewMode != null) {
            sSplitViewMode = SplitViewMode.valueOf(splitViewMode);
        }
        pEpExtraAccounts = storage.getString("pEpExtraAccounts", null);
        //pEpUseKeyserver = storage.getBoolean("pEpUseKeyserver", false);
        pEpPassiveMode = storage.getBoolean("pEpPassiveMode", false);
        pEpSubjectProtection = getValuePEpSubjectProtection(storage);
        pEpForwardWarningEnabled = storage.getBoolean(
                "pEpForwardWarningEnabled", BuildConfig.IS_ENTERPRISE);
        pEpSyncEnabled = storage.getBoolean("pEpEnableSync", true);
        usingpEpSyncFolder = storage.getBoolean("pEpSyncFolder", pEpSyncEnabled);
        appVersionCode = storage.getLong("appVersionCode", -1);

        mAttachmentDefaultPath = storage.getString("attachmentdefaultpath",
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
        sUseBackgroundAsUnreadIndicator = storage.getBoolean("useBackgroundAsUnreadIndicator", false);
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
        shallRequestPermissions = storage.getBoolean("shallRequestPermissions", true);

        K9.setK9Language(storage.getString("language", ""));

        int themeValue = storage.getInt("theme", AppTheme.FOLLOW_SYSTEM.ordinal());
        ThemeManager.setAppTheme(AppTheme.values()[themeValue]);

        themeValue = storage.getInt("messageViewTheme", Theme.USE_GLOBAL.ordinal());
        ThemeManager.setK9MessageViewTheme(Theme.values()[themeValue]);
        themeValue = storage.getInt("messageComposeTheme", Theme.USE_GLOBAL.ordinal());
        ThemeManager.setK9ComposerTheme(Theme.values()[themeValue]);
        ThemeManager.setUseFixedMessageViewTheme(storage.getBoolean("fixedMessageViewTheme", true));
        pEpUsePassphraseForNewKeys = storage.getBoolean("pEpUsePassphraseForNewKeys", false);
        pEpNewKeysPassphrase = storage.getPassphrase();
        pEpUseTrustwords = ManageableSettingKt.decodeBooleanFromString(
                storage.getString(
                        "pEpUseTrustwords",
                        ManageableSettingKt.encodeBooleanToString(
                                new ManageableSetting<>(
                                        !BuildConfig.IS_ENTERPRISE,
                                        true
                                )
                        )
                )
        );
        allowpEpSyncNewDevices = storage.getBoolean("allowpEpSyncNewDevices", !BuildConfig.IS_ENTERPRISE);
        enableEchoProtocol = storage.getBoolean("enableEchoProtocol", true);
        mediaKeys = parseMediaKeys(storage.getString("mediaKeys", null));
        new Handler(Looper.getMainLooper()).post(ThemeManager::updateAppTheme);
    }

    private static Set<MediaKey> parseMediaKeys(String mediaKeysString) {
        Set<MediaKey> set = null;
        if (mediaKeysString != null) {
            set = new HashSet<>();
            for (String s : mediaKeysString.split(",")) {
                String[] pair = s.split(" : ");
                if (pair.length != 2) {
                    Timber.e("Bad format for saved media keys");
                    return null;
                } else {
                    set.add(new MediaKey(pair[0], pair[1]));
                }
            }
        }
        return set;
    }

    private static String serializeMediaKeys() {
        if (mediaKeys == null) return null;
        StringBuilder sb = new StringBuilder();
        for (MediaKey key : mediaKeys) {
            sb.append(key.getAddressPattern());
            sb.append(" : ");
            sb.append(key.getFpr());
            sb.append(",");
        }
        return sb.length() > 0
                ? sb.substring(0, sb.length() - 1)
                : null;
    }

    private static boolean getValuePEpSubjectProtection(Storage storage) {
        return storage.getBoolean("pEpSubjectProtection", !storage.getBoolean("pEpSubjectUnprotected", false));
    }

    /**
     * Mutually excluyent setter for the behavior after a message is deleted.
     *
     * @param returnToList
     * @param showNext
     */
    private static void setAfterMessageDeleteBehavior(boolean returnToList, boolean showNext) {
        if (!showNext && returnToList) {
            mMessageViewReturnToList = true;
            mMessageViewShowNext = false;
        } else {
            mMessageViewReturnToList = false;
            mMessageViewShowNext = true;
        }
    }

    /**
     * since Android invokes Application.onCreate() only after invoking all
     * other components' onCreate(), here is a way to notify interested
     * component that the application is available and ready
     */
    protected void notifyObservers() {
        synchronized (observers) {
            for (final ApplicationAware aware : observers) {
                Timber.v("Initializing observer: %s", aware);

                try {
                    aware.initializeComponent(this);
                } catch (Exception e) {
                    Timber.w(e, "Failure when notifying %s", aware);
                }
            }

            sInitialized = true;
            observers.clear();
        }
    }

    /**
     * Register a component to be notified when the {@link K9} instance is ready.
     *
     * @param component Never <code>null</code>.
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

    public static String getK9CurrentLanguage() {
        if(language.isEmpty()) {
           return LangUtils.getDefaultLocale().getLanguage();
        }
        else return language;
    }

    public static void setK9Language(String nlanguage) {
        language = nlanguage;
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

        GregorianCalendar gregorianCalendar = new GregorianCalendar();

        Integer startHour = Integer.parseInt(mQuietTimeStarts.split(":")[0]);
        Integer startMinute = Integer.parseInt(mQuietTimeStarts.split(":")[1]);
        Integer endHour = Integer.parseInt(mQuietTimeEnds.split(":")[0]);
        Integer endMinute = Integer.parseInt(mQuietTimeEnds.split(":")[1]);

        Integer now = (gregorianCalendar.get(Calendar.HOUR) * 60) + gregorianCalendar.get(Calendar.MINUTE);
        Integer quietStarts = startHour * 60 + startMinute;
        Integer quietEnds = endHour * 60 + endMinute;

        // If start and end times are the same, we're never quiet
        if (quietStarts.equals(quietEnds)) {
            return false;
        }


        // 21:00 - 05:00 means we want to be quiet if it's after 9 or before 5
        if (quietStarts > quietEnds) {
            // if it's 22:00 or 03:00 but not 8:00
            return now >= quietStarts || now <= quietEnds;
        }

        // 01:00 - 05:00
        else {

            // if it' 2:00 or 4:00 but not 8:00 or 0:00
            return now >= quietStarts && now <= quietEnds;
        }

    }

    public static void setDebug(boolean debug) {
        K9.DEBUG = debug;
        updateLoggingStatus();
    }

    public static boolean isDebug() {
        return DEBUG;
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
        return false;
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
                : mAttachmentDefaultPath;
    }

    public static void setAttachmentDefaultPath(String attachmentDefaultPath) {
        K9.mAttachmentDefaultPath = attachmentDefaultPath;
    }

    public static String getpEpNewKeysPassphrase(){
        return pEpNewKeysPassphrase;
    }

    public static void setpEpNewKeysPassphrase(String passphrase){
        K9.pEpNewKeysPassphrase = passphrase;
    }

    public static ManageableSetting<Boolean> getpEpUseTrustwords() {
        return pEpUseTrustwords;
    }

    public static void setpEpUseTrustwords(ManageableSetting<Boolean> useTrustwords) {
        pEpUseTrustwords = useTrustwords;
    }

    public static boolean isUsingTrustwords() {
        return pEpUseTrustwords.getValue();
    }

    public static void setpEpUseTrustwords(boolean useTrustwords) {
        pEpUseTrustwords.setValue(useTrustwords);
    }

    public void setAllowpEpSyncNewDevices(boolean allowpEpSyncNewDevices) {
        K9.allowpEpSyncNewDevices = allowpEpSyncNewDevices;
    }

    public static void setEchoProtocolEnabled(boolean enableEchoProtocol) {
        K9.enableEchoProtocol = enableEchoProtocol;
    }

    public static boolean isEchoProtocolEnabled() {
        return enableEchoProtocol;
    }

    public static void setMediaKeys(Set<MediaKey> mediaKeys) {
        K9.mediaKeys = mediaKeys;
    }

    public static Set<MediaKey> getMediaKeys() {
        return mediaKeys;
    }

    public static boolean ispEpUsingPassphraseForNewKey() {
        return pEpNewKeysPassphrase != null && !pEpNewKeysPassphrase.isEmpty();
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

    public static boolean isUsingpEpSyncFolder() {
        return usingpEpSyncFolder;
    }

    public static void setUsingpEpSyncFolder(boolean usingpEpSyncFolder) {
        K9.usingpEpSyncFolder = usingpEpSyncFolder;
    }

    public static boolean ispEpUsePassphraseForNewKeys() {
        return pEpUsePassphraseForNewKeys;
    }

    public static void setpEpUsePassphraseForNewKeys(boolean pEpUsePassphraseForNewKeys) {
        K9.pEpUsePassphraseForNewKeys = pEpUsePassphraseForNewKeys;
    }

    public static long getAppVersionCode() {
        return appVersionCode;
    }

    public static void setAppVersionCode(long appVersionCode) {
        K9.appVersionCode = appVersionCode;
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
     * {@code false}, otherwise.
     */
    public static synchronized boolean areDatabasesUpToDate() {
        return sDatabasesUpToDate;
    }


    /**
     * Remember that all account databases are using the most recent database schema.
     *
     * @param save Whether or not to write the current database version to the
     *             {@code SharedPreferences} {@link #DATABASE_VERSION_CACHE}.
     * @see #areDatabasesUpToDate()
     */
    public static synchronized void setDatabasesUpToDate(boolean save) {
        sDatabasesUpToDate = true;

        if (save) {
            Editor editor = sDatabaseVersionCache.edit();
            editor.putInt(KEY_LAST_ACCOUNT_DATABASE_VERSION, LocalStore.DB_VERSION);
            editor.apply();
        }
    }

    private static void updateLoggingStatus() {
        Timber.uprootAll();
        boolean enableDebugLogging = BuildConfig.DEBUG || DEBUG;
        if (enableDebugLogging) {
            Timber.plant(new DebugTree());
        }
    }

    public static boolean isShallRequestPermissions() {
        return shallRequestPermissions;
    }

    public static void setShallRequestPermissions(boolean shallRequestPermissions) {
        K9.shallRequestPermissions = shallRequestPermissions;
    }

    private ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        int activityCount = 0;

        @Override
        public void onActivityCreated(@NotNull Activity activity, Bundle savedInstanceState) {
            if (!(activity instanceof SplashScreen)) {
                if (activityCount == 0) {
//                if (activity instanceof K9Activity) pEpSyncProvider.setSyncHandshakeCallback((Sync.showHandshakeCallback) activity);
                    pEpProvider = PEpProviderFactory.createAndSetupProvider(getApplicationContext());
                    //pEpInitSyncEnvironment();
                }
                ++activityCount;
            }
        }

        @Override
        public void onActivityStarted(@NotNull Activity activity) {

        }

        @Override
        public void onActivityResumed(@NotNull Activity activity) {

        }

        @Override
        public void onActivityPaused(@NotNull Activity activity) {

        }

        @Override
        public void onActivityStopped(@NotNull Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(@NotNull Activity activity, @NotNull Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(@NotNull Activity activity) {
            if (!(activity instanceof SplashScreen)) {
                --activityCount;
                if (activityCount == 0) {
                    PEpProvider provider = PEpProviderFactory.createAndSetupProvider(K9.this);
                    KeySyncCleaner.queueAutoConsumeMessages();
                    if (provider.isSyncRunning()) provider.stopSync();
                    provider.close();
                    pEpProvider.close();
                    provider = null;
                    pEpProvider = null;
                }
            }
        }
    };

    public PEpProvider getpEpProvider() {
        return pEpProvider;
    }

    public static void setPEpExtraAccounts(String text) {
        pEpExtraAccounts = text;
    }

    public static String getPEpExtraAccounts() {
        return pEpExtraAccounts;
    }

    public void setPEpUseKeyserver(boolean use) {
        // Server lookup does not
        /*pEpUseKeyserver = use;
        if (use) {
            pEpProvider.startKeyserverLookup();
        } else{
            pEpProvider.stopKeyserverLookup();
        }*/

    }

    /**
     * @deprecated "Sequoia does not support SeverLookup"
     */
    @Deprecated
    public static boolean getPEpUseKeyserver() {
        // return pEpUseKeyserver;
        return false;
    }

    public static boolean getPEpPassiveMode() {
        return pEpPassiveMode;
    }

    public void setPEpPassiveMode(boolean enabled) {
        K9.pEpPassiveMode = enabled;
    }

    public static boolean ispEpSubjectProtection() {
        return pEpSubjectProtection;
    }

    public void setpEpSubjectProtection(boolean pEpSubjectProtection) {
        K9.pEpSubjectProtection = pEpSubjectProtection;
        pEpProvider.setSubjectProtection(pEpSubjectProtection);
        MessagingController.getInstance(this).setSubjectProtected(pEpSubjectProtection);
    }


    public static boolean ispEpForwardWarningEnabled() {
        return pEpForwardWarningEnabled;
    }

    public void setpEpForwardWarningEnabled(boolean pEpForwardWarningEnabled) {
        K9.pEpForwardWarningEnabled = pEpForwardWarningEnabled;
    }

    private void initializeInjector() {
        component = createApplicationComponent();
    }

    protected ApplicationComponent createApplicationComponent() {
        return DaggerApplicationComponent
                .factory()
                .create(this);
    }

    public ApplicationComponent getComponent() {
        return component;
    }

    private void setupFastPoller() {
        if (poller == null) {
            poller = new Poller(new Handler(Looper.getMainLooper()));
            poller.init(POLLING_INTERVAL, this::polling);
        } else {
            poller.stopPolling();
        }
        poller.startPolling();
    }

    private void polling() {
        if (needsFastPoll && !isPollingMessages) {
            Log.d("pEpDecrypt", "Entering looper");
            isPollingMessages = true;
            MessagingController messagingController = MessagingController.getInstance(this);
            messagingController.checkpEpSyncMail(K9.this, new PEpProvider.CompletedCallback() {
                @Override
                public void onComplete() {
                    isPollingMessages = false;
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("pEpSync", "onError: ", throwable);
                    isPollingMessages = false;
                }
            });
        }
    }

    public static boolean ispEpSyncEnabled() {
        return pEpSyncEnabled;
    }


    public void setpEpSyncEnabled(boolean enabled) {
        pEpSyncEnabled = enabled;

        if (enabled) {
            pEpInitSyncEnvironment();
        } else if (grouped) {
            leaveDeviceGroup();
        } else {
            shutdownSync();
        }
        forceSaveAppSettings();
    }

    public boolean needsFastPoll() {
        return needsFastPoll;
    }

    public boolean isShowingKeyimportDialog() {
        return showingKeyimportDialog;
    }

    public void setShowingKeyimportDialog(boolean showingKeyimportDialog) {
        this.showingKeyimportDialog = showingKeyimportDialog;
    }

    Sync.NotifyHandshakeCallback notifyHandshakeCallback = new Sync.NotifyHandshakeCallback() {

        @Override
        public void notifyHandshake(Identity myself, Identity partner, SyncHandshakeSignal signal) {
            Log.e("pEpEngine", String.format("pEp notifyHandshake: %s", signal.name()));

            if (isDebug()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(K9.this, signal.name(), Toast.LENGTH_LONG).show());
            }
            // Before starting a new "event" we dismiss the current one.
//            Intent broadcastIntent = new Intent("KEYSYNC_DISMISS");
//            K9.this.sendOrderedBroadcast(broadcastIntent, null);
            switch (signal) {
                case SyncNotifyUndefined:
                    break;
                case SyncNotifyInitAddOurDevice:
                case SyncNotifyInitAddOtherDevice:
                    if (allowpEpSyncNewDevices) {
                        ImportWizardFrompEp.actionStartKeySync(getApplicationContext(), myself, partner, signal, false);
                        needsFastPoll = true;
                    } else {
                        pEpSyncProvider.cancelSync();
                    }
                    break;
                case SyncNotifyInitFormGroup:
                    if (allowpEpSyncNewDevices) {
                        ImportWizardFrompEp.actionStartKeySync(getApplicationContext(), myself, partner, signal, true);
                        needsFastPoll = true;
                    } else {
                        pEpSyncProvider.cancelSync();
                    }
                    break;
                case SyncNotifyTimeout:
                    //Close handshake
                    ImportWizardFrompEp.notifyNewSignal(getApplicationContext(), signal);
                    needsFastPoll = false;
                    break;
                case SyncNotifyAcceptedDeviceAdded:
                case SyncNotifyAcceptedGroupCreated:
                    needsFastPoll = false;
                    break;
                case SyncNotifySole:
                    needsFastPoll = false;
                    grouped = false;
                    ImportWizardFrompEp.notifyNewSignal(getApplicationContext(), signal);
                    break;
                case SyncNotifyInGroup:
                    needsFastPoll = false;
                    grouped = true;
                    pEpSyncEnabled = true;
                    ImportWizardFrompEp.notifyNewSignal(getApplicationContext(), signal);
                    break;
                case SyncPassphraseRequired:
                    needsFastPoll = false;
                    Timber.e("Showing passphrase dialog for sync");
                   // PassphraseProvider.INSTANCE.passphraseFromUser(K9.this);
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            PassphraseActivity.notifyRequest(K9.this, PassphraseRequirementType.SYNC_PASSPHRASE), 4000);
                    break;
            }


        }
    };

    public Sync.NotifyHandshakeCallback getNotifyHandshakeCallback() {
        return this.notifyHandshakeCallback;
    }

    public void setGrouped(boolean value) {
        this.grouped = value;
    }

    public boolean isGrouped() {
        return this.grouped;
    }

    public void leaveDeviceGroup() {
        grouped = false;
        if (pEpSyncProvider.isSyncRunning()) {
            pEpSyncProvider.leaveDeviceGroup();
        }
        pEpSyncEnabled = false;
    }

    public void shutdownSync() {
        Log.e("pEpEngine", "shutdownSync: start" );
        if (pEpProvider.isSyncRunning()) {
            pEpProvider.stopSync();
        }
        pEpSyncEnabled = false;
        Log.e("pEpEngine", "shutdownSync: end" );
    }

    public void persistentShutDown() {
        shutdownSync();
        forceSaveAppSettings();
    }

    public void shutdownSync(PEpProvider pEpProvider) {
        Log.e("pEpEngine", "shutdownSync: start" );
        if (pEpProvider.isSyncRunning()) {
            Log.e("pEpEngine", "shutdownSync: stopping" );

            pEpProvider.stopSync();
        }
        pEpSyncEnabled = false;
        Log.e("pEpEngine", "shutdownSync: end" );

    }

    private void forceSaveAppSettings() {
        StorageEditor editor = Preferences.getPreferences(this).getStorage().edit();
        save(editor);
        editor.commit();
    }

    private void startConnectivityMonitor() {
        connectivityMonitor.register(this);
    }

}
