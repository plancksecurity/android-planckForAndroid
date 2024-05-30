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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
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
import com.fsck.k9.planck.LangUtils;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckProviderFactory;
import com.fsck.k9.planck.ui.tools.AppTheme;
import com.fsck.k9.planck.ui.tools.Theme;
import com.fsck.k9.planck.ui.tools.ThemeManager;
import com.fsck.k9.power.DeviceIdleManager;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.provider.UnreadWidgetProvider;
import com.fsck.k9.service.BootReceiver;
import com.fsck.k9.service.MailServiceLegacy;
import com.fsck.k9.service.ShutdownReceiver;
import com.fsck.k9.service.StorageGoneReceiver;
import com.fsck.k9.widget.list.MessageListWidgetProvider;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Provider;

import dagger.hilt.android.HiltAndroidApp;
import foundation.pEp.jniadapter.AndroidHelper;
import foundation.pEp.jniadapter.Sync;
import security.planck.appalive.AppAliveMonitor;
import security.planck.audit.AuditLogger;
import security.planck.file.PlanckSystemFileLocator;
import security.planck.mdm.ManageableSetting;
import security.planck.mdm.ManageableSettingKt;
import security.planck.mdm.MediaKey;
import security.planck.mdm.RestrictionsReceiver;
import security.planck.mdm.UserProfile;
import security.planck.network.ConnectionMonitor;
import security.planck.passphrase.PassphraseRepository;
import security.planck.provisioning.ProvisioningManager;
import security.planck.sync.KeySyncCleaner;
import security.planck.sync.SyncRepository;
import security.planck.ui.passphrase.PassphraseActivity;
import security.planck.ui.passphrase.PassphraseRequirementType;
import timber.log.Timber;
import timber.log.Timber.DebugTree;

@HiltAndroidApp
public class K9 extends MultiDexApplication implements DefaultLifecycleObserver {
    public static final boolean DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE = false;
    public PlanckProvider planckProvider;
    private final ConnectionMonitor connectivityMonitor = new ConnectionMonitor();
    private static boolean enableEchoProtocol = false;
    private static Set<MediaKey> mediaKeys;
    private Boolean runningOnWorkProfile;

    private final AtomicBoolean deviceJustLeftGroup = new AtomicBoolean(false);
    private static final Long THIRTY_DAYS_IN_SECONDS = 2592000L;
    private static ManageableSetting<Long> auditLogDataTimeRetention =
            new ManageableSetting<>(THIRTY_DAYS_IN_SECONDS);

    @Inject
    Preferences preferences;
    @Inject
    ProvisioningManager provisioningManager;
    @Inject
    PlanckSystemFileLocator planckSystemFileLocator;
    @Inject
    AppAliveMonitor appAliveMonitor;
    @Inject
    Provider<RestrictionsReceiver> restrictionsReceiver;
    @Inject
    Provider<AuditLogger> auditLogger;

    @Inject
    Provider<SyncRepository> syncRepository;
    @Inject
    PassphraseRepository passphraseRepository;

    public static K9JobManager jobManager;

    public static Set<String> getMasterKeys() {
        return pEpExtraKeys;
    }

    public static void setMasterKeys(Set<String> keys) {
        pEpExtraKeys = keys;
    }

    public boolean isRunningOnWorkProfile() {
        if (!BuildConfig.IS_OFFICIAL) return false;
        if (runningOnWorkProfile == null) {
            runningOnWorkProfile = new UserProfile().isRunningOnWorkProfile(this);
        }
        return runningOnWorkProfile;
    }

    private boolean runningInForeground;

    public boolean isRunningInForeground() {
        return runningInForeground;
    }

    public boolean isBatteryOptimizationAsked() {
        return batteryOptimizationAsked;
    }

    private boolean batteryOptimizationAsked;

    public void batteryOptimizationAsked() {
        batteryOptimizationAsked = true;
    }


    public static final int VERSION_MIGRATE_OPENPGP_TO_ACCOUNTS = 63;
    public static final int DEFAULT_CONTACT_NAME_COLOR = 0xff00008f;

    public static String password = null;
    private static long PASSPHRASE_DELAY = 4000;


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
    public static ManageableSetting<Boolean> DEBUG = new ManageableSetting<>(false);

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
    private static boolean mMessageListSenderAboveSubject = true;
    private static boolean mShowContactName = true;
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
    private static boolean mWrapFolderNames = false;
    private static boolean mHideUserAgent = true;
    private static boolean mHideTimeZone = false;

    private static SortType mSortType = Account.DEFAULT_SORT_TYPE;
    private static Map<SortType, Boolean> mSortAscending = new HashMap<SortType, Boolean>();

    private static boolean sUseBackgroundAsUnreadIndicator = false;
    private static boolean sThreadedViewEnabled = true;
    private static SplitViewMode sSplitViewMode = SplitViewMode.NEVER;
    private static boolean sColorizeMissingContactPictures = DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE;
    private static String planckExtraAccounts = "";
    //private static boolean pEpUseKeyserver = false;
    private static boolean planckPassiveMode = false;
    private static boolean planckSubjectProtection = true;
    private static ManageableSetting<Boolean> planckForwardWarningEnabled =
            new ManageableSetting<>(true);
    private static ManageableSetting<Boolean> planckSyncEnabled = new ManageableSetting<>(true);
    private static boolean shallRequestPermissions = true;
    private static boolean usingpEpSyncFolder = true;
    private static long appVersionCode = -1;
    private static Set<String> pEpExtraKeys = Collections.emptySet();


    private static int sPgpInlineDialogCounter;
    private static int sPgpSignOnlyDialogCounter;

    private static String planckNewKeysPassphrase = "";

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
    public static final int DEFAULT_VISIBLE_LIMIT = 250;

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
        if (!PassphraseRepository.getPassphraseUnlocked()) return;
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

    public boolean deviceJustLeftGroup() {
        return deviceJustLeftGroup.get();
    }

    public void markDeviceJustLeftGroup(boolean justLeft) {
        deviceJustLeftGroup.set(justLeft);
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
        if (isRunningOnWorkProfile()) {
            registerReceiver(
                    restrictionsReceiver.get(),
                    new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
            );
        }
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
        editor.putString(
                "enableDebugLogging",
                ManageableSettingKt.serializeBooleanManageableSetting(DEBUG)
        );
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

        editor.putBoolean("useBackgroundAsUnreadIndicator", sUseBackgroundAsUnreadIndicator);
        editor.putBoolean("threadedView", sThreadedViewEnabled);
        editor.putString("splitViewMode", sSplitViewMode.name());
        editor.putBoolean("colorizeMissingContactPictures", sColorizeMissingContactPictures);

        editor.putInt("pgpInlineDialogCounter", sPgpInlineDialogCounter);
        editor.putInt("pgpSignOnlyDialogCounter", sPgpSignOnlyDialogCounter);

        editor.putString("pEpExtraAccounts", planckExtraAccounts);
        //editor.putBoolean("pEpUseKeyserver", pEpUseKeyserver);
        editor.putBoolean("pEpPassiveMode", planckPassiveMode);
        editor.putBoolean("pEpSubjectProtection", planckSubjectProtection);
        editor.putString(
                "pEpForwardWarningEnabled",
                ManageableSettingKt.serializeBooleanManageableSetting(planckForwardWarningEnabled)
        );
        editor.putString(
                "pEpEnableSync",
                ManageableSettingKt.serializeBooleanManageableSetting(planckSyncEnabled)
        );
        editor.putBoolean("shallRequestPermissions", shallRequestPermissions);

        editor.putBoolean("pEpSyncFolder", usingpEpSyncFolder);
        editor.putLong("appVersionCode", appVersionCode);
        editor.putBoolean("enableEchoProtocol", enableEchoProtocol);
        editor.putString("mediaKeys", serializeMediaKeys());
        editor.putString("extraKeys", serializeExtraKeys());
        editor.putString(
                "auditLogDataTimeRetention",
                ManageableSettingKt.serializeLongManageableSetting(auditLogDataTimeRetention)
        );

        fontSizes.save(editor);
    }

    @Override
    public void onCreate() {
        AndroidHelper.setup(this);

        if (K9.DEVELOPER_MODE) {
            StrictMode.enableDefaults();
        }

        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        //Debug.waitForDebugger();
        app = this;
        Globals.setContext(this);
        performOperationsOnUpdate();

        provisioningManager.startProvisioning();
    }

    private void initializeAuditLog() {
        auditLogger.get().addStopEventLog(appAliveMonitor.getLastAppAliveMonitoredTime());
        auditLogger.get().addStartEventLog();
        appAliveMonitor.startAppAliveMonitor();
    }

    public void finalizeSetup() {
        pEpSetupUiEngineSession();
        K9MailLib.setDebugStatus(new K9MailLib.DebugStatus() {
            @Override
            public boolean enabled() {
                return isDebug();
            }

            @Override
            public boolean debugSensitive() {
                return DEBUG_SENSITIVE;
            }
        });

        checkCachedDatabaseVersion();

        loadPrefs(preferences);

        /*
         * We have to give MimeMessage a temp directory because File.createTempFile(String, String)
         * doesn't work in Android and MimeMessage does not have access to a Context.
         */
        BinaryTempFileBody.setTempDirectory(getCacheDir());

        LocalKeyStore.setKeyStoreLocation(
                planckSystemFileLocator.getKeyStoreFolder().toString()
        );

        MessagingController messagingController = MessagingController.getInstance(this);
        // Perform engine provisioning just after its initialization in MessagingController
        planckProvider = messagingController.getPlanckProvider();
        provisioningManager.performInitializedEngineProvisioning();
        initializeAuditLog();
        passphraseRepository.initializeBlocking();

        initJobManager(preferences, messagingController);

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

        if (PassphraseRepository.getPassphraseUnlocked()) {
            refreshFoldersForAllAccounts();
            startupSync();
        }

        notifyObservers();

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        batteryOptimizationAsked = powerManager.isIgnoringBatteryOptimizations(packageName);
    }

    private void startupSync() {
        syncRepository.get().planckInitSyncEnvironment();
        syncRepository.get().setupFastPoller();
    }

    public void startAllServices() {
        setServicesEnabled(this);
        refreshFoldersForAllAccounts();
        startupSync();
    }

    private void performOperationsOnUpdate() {
        AppUpdater appUpdater = new AppUpdater(this, new File(getCacheDir().getAbsolutePath()));
        appUpdater.performOperationsOnUpdate();
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

    public void pEpInitSyncEnvironment() {
        syncRepository.get().planckInitSyncEnvironment();
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
        setDebug(
                ManageableSettingKt.deserializeBooleanManageableSetting(
                        storage.getString(
                                "enableDebugLogging",
                                ManageableSettingKt.serializeBooleanManageableSetting(
                                        new ManageableSetting<>(BuildConfig.DEVELOPER_MODE)
                                )
                        )
                )
        );
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
                true
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
        mShowContactName = storage.getBoolean("showContactName", true);
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
        planckExtraAccounts = storage.getString("pEpExtraAccounts", null);
        //pEpUseKeyserver = storage.getBoolean("pEpUseKeyserver", false);
        planckPassiveMode = storage.getBoolean("pEpPassiveMode", false);
        planckSubjectProtection = getValuePlanckSubjectProtection(storage);
        planckForwardWarningEnabled = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        "pEpForwardWarningEnabled",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(true)
                        )
                )
        );
        planckSyncEnabled = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        "pEpEnableSync",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(true)
                        )
                )
        );
        usingpEpSyncFolder = storage.getBoolean("pEpSyncFolder", planckSyncEnabled.getValue());
        appVersionCode = storage.getLong("appVersionCode", -1);
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
        enableEchoProtocol = storage.getBoolean("enableEchoProtocol", false);
        mediaKeys = parseMediaKeys(storage.getString("mediaKeys", null));
        pEpExtraKeys = parseExtraKeys(storage.getString("extraKeys", null));
        auditLogDataTimeRetention = ManageableSettingKt.deserializeLongManageableSetting(
                storage.getString(
                        "auditLogDataTimeRetention",
                        ManageableSettingKt.serializeLongManageableSetting(
                                new ManageableSetting<>(THIRTY_DAYS_IN_SECONDS)
                        )
                )
        );
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

    private static Set<String> parseExtraKeys(String extraKeysString) {
        Set<String> set = Collections.emptySet();
        if (extraKeysString != null) {
            String[] array = extraKeysString.split(",");
            return new HashSet<>(Arrays.asList(array));
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

    private static String serializeExtraKeys() {
        if (pEpExtraKeys == null || pEpExtraKeys.isEmpty()) return null;
        return String.join(",", pEpExtraKeys);
    }

    private static boolean getValuePlanckSubjectProtection(Storage storage) {
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

    public static void setDebug(ManageableSetting<Boolean> debug) {
        K9.DEBUG = debug;
        updateLoggingStatus();
    }

    public static ManageableSetting<Boolean> getDebug() {
        return DEBUG;
    }

    public static void setDebug(boolean debug) {
        K9.DEBUG.setValue(debug);
        updateLoggingStatus();
    }

    public static boolean isDebug() {
        return DEBUG.getValue();
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
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .getAbsolutePath();
    }

    public static String getPlanckNewKeysPassphrase(){
        return planckNewKeysPassphrase;
    }

    public static void setPlanckNewKeysPassphrase(String passphrase){
        K9.planckNewKeysPassphrase = passphrase;
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

    public void setAuditLogDataTimeRetention(Long auditLogDataTimeRetention) {
        auditLogger.get().setLogAgeLimit(auditLogDataTimeRetention);
        K9.auditLogDataTimeRetention.setValue(auditLogDataTimeRetention);
    }

    public Long getAuditLogDataTimeRetentionValue() {
        return auditLogDataTimeRetention.getValue();
    }

    public void setAuditLogDataTimeRetention(ManageableSetting<Long> auditLogDataTimeRetention) {
        auditLogger.get().setLogAgeLimit(auditLogDataTimeRetention.getValue());
        K9.auditLogDataTimeRetention = auditLogDataTimeRetention;
    }

    public ManageableSetting<Long> getAuditLogDataTimeRetention() {
        return auditLogDataTimeRetention;
    }

    public AuditLogger getAuditLogger() {
        return auditLogger.get();
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
        boolean enableDebugLogging = BuildConfig.DEBUG || isDebug();
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
            ++activityCount;
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
            --activityCount;
            if (activityCount == 0) {
                KeySyncCleaner.queueAutoConsumeMessages();
            }
        }
    };

    public PlanckProvider getPlanckProvider() {
        return planckProvider;
    }

    public static void setPlanckExtraAccounts(String text) {
        planckExtraAccounts = text;
    }

    public static String getPlanckExtraAccounts() {
        return planckExtraAccounts;
    }

    public void setPlanckUseKeyserver(boolean use) {
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
    public static boolean getPlanckUseKeyserver() {
        // return pEpUseKeyserver;
        return false;
    }

    public static boolean getPlanckPassiveMode() {
        return planckPassiveMode;
    }

    public void setPlanckPassiveMode(boolean enabled) {
        K9.planckPassiveMode = enabled;
    }

    public static boolean isPlanckSubjectProtection() {
        return planckSubjectProtection;
    }

    public void setPlanckSubjectProtection(boolean planckSubjectProtection) {
        K9.planckSubjectProtection = planckSubjectProtection;
        planckProvider.setSubjectProtection(planckSubjectProtection);
        MessagingController.getInstance(this).setSubjectProtected(planckSubjectProtection);
    }

    public static ManageableSetting<Boolean> getPlanckForwardWarningEnabled() {
        return planckForwardWarningEnabled;
    }

    public void setPlanckForwardWarningEnabled(ManageableSetting<Boolean> planckForwardWarningEnabled) {
        K9.planckForwardWarningEnabled = planckForwardWarningEnabled;
    }

    public static boolean isPlanckForwardWarningEnabled() {
        return planckForwardWarningEnabled.getValue();
    }

    public void setPlanckForwardWarningEnabled(boolean planckForwardWarningEnabled) {
        K9.planckForwardWarningEnabled.setValue(planckForwardWarningEnabled);
    }

    public static ManageableSetting<Boolean> getPlanckSyncEnabled() {
        return planckSyncEnabled;
    }

    public void setPlanckSyncEnabled(ManageableSetting<Boolean> planckSyncEnabled) {
        boolean changed = K9.planckSyncEnabled.getValue() != planckSyncEnabled.getValue();
        K9.planckSyncEnabled = planckSyncEnabled;
        if (planckProvider != null && changed) {
            setPlanckSyncEnabledInRepository(planckSyncEnabled.getValue());
        }
    }

    public static boolean isPlanckSyncEnabled() {
        return planckSyncEnabled.getValue();
    }


    public void setPlanckSyncEnabled(boolean enabled) {
        if (enabled != planckSyncEnabled.getValue()) {
            planckSyncEnabled.setValue(enabled);
            setPlanckSyncEnabledInRepository(enabled);
        }
    }

    private void setPlanckSyncEnabledInRepository(boolean enabled) {
        syncRepository.get().setPlanckSyncEnabled(enabled);
        forceSaveAppSettings();
    }

    public void markSyncEnabled(boolean enabled) {
        planckSyncEnabled.setValue(enabled);
    }

    public Sync.NotifyHandshakeCallback getNotifyHandshakeCallback() {
        return syncRepository.get().getNotifyHandshakeCallback();
    }

    public void showHandshakeSignalOnDebug(String signalName) {
        if (isDebug()) {
            Log.e("pEpEngine", String.format("pEp notifyHandshake: %s", signalName));
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(K9.this, signalName, Toast.LENGTH_LONG).show());
        }
    }

    public void showPassphraseDialogForSync() {
        Timber.e("Showing passphrase dialog for sync");
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                PassphraseActivity.notifyRequest(K9.this,
                        PassphraseRequirementType.SYNC_PASSPHRASE), PASSPHRASE_DELAY);
    }

    private void forceSaveAppSettings() {
        StorageEditor editor = Preferences.getPreferences(this).getStorage().edit();
        save(editor);
        editor.commit();
    }

    private void startConnectivityMonitor() {
        connectivityMonitor.register(this);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        runningInForeground = true;
        auditLogger.get().checkPendingTamperingWarningFromBackground();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        runningInForeground = false;
    }

}
