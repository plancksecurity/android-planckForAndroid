
package com.fsck.k9;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;

import security.planck.mdm.ManageableSetting;
import security.planck.mdm.ManageableSettingKt;
import timber.log.Timber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.fsck.k9.account.AccountCreator;
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection;
import com.fsck.k9.auth.OAuthProviderType;
import com.fsck.k9.backends.RealOAuthTokenProviderFactory;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Folder.FolderClass;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.OAuthTokenProviderFactory;
import com.fsck.k9.mail.ssl.LocalKeyStore;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.transport.smtp.SmtpChecker;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.StorageManager;
import com.fsck.k9.mailstore.StorageManager.StorageProvider;
import com.fsck.k9.planck.ui.ConnectionSettings;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;
import com.fsck.k9.provider.EmailProvider;
import com.fsck.k9.provider.EmailProvider.StatsColumns;
import com.fsck.k9.search.ConditionsTreeNode;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchSpecification.Attribute;
import com.fsck.k9.search.SearchSpecification.SearchCondition;
import com.fsck.k9.search.SearchSpecification.SearchField;
import com.fsck.k9.search.SqlQueryBuilderInvoker;
import com.larswerkman.colorpicker.ColorPicker;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.fsck.k9.Preferences.getEnumStringPref;

/**
 * Account stores all of the settings for a single account defined by the user. It is able to save
 * and delete itself given a Preferences to work with. Each account is defined by a UUID.
 */
public class Account implements BaseAccount, StoreConfig {

    /**
     * This local folder is used to store messages to be sent.
     */
    public static final String OUTBOX = "PEP_INTERNAL_OUTBOX";
    private final boolean DEFAULT_PEP_SYNC_ENABLED = true;
    private ManageableSetting<Boolean> planckSyncEnabled;
    private String oAuthState;
    private OAuthProviderType mandatoryOAuthProviderType;

    public OAuthProviderType getMandatoryOAuthProviderType() {
        return mandatoryOAuthProviderType;
    }

    /**
     * Only to be done when the OAuth type is set as mandatory. This can be:
     * - when the user chooses this type of provider at the beginning of the account setup
     * - when the IT Manager sets this value via MDM
     * NOT to be used when the OAuth type is guessed from mail settings (case of Google)
     *
     * @param oAuthProviderType [OAuthProviderType] to be set as mandatory.
     */
    public void setMandatoryOAuthProviderType(OAuthProviderType oAuthProviderType) {
        this.mandatoryOAuthProviderType = oAuthProviderType;
    }

    public synchronized String getOAuthState() {
        return oAuthState;
    }

    public synchronized void setOAuthState(String oAuthState) {
        this.oAuthState = oAuthState;
    }

    public boolean isPlanckPrivacyProtected() {
        return planckPrivacyProtected.getValue();
    }

    public void setPlanckPrivacyProtection(boolean privacyProtection) {
        this.planckPrivacyProtected.setValue(privacyProtection);
    }

    public ManageableSetting<Boolean> getPlanckPrivacyProtected() {
        return planckPrivacyProtected;
    }

    public void setPlanckPrivacyProtection(ManageableSetting<Boolean> config) {
        planckPrivacyProtected = config;
    }

    public void setMailSettings(
            Context context,
            ConnectionSettings connectionSettings,
            boolean keepPasswords
    ) {
        ServerSettings incomingServerSettings = connectionSettings.getIncoming();
        ServerSettings outgoingServerSettings = connectionSettings.getOutgoing();

        if (keepPasswords) {
            String incomingPassword = null;
            String outgoingPassword = null;
            if (storeUri != null) {
                incomingPassword = RemoteStore.decodeStoreUri(storeUri).password;
            }
            if (transportUri != null) {
                outgoingPassword = Transport.decodeTransportUri(transportUri).password;
            }
            incomingServerSettings = incomingServerSettings.newPassword(incomingPassword);
            outgoingServerSettings = outgoingServerSettings.newPassword(outgoingPassword);
        }
        setStoreUri(RemoteStore.createStoreUri(incomingServerSettings));
        setTransportUri(Transport.createTransportUri(outgoingServerSettings));
        deletePolicy = AccountCreator.getDefaultDeletePolicy(incomingServerSettings.type);
        setupFolderNames(context, incomingServerSettings.host.toLowerCase());
    }

    private void setupFolderNames(Context context, String domain) {
        draftsFolderName = context.getString(R.string.special_mailbox_name_drafts);
        trashFolderName = context.getString(R.string.special_mailbox_name_trash);
        sentFolderName = context.getString(R.string.special_mailbox_name_sent);
        archiveFolderName = context.getString(R.string.special_mailbox_name_archive);

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain != null && domain.endsWith(".yahoo.com")) {
            spamFolderName = "Bulk Mail";
        } else {
            spamFolderName = context.getString(R.string.special_mailbox_name_spam);
        }
    }

    public Boolean isPlanckSyncEnabled() {
        return planckSyncEnabled.getValue();
    }

    public void setPlanckSyncAccount(Boolean planckSyncEnabled) {
        this.planckSyncEnabled.setValue(planckSyncEnabled);
    }

    public ManageableSetting<Boolean> getPlanckSyncEnabled() {
        return planckSyncEnabled;
    }

    public void setPlanckSyncAccount(ManageableSetting<Boolean> planckSyncEnabled) {
        this.planckSyncEnabled = planckSyncEnabled;
    }

    @NonNull
    public static FolderMode getDefaultFolderDisplayMode() {
        return FolderMode.ALL;
    }

    public enum Expunge {
        EXPUNGE_IMMEDIATELY,
        EXPUNGE_MANUALLY,
        EXPUNGE_ON_POLL
    }

    public enum SetupState {
        INITIAL,
        READY
    }
    private SetupState setupState;

    public SetupState getSetupState() {
        return setupState;
    }

    public void setSetupState(SetupState setupState) {
        this.setupState = setupState;
    }

    public enum DeletePolicy {
        NEVER(0),
        SEVEN_DAYS(1),
        ON_DELETE(2),
        MARK_AS_READ(3);

        public final int setting;

        DeletePolicy(int setting) {
            this.setting = setting;
        }

        public String preferenceString() {
            return Integer.toString(setting);
        }

        public static DeletePolicy fromInt(int initialSetting) {
            for (DeletePolicy policy: values()) {
                if (policy.setting == initialSetting) {
                    return policy;
                }
            }
            throw new IllegalArgumentException("DeletePolicy " + initialSetting + " unknown");
        }
    }

    public static final MessageFormat DEFAULT_MESSAGE_FORMAT = MessageFormat.HTML;
    public static final boolean DEFAULT_MESSAGE_FORMAT_AUTO = false;
    public static final QuoteStyle DEFAULT_QUOTE_STYLE = QuoteStyle.HEADER;
    public static final String DEFAULT_QUOTE_PREFIX = ">";
    public static final boolean DEFAULT_QUOTED_TEXT_SHOWN = false;
    public static final boolean DEFAULT_REPLY_AFTER_QUOTE = false;
    public static final boolean DEFAULT_STRIP_SIGNATURE = true;
    public static final int DEFAULT_REMOTE_SEARCH_NUM_RESULTS = 50;

    public static final String ACCOUNT_DESCRIPTION_KEY = "description";
    public static final String STORE_URI_KEY = "storeUri";
    public static final String TRANSPORT_URI_KEY = "transportUri";

    public static final String IDENTITY_NAME_KEY = "name";
    public static final String IDENTITY_EMAIL_KEY = "email";
    public static final String IDENTITY_DESCRIPTION_KEY = "description";
    public static final boolean DEFAULT_PEP_PRIVACY_PROTECTED = true;
    public static final int DEFAULT_CHIP_COLOR = 0xFF0000FF;
    /*
     * http://developer.android.com/design/style/color.html
     * Note: Order does matter, it's the order in which they will be picked.
     */
    private static final Integer[] getPredeFinedColors() {
        return new Integer[] {
                Color.parseColor("#0099CC"),    // blue
                Color.parseColor("#669900"),    // green
                Color.parseColor("#FF8800"),    // orange
                Color.parseColor("#CC0000"),    // red
                Color.parseColor("#9933CC")     // purple
        };
    }

    public enum SortType {
        SORT_DATE(R.string.sort_earliest_first, R.string.sort_latest_first, false),
        SORT_ARRIVAL(R.string.sort_earliest_first, R.string.sort_latest_first, false),
        SORT_SUBJECT(R.string.sort_subject_alpha, R.string.sort_subject_re_alpha, true),
        SORT_SENDER(R.string.sort_sender_alpha, R.string.sort_sender_re_alpha, true),
        SORT_UNREAD(R.string.sort_unread_first, R.string.sort_unread_last, true),
        SORT_FLAGGED(R.string.sort_flagged_first, R.string.sort_flagged_last, true),
        SORT_ATTACHMENT(R.string.sort_attach_first, R.string.sort_unattached_first, true);

        private int ascendingToast;
        private int descendingToast;
        private boolean defaultAscending;

        SortType(int ascending, int descending, boolean ndefaultAscending) {
            ascendingToast = ascending;
            descendingToast = descending;
            defaultAscending = ndefaultAscending;
        }

        public int getToast(boolean ascending) {
            return (ascending) ? ascendingToast : descendingToast;
        }

        public boolean isDefaultAscending() {
            return defaultAscending;
        }
    }

    public static final SortType DEFAULT_SORT_TYPE = SortType.SORT_DATE;
    public static final boolean DEFAULT_SORT_ASCENDING = false;
    public static final String NO_OPENPGP_PROVIDER = "";
    public static final long NO_OPENPGP_KEY = 0;

    public static final int INTERVAL_MINUTES_NEVER = -1;

    private DeletePolicy deletePolicy = DeletePolicy.NEVER;

    private final String accountUuid;
    private String storeUri;

    /**
     * Storage provider ID, used to locate and manage the underlying DB/file
     * storage
     */
    private String localStorageProviderId;
    private String transportUri;
    private boolean doesAppendSentMessages;
    private ManageableSetting<String> description;
    private String alwaysBcc;
    private int automaticCheckIntervalMinutes;
    private ManageableSetting<Integer> displayCount;
    private int chipColor;
    private long latestOldMessageSeenTime;
    private boolean notifyNewMail;
    private FolderMode folderNotifyNewMailMode;
    private boolean notifySelfNewMail;
    private boolean notifyContactsMailOnly;
    private String inboxFolderName;
    private String draftsFolderName;
    private String sentFolderName;
    private String trashFolderName;
    private String archiveFolderName;
    private String spamFolderName;
    private String autoExpandFolderName;
    private FolderMode folderDisplayMode;
    private FolderMode folderSyncMode;
    private FolderMode folderPushMode;
    private FolderMode folderTargetMode;
    private int accountNumber;
    private boolean pushPollOnConnect;
    private boolean notifySync;
    private SortType sortType;
    private Map<SortType, Boolean> sortAscending = new HashMap<>();
    private ShowPictures showPictures;
    private boolean isSignatureBeforeQuotedText;
    private Expunge expungePolicy = Expunge.EXPUNGE_IMMEDIATELY;
    private int maxPushFolders;
    private int idleRefreshMinutes;
    private boolean goToUnreadMessageSearch;
    private final Map<NetworkType, Boolean> compressionMap = new ConcurrentHashMap<>();
    private Searchable searchableFolders;
    private boolean subscribedFoldersOnly;
    private int maximumPolledMessageAge;
    private int maximumAutoDownloadMessageSize;
    // Tracks if we have sent a notification for this account for
    // current set of fetched messages
    private boolean ringNotified;
    private MessageFormat messageFormat;
    private boolean messageFormatAuto;
    private QuoteStyle quoteStyle;
    private String quotePrefix;
    private ManageableSetting<Boolean> defaultQuotedTextShown;
    private boolean replyAfterQuote;
    private boolean stripSignature;
    private boolean syncRemoteDeletions;
    private String openPgpProvider;
    private long openPgpKey;
    private boolean autocryptPreferEncryptMutual;
    private boolean openPgpHideSignOnly;
    private boolean markMessageAsReadOnView;
    private boolean alwaysShowCcBcc;
    private ManageableSetting<Boolean> allowRemoteSearch;
    private boolean remoteSearchFullText;
    private ManageableSetting<Integer> remoteSearchNumResults;
    private ManageableSetting<Boolean> planckPrivacyProtected;

    /**
     * Indicates whether this account is enabled, i.e. ready for use, or not.
     *
     * <p>
     * Right now newly imported accounts are disabled if the settings file didn't contain a
     * password for the incoming and/or outgoing server.
     * </p>
     */
    private boolean isEnabled;

    /**
     * Name of the folder that was last selected for a copy or move operation.
     *
     * Note: For now this value isn't persisted. So it will be reset when
     *       K-9 Mail is restarted.
     */
    private String lastSelectedFolderName = null;

    private List<Identity> identities;

    private NotificationSetting notificationSetting = new NotificationSetting();

    public enum FolderMode {
        NONE, ALL, FIRST_CLASS, FIRST_AND_SECOND_CLASS, NOT_SECOND_CLASS
    }

    public enum ShowPictures {
        NEVER, ALWAYS, ONLY_FROM_CONTACTS
    }

    public enum Searchable {
        ALL, DISPLAYABLE, NONE
    }

    public enum QuoteStyle {
        PREFIX, HEADER
    }

    public enum MessageFormat {
        TEXT, HTML, AUTO
    }

    protected Account(Context context) {
        accountUuid = UUID.randomUUID().toString();
        setupState = SetupState.INITIAL;
        localStorageProviderId = StorageManager.getInstance(context).getDefaultProviderId();
        automaticCheckIntervalMinutes = INTERVAL_MINUTES_NEVER;
        idleRefreshMinutes = 24;
        pushPollOnConnect = true;
        displayCount = new ManageableSetting<>(K9.DEFAULT_VISIBLE_LIMIT);
        accountNumber = -1;
        notifyNewMail = true;
        folderNotifyNewMailMode = FolderMode.ALL;
        notifySync = true;
        notifySelfNewMail = true;
        notifyContactsMailOnly = false;
        folderDisplayMode = getDefaultFolderDisplayMode();
        folderSyncMode = FolderMode.FIRST_CLASS;
        folderPushMode = FolderMode.FIRST_CLASS;
        folderTargetMode = getDefaultFolderDisplayMode();
        sortType = DEFAULT_SORT_TYPE;
        sortAscending.put(DEFAULT_SORT_TYPE, DEFAULT_SORT_ASCENDING);
        showPictures = ShowPictures.NEVER;
        isSignatureBeforeQuotedText = false;
        expungePolicy = Expunge.EXPUNGE_IMMEDIATELY;
        autoExpandFolderName = Store.INBOX;
        inboxFolderName = Store.INBOX;
        maxPushFolders = 10;
        chipColor = pickColor(context);
        goToUnreadMessageSearch = false;
        subscribedFoldersOnly = false;
        maximumPolledMessageAge = -1;
        maximumAutoDownloadMessageSize = 0;
        messageFormat = DEFAULT_MESSAGE_FORMAT;
        messageFormatAuto = DEFAULT_MESSAGE_FORMAT_AUTO;
        quoteStyle = DEFAULT_QUOTE_STYLE;
        quotePrefix = DEFAULT_QUOTE_PREFIX;
        description = new ManageableSetting<>(null);
        defaultQuotedTextShown = new ManageableSetting<>(DEFAULT_QUOTED_TEXT_SHOWN);
        replyAfterQuote = DEFAULT_REPLY_AFTER_QUOTE;
        stripSignature = DEFAULT_STRIP_SIGNATURE;
        syncRemoteDeletions = true;
        openPgpKey = NO_OPENPGP_KEY;
        allowRemoteSearch = new ManageableSetting<>(true);
        remoteSearchFullText = false;
        remoteSearchNumResults = new ManageableSetting<>(DEFAULT_REMOTE_SEARCH_NUM_RESULTS);
        isEnabled = true;
        markMessageAsReadOnView = true;
        alwaysShowCcBcc = false;

        searchableFolders = Searchable.ALL;

        identities = new ArrayList<>();

        Identity identity = new Identity();
        identity.setSignatureUse(true);
        identity.setSignature(context.getString(R.string.default_signature));
        identity.setDescription(context.getString(R.string.default_identity_description));
        identities.add(identity);

        notificationSetting = new NotificationSetting();
        notificationSetting.setVibrate(false);
        notificationSetting.setVibratePattern(0);
        notificationSetting.setVibrateTimes(5);
        notificationSetting.setRingEnabled(true);
        notificationSetting.setRingtone("content://settings/system/notification_sound");
        notificationSetting.setLedColor(chipColor);

        planckPrivacyProtected = new ManageableSetting<>(
                DEFAULT_PEP_PRIVACY_PROTECTED
        );
        planckSyncEnabled = new ManageableSetting<>(DEFAULT_PEP_SYNC_ENABLED);
    }

    /*
     * Pick a nice Android guidelines color if we haven't used them all yet.
     */
    private int pickColor(Context context) {
        List<Account> accounts = Preferences.getPreferences(context).getAccounts();

        Integer[] predefinedColors = getPredeFinedColors();
        List<Integer> availableColors = new ArrayList<>(predefinedColors.length);
        Collections.addAll(availableColors, predefinedColors);

        for (Account account : accounts) {
            Integer color = account.getChipColor();
            if (availableColors.contains(color)) {
                availableColors.remove(color);
                if (availableColors.isEmpty()) {
                    break;
                }
            }
        }

        return (availableColors.isEmpty()) ? ColorPicker.getRandomColor() : availableColors.get(0);
    }

    protected Account(Preferences preferences, String uuid) {
        this.accountUuid = uuid;
        this.setupState = SetupState.READY;
        loadAccount(preferences);
    }

    /**
     * Load stored settings for this account.
     */
    private synchronized void loadAccount(Preferences preferences) {

        Storage storage = preferences.getStorage();

        setStoreUri(Base64.decode(storage.getString(accountUuid + ".storeUri", null)));
        localStorageProviderId = storage.getString(
                accountUuid + ".localStorageProvider", StorageManager.getInstance(K9.app).getDefaultProviderId());
        setTransportUri(Base64.decode(storage.getString(accountUuid + ".transportUri", null)));
        String descriptionText = storage.getString(accountUuid + ".description", null);
        description = descriptionText != null
                ? ManageableSettingKt.deserializeStringManageableSetting(descriptionText)
                : new ManageableSetting<>(null);
        alwaysBcc = storage.getString(accountUuid + ".alwaysBcc", alwaysBcc);
        automaticCheckIntervalMinutes = storage.getInt(accountUuid + ".automaticCheckIntervalMinutes", INTERVAL_MINUTES_NEVER);
        idleRefreshMinutes = storage.getInt(accountUuid + ".idleRefreshMinutes", 24);
        pushPollOnConnect = storage.getBoolean(accountUuid + ".pushPollOnConnect", true);
        displayCount = ManageableSettingKt.deserializeIntManageableSetting(
                storage.getString(
                        accountUuid + ".displayCount",
                        ManageableSettingKt.serializeIntManageableSetting(
                                new ManageableSetting<>(K9.DEFAULT_VISIBLE_LIMIT)
                        )
                )
        );
        if (displayCount.getValue() < 0) {
            displayCount.setValue(K9.DEFAULT_VISIBLE_LIMIT);
        }
        latestOldMessageSeenTime = storage.getLong(accountUuid + ".latestOldMessageSeenTime", 0);
        notifyNewMail = storage.getBoolean(accountUuid + ".notifyNewMail", false);

        folderNotifyNewMailMode = getEnumStringPref(storage, accountUuid + ".folderNotifyNewMailMode", FolderMode.ALL);
        notifySelfNewMail = storage.getBoolean(accountUuid + ".notifySelfNewMail", true);
        notifyContactsMailOnly = storage.getBoolean(accountUuid + ".notifyContactsMailOnly", false);
        notifySync = storage.getBoolean(accountUuid + ".notifyMailCheck", false);
        deletePolicy =  DeletePolicy.fromInt(storage.getInt(accountUuid + ".deletePolicy", DeletePolicy.NEVER.setting));
        inboxFolderName = storage.getString(accountUuid + ".inboxFolderName", Store.INBOX);
        draftsFolderName = storage.getString(accountUuid + ".draftsFolderName", "Drafts");
        sentFolderName = storage.getString(accountUuid + ".sentFolderName", "Sent");
        trashFolderName = storage.getString(accountUuid + ".trashFolderName", "Trash");
        archiveFolderName = storage.getString(accountUuid + ".archiveFolderName", "Archive");
        spamFolderName = storage.getString(accountUuid + ".spamFolderName", "Spam");
        expungePolicy = getEnumStringPref(storage, accountUuid + ".expungePolicy", Expunge.EXPUNGE_IMMEDIATELY);
        syncRemoteDeletions = storage.getBoolean(accountUuid + ".syncRemoteDeletions", true);

        maxPushFolders = storage.getInt(accountUuid + ".maxPushFolders", 10);
        goToUnreadMessageSearch = storage.getBoolean(accountUuid + ".goToUnreadMessageSearch", false);
        subscribedFoldersOnly = storage.getBoolean(accountUuid + ".subscribedFoldersOnly", false);
        maximumPolledMessageAge = storage.getInt(accountUuid + ".maximumPolledMessageAge", -1);
        maximumAutoDownloadMessageSize = storage.getInt(accountUuid + ".maximumAutoDownloadMessageSize", 32768);
        messageFormat =  getEnumStringPref(storage, accountUuid + ".messageFormat", DEFAULT_MESSAGE_FORMAT);
        messageFormatAuto = storage.getBoolean(accountUuid + ".messageFormatAuto", DEFAULT_MESSAGE_FORMAT_AUTO);
        if (messageFormatAuto && messageFormat == MessageFormat.TEXT) {
            messageFormat = MessageFormat.AUTO;
        }
        quoteStyle = getEnumStringPref(storage, accountUuid + ".quoteStyle", DEFAULT_QUOTE_STYLE);
        quotePrefix = storage.getString(accountUuid + ".quotePrefix", DEFAULT_QUOTE_PREFIX);
        defaultQuotedTextShown = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        accountUuid + ".defaultQuotedTextShown",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(DEFAULT_QUOTED_TEXT_SHOWN)
                        )
                )
        );
        replyAfterQuote = storage.getBoolean(accountUuid + ".replyAfterQuote", DEFAULT_REPLY_AFTER_QUOTE);
        stripSignature = storage.getBoolean(accountUuid + ".stripSignature", DEFAULT_STRIP_SIGNATURE);
        for (NetworkType type : NetworkType.values()) {
            Boolean useCompression = storage.getBoolean(accountUuid + ".useCompression." + type,
                                     true);
            compressionMap.put(type, useCompression);
        }

        autoExpandFolderName = storage.getString(accountUuid  + ".autoExpandFolderName", Store.INBOX);

        accountNumber = storage.getInt(accountUuid + ".accountNumber", 0);

        chipColor = storage.getInt(accountUuid + ".chipColor", ColorPicker.getRandomColor());

        sortType = getEnumStringPref(storage, accountUuid + ".sortTypeEnum", SortType.SORT_DATE);

        sortAscending.put(sortType, storage.getBoolean(accountUuid + ".sortAscending", false));

        showPictures = getEnumStringPref(storage, accountUuid + ".showPicturesEnum", ShowPictures.NEVER);

        notificationSetting.setVibrate(storage.getBoolean(accountUuid + ".vibrate", false));
        notificationSetting.setVibratePattern(storage.getInt(accountUuid + ".vibratePattern", 0));
        notificationSetting.setVibrateTimes(storage.getInt(accountUuid + ".vibrateTimes", 5));
        notificationSetting.setRingEnabled(storage.getBoolean(accountUuid + ".ring", true));
        notificationSetting.setRingtone(storage.getString(accountUuid + ".ringtone",
                                         "content://settings/system/notification_sound"));
        notificationSetting.setLed(storage.getBoolean(accountUuid + ".led", true));
        notificationSetting.setLedColor(storage.getInt(accountUuid + ".ledColor", chipColor));

        folderDisplayMode = getEnumStringPref(storage, accountUuid + ".folderDisplayMode", getDefaultFolderDisplayMode());

        folderSyncMode = getEnumStringPref(storage, accountUuid + ".folderSyncMode", FolderMode.FIRST_CLASS);

        folderPushMode = getEnumStringPref(storage, accountUuid + ".folderPushMode", FolderMode.FIRST_CLASS);

        folderTargetMode = getEnumStringPref(storage, accountUuid + ".folderTargetMode", getDefaultFolderDisplayMode());

        searchableFolders = getEnumStringPref(storage, accountUuid + ".searchableFolders", Searchable.ALL);

        isSignatureBeforeQuotedText = storage.getBoolean(accountUuid + ".signatureBeforeQuotedText", false);
        identities = loadIdentities(storage);

        openPgpProvider = storage.getString(accountUuid + ".openPgpProvider", "");
        openPgpKey = storage.getLong(accountUuid + ".cryptoKey", NO_OPENPGP_KEY);
        openPgpHideSignOnly = storage.getBoolean(accountUuid + ".openPgpHideSignOnly", true);
        autocryptPreferEncryptMutual = storage.getBoolean(accountUuid + ".autocryptMutualMode", false);
        allowRemoteSearch = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        accountUuid + ".allowRemoteSearch",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(true)
                        )
                )
        );
        remoteSearchFullText = storage.getBoolean(accountUuid + ".remoteSearchFullText", false);
        remoteSearchNumResults = ManageableSettingKt.deserializeIntManageableSetting(
                storage.getString(
                        accountUuid + ".remoteSearchNumResults",
                        ManageableSettingKt.serializeIntManageableSetting(
                                new ManageableSetting<>(DEFAULT_REMOTE_SEARCH_NUM_RESULTS)
                        )
                )
        );

        isEnabled = storage.getBoolean(accountUuid + ".enabled", true);
        markMessageAsReadOnView = storage.getBoolean(accountUuid + ".markMessageAsReadOnView", true);
        alwaysShowCcBcc = storage.getBoolean(accountUuid + ".alwaysShowCcBcc", false);

        planckPrivacyProtected = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        accountUuid + ".pEpPrivacyProtected",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(DEFAULT_PEP_PRIVACY_PROTECTED)
                        )
                )
        );
        planckSyncEnabled = ManageableSettingKt.deserializeBooleanManageableSetting(
                storage.getString(
                        accountUuid + ".pEpSync",
                        ManageableSettingKt.serializeBooleanManageableSetting(
                                new ManageableSetting<>(DEFAULT_PEP_SYNC_ENABLED)
                        )
                )
        );

        // Use email address as account description if necessary
        if (description.getValue() == null) {
            description.setValue(getEmail());
        }
        oAuthState = storage.getString(accountUuid + ".oAuthState", null);
        String oAuthProvider = storage.getString(accountUuid + ".oAuthProviderType", null);
        mandatoryOAuthProviderType = oAuthProvider != null
                ? OAuthProviderType.valueOf(oAuthProvider)
                : null;
    }

    protected synchronized void delete(Preferences preferences) {
        deleteCertificates();

        // Get the list of account UUIDs
        String[] uuids = preferences.getStorage().getString("accountUuids", "").split(",");

        // Create a list of all account UUIDs excluding this account
        List<String> newUuids = new ArrayList<>(uuids.length);
        for (String uuid : uuids) {
            if (!uuid.equals(accountUuid)) {
                newUuids.add(uuid);
            }
        }

        StorageEditor editor = preferences.getStorage().edit();

        // Only change the 'accountUuids' value if this account's UUID was listed before
        if (newUuids.size() < uuids.length) {
            String accountUuids = Utility.combine(newUuids.toArray(), ',');
            editor.putString("accountUuids", accountUuids);
        }

        editor.remove(accountUuid + ".storeUri");
        editor.remove(accountUuid + ".transportUri");
        editor.remove(accountUuid + ".description");
        editor.remove(accountUuid + ".name");
        editor.remove(accountUuid + ".email");
        editor.remove(accountUuid + ".alwaysBcc");
        editor.remove(accountUuid + ".automaticCheckIntervalMinutes");
        editor.remove(accountUuid + ".pushPollOnConnect");
        editor.remove(accountUuid + ".idleRefreshMinutes");
        editor.remove(accountUuid + ".lastAutomaticCheckTime");
        editor.remove(accountUuid + ".latestOldMessageSeenTime");
        editor.remove(accountUuid + ".notifyNewMail");
        editor.remove(accountUuid + ".notifySelfNewMail");
        editor.remove(accountUuid + ".deletePolicy");
        editor.remove(accountUuid + ".draftsFolderName");
        editor.remove(accountUuid + ".sentFolderName");
        editor.remove(accountUuid + ".trashFolderName");
        editor.remove(accountUuid + ".archiveFolderName");
        editor.remove(accountUuid + ".spamFolderName");
        editor.remove(accountUuid + ".autoExpandFolderName");
        editor.remove(accountUuid + ".accountNumber");
        editor.remove(accountUuid + ".vibrate");
        editor.remove(accountUuid + ".vibratePattern");
        editor.remove(accountUuid + ".vibrateTimes");
        editor.remove(accountUuid + ".ring");
        editor.remove(accountUuid + ".ringtone");
        editor.remove(accountUuid + ".folderDisplayMode");
        editor.remove(accountUuid + ".folderSyncMode");
        editor.remove(accountUuid + ".folderPushMode");
        editor.remove(accountUuid + ".folderTargetMode");
        editor.remove(accountUuid + ".signatureBeforeQuotedText");
        editor.remove(accountUuid + ".expungePolicy");
        editor.remove(accountUuid + ".syncRemoteDeletions");
        editor.remove(accountUuid + ".maxPushFolders");
        editor.remove(accountUuid + ".searchableFolders");
        editor.remove(accountUuid + ".chipColor");
        editor.remove(accountUuid + ".led");
        editor.remove(accountUuid + ".ledColor");
        editor.remove(accountUuid + ".goToUnreadMessageSearch");
        editor.remove(accountUuid + ".subscribedFoldersOnly");
        editor.remove(accountUuid + ".maximumPolledMessageAge");
        editor.remove(accountUuid + ".maximumAutoDownloadMessageSize");
        editor.remove(accountUuid + ".messageFormatAuto");
        editor.remove(accountUuid + ".quoteStyle");
        editor.remove(accountUuid + ".quotePrefix");
        editor.remove(accountUuid + ".sortTypeEnum");
        editor.remove(accountUuid + ".sortAscending");
        editor.remove(accountUuid + ".showPicturesEnum");
        editor.remove(accountUuid + ".replyAfterQuote");
        editor.remove(accountUuid + ".stripSignature");
        editor.remove(accountUuid + ".cryptoApp");
        editor.remove(accountUuid + ".cryptoAutoSignature");
        editor.remove(accountUuid + ".cryptoAutoEncrypt");
        editor.remove(accountUuid + ".cryptoApp");
        editor.remove(accountUuid + ".cryptoKey");
        editor.remove(accountUuid + ".cryptoSupportSignOnly");
        editor.remove(accountUuid + ".enabled");
        editor.remove(accountUuid + ".markMessageAsReadOnView");
        editor.remove(accountUuid + ".alwaysShowCcBcc");
        editor.remove(accountUuid + ".allowRemoteSearch");
        editor.remove(accountUuid + ".remoteSearchFullText");
        editor.remove(accountUuid + ".remoteSearchNumResults");
        editor.remove(accountUuid + ".defaultQuotedTextShown");
        editor.remove(accountUuid + ".displayCount");
        editor.remove(accountUuid + ".inboxFolderName");
        editor.remove(accountUuid + ".localStorageProvider");
        editor.remove(accountUuid + ".messageFormat");
        editor.remove(accountUuid + ".messageReadReceipt");
        editor.remove(accountUuid + ".notifyMailCheck");
        editor.remove(accountUuid + ".pEpStoreEncryptedOnServer");
        editor.remove(accountUuid + ".pEpPrivacyProtected");

        editor.remove(accountUuid + ".notifyContactsMailOnly");
        editor.remove(accountUuid + ".openPgpHideSignOnly");
        editor.remove(accountUuid + ".autocryptMutualMode");
        editor.remove(accountUuid + ".pEpSync");
        editor.remove(accountUuid + ".folderNotifyNewMailMode");

        for (NetworkType type : NetworkType.values()) {
            editor.remove(accountUuid + ".useCompression." + type.name());
        }
        deleteIdentities(preferences.getStorage(), editor);
        // TODO: Remove preference settings that may exist for individual
        // folders in the account.
        editor.commit();
    }

    private static int findNewAccountNumber(List<Integer> accountNumbers) {
        int newAccountNumber = -1;
        Collections.sort(accountNumbers);
        for (int accountNumber : accountNumbers) {
            if (accountNumber > newAccountNumber + 1) {
                break;
            }
            newAccountNumber = accountNumber;
        }
        newAccountNumber++;
        return newAccountNumber;
    }

    private static List<Integer> getExistingAccountNumbers(Preferences preferences) {
        List<Account> accounts = preferences.getAccounts();
        List<Integer> accountNumbers = new ArrayList<>(accounts.size());
        for (Account a : accounts) {
            accountNumbers.add(a.getAccountNumber());
        }
        return accountNumbers;
    }
    public static int generateAccountNumber(Preferences preferences) {
        List<Integer> accountNumbers = getExistingAccountNumbers(preferences);
        return findNewAccountNumber(accountNumbers);
    }

    public void move(Preferences preferences, boolean moveUp) {
        String[] uuids = preferences.getStorage().getString("accountUuids", "").split(",");
        StorageEditor editor = preferences.getStorage().edit();
        String[] newUuids = new String[uuids.length];
        if (moveUp) {
            for (int i = 0; i < uuids.length; i++) {
                if (i > 0 && uuids[i].equals(accountUuid)) {
                    newUuids[i] = newUuids[i-1];
                    newUuids[i-1] = accountUuid;
                }
                else {
                    newUuids[i] = uuids[i];
                }
            }
        }
        else {
            for (int i = uuids.length - 1; i >= 0; i--) {
                if (i < uuids.length - 1 && uuids[i].equals(accountUuid)) {
                    newUuids[i] = newUuids[i+1];
                    newUuids[i+1] = accountUuid;
                }
                else {
                    newUuids[i] = uuids[i];
                }
            }
        }
        String accountUuids = Utility.combine(newUuids, ',');
        editor.putString("accountUuids", accountUuids);
        editor.commit();
        preferences.loadAccounts();
    }

    public synchronized void save(Preferences preferences) {
        if(setupState == SetupState.INITIAL && BuildConfig.DEBUG) {
            throw new IllegalStateException("Trying to save an account in state INITIAL");
        }
        StorageEditor editor = preferences.getStorage().edit();

        if (!preferences.getStorage().getString("accountUuids", "").contains(accountUuid)) {
            /*
             * When the account is first created we assign it a unique account number. The
             * account number will be unique to that account for the lifetime of the account.
             * So, we get all the existing account numbers, sort them ascending, loop through
             * the list and check if the number is greater than 1 + the previous number. If so
             * we use the previous number + 1 as the account number. This refills gaps.
             * accountNumber starts as -1 on a newly created account. It must be -1 for this
             * algorithm to work.
             *
             * I bet there is a much smarter way to do this. Anyone like to suggest it?
             */
            List<Account> accounts = preferences.getAccounts();
            int[] accountNumbers = new int[accounts.size()];
            for (int i = 0; i < accounts.size(); i++) {
                accountNumbers[i] = accounts.get(i).getAccountNumber();
            }
            Arrays.sort(accountNumbers);
            for (int accountNumber : accountNumbers) {
                if (accountNumber > this.accountNumber + 1) {
                    break;
                }
                this.accountNumber = accountNumber;
            }
            accountNumber++;

            String accountUuids = preferences.getStorage().getString("accountUuids", "");
            accountUuids += (accountUuids.length() != 0 ? "," : "") + accountUuid;
            editor.putString("accountUuids", accountUuids);
        }

        editor.putString(accountUuid + ".storeUri", Base64.encode(storeUri));
        editor.putString(accountUuid + ".localStorageProvider", localStorageProviderId);
        editor.putString(accountUuid + ".transportUri", Base64.encode(transportUri));
        editor.putString(
                accountUuid + ".description",
                description.getValue() != null
                        ? ManageableSettingKt.serializeStringManageableSetting(description)
                        : null
        );
        editor.putString(accountUuid + ".alwaysBcc", alwaysBcc);
        editor.putInt(accountUuid + ".automaticCheckIntervalMinutes", automaticCheckIntervalMinutes);
        editor.putInt(accountUuid + ".idleRefreshMinutes", idleRefreshMinutes);
        editor.putBoolean(accountUuid + ".pushPollOnConnect", pushPollOnConnect);
        editor.putString(
                accountUuid + ".displayCount",
                ManageableSettingKt.serializeIntManageableSetting(displayCount)
        );
        editor.putLong(accountUuid + ".latestOldMessageSeenTime", latestOldMessageSeenTime);
        editor.putBoolean(accountUuid + ".notifyNewMail", notifyNewMail);
        editor.putString(accountUuid + ".folderNotifyNewMailMode", folderNotifyNewMailMode.name());
        editor.putBoolean(accountUuid + ".notifySelfNewMail", notifySelfNewMail);
        editor.putBoolean(accountUuid + ".notifyContactsMailOnly", notifyContactsMailOnly);
        editor.putBoolean(accountUuid + ".notifyMailCheck", notifySync);
        editor.putInt(accountUuid + ".deletePolicy", deletePolicy.setting);
        editor.putString(accountUuid + ".inboxFolderName", inboxFolderName);
        editor.putString(accountUuid + ".draftsFolderName", draftsFolderName);
        editor.putString(accountUuid + ".sentFolderName", sentFolderName);
        editor.putString(accountUuid + ".trashFolderName", trashFolderName);
        editor.putString(accountUuid + ".archiveFolderName", archiveFolderName);
        editor.putString(accountUuid + ".spamFolderName", spamFolderName);
        editor.putString(accountUuid + ".autoExpandFolderName", autoExpandFolderName);
        editor.putInt(accountUuid + ".accountNumber", accountNumber);
        editor.putString(accountUuid + ".sortTypeEnum", sortType.name());
        editor.putBoolean(accountUuid + ".sortAscending", sortAscending.get(sortType));
        editor.putString(accountUuid + ".showPicturesEnum", showPictures.name());
        editor.putString(accountUuid + ".folderDisplayMode", folderDisplayMode.name());
        editor.putString(accountUuid + ".folderSyncMode", folderSyncMode.name());
        editor.putString(accountUuid + ".folderPushMode", folderPushMode.name());
        editor.putString(accountUuid + ".folderTargetMode", folderTargetMode.name());
        editor.putBoolean(accountUuid + ".signatureBeforeQuotedText", this.isSignatureBeforeQuotedText);
        editor.putString(accountUuid + ".expungePolicy", expungePolicy.name());
        editor.putBoolean(accountUuid + ".syncRemoteDeletions", syncRemoteDeletions);
        editor.putInt(accountUuid + ".maxPushFolders", maxPushFolders);
        editor.putString(accountUuid + ".searchableFolders", searchableFolders.name());
        editor.putInt(accountUuid + ".chipColor", chipColor);
        editor.putBoolean(accountUuid + ".goToUnreadMessageSearch", goToUnreadMessageSearch);
        editor.putBoolean(accountUuid + ".subscribedFoldersOnly", subscribedFoldersOnly);
        editor.putInt(accountUuid + ".maximumPolledMessageAge", maximumPolledMessageAge);
        editor.putInt(accountUuid + ".maximumAutoDownloadMessageSize", maximumAutoDownloadMessageSize);
        if (MessageFormat.AUTO.equals(messageFormat)) {
            // saving MessageFormat.AUTO as is to the database will cause downgrades to crash on
            // startup, so we save as MessageFormat.TEXT instead with a separate flag for auto.
            editor.putString(accountUuid + ".messageFormat", Account.MessageFormat.TEXT.name());
            messageFormatAuto = true;
        } else {
            editor.putString(accountUuid + ".messageFormat", messageFormat.name());
            messageFormatAuto = false;
        }
        editor.putBoolean(accountUuid + ".messageFormatAuto", messageFormatAuto);
        editor.putString(accountUuid + ".quoteStyle", quoteStyle.name());
        editor.putString(accountUuid + ".quotePrefix", quotePrefix);
        editor.putString(
                accountUuid + ".defaultQuotedTextShown",
                ManageableSettingKt.serializeBooleanManageableSetting(defaultQuotedTextShown)
        );
        editor.putBoolean(accountUuid + ".replyAfterQuote", replyAfterQuote);
        editor.putBoolean(accountUuid + ".stripSignature", stripSignature);
        editor.putLong(accountUuid + ".cryptoKey", openPgpKey);
        editor.putBoolean(accountUuid + ".openPgpHideSignOnly", openPgpHideSignOnly);
        editor.putString(accountUuid + ".openPgpProvider", openPgpProvider);
        editor.putBoolean(accountUuid + ".autocryptMutualMode", autocryptPreferEncryptMutual);
        editor.putString(
                accountUuid + ".allowRemoteSearch",
                ManageableSettingKt.serializeBooleanManageableSetting(allowRemoteSearch)
        );
        editor.putBoolean(accountUuid + ".remoteSearchFullText", remoteSearchFullText);
        editor.putString(
                accountUuid + ".remoteSearchNumResults",
                ManageableSettingKt.serializeIntManageableSetting(remoteSearchNumResults)
        );
        editor.putBoolean(accountUuid + ".enabled", isEnabled);
        editor.putBoolean(accountUuid + ".markMessageAsReadOnView", markMessageAsReadOnView);
        editor.putBoolean(accountUuid + ".alwaysShowCcBcc", alwaysShowCcBcc);
        editor.putString(accountUuid + ".cryptoApp", NO_OPENPGP_PROVIDER);

        editor.putBoolean(accountUuid + ".vibrate", notificationSetting.isVibrateEnabled());
        editor.putInt(accountUuid + ".vibratePattern", notificationSetting.getVibratePattern());
        editor.putInt(accountUuid + ".vibrateTimes", notificationSetting.getVibrateTimes());
        editor.putBoolean(accountUuid + ".ring", notificationSetting.isRingEnabled());
        editor.putString(accountUuid + ".ringtone", notificationSetting.getRingtone());
        editor.putBoolean(accountUuid + ".led", notificationSetting.isLedEnabled());
        editor.putInt(accountUuid + ".ledColor", notificationSetting.getLedColor());
        editor.putString(
                accountUuid + ".pEpPrivacyProtected",
                ManageableSettingKt.serializeBooleanManageableSetting(planckPrivacyProtected)
        );
        editor.putString(
                accountUuid + ".pEpSync",
                ManageableSettingKt.serializeBooleanManageableSetting(planckSyncEnabled)
        );
        editor.putString(accountUuid + ".oAuthState", oAuthState);
        editor.putString(
                accountUuid + ".oAuthProviderType",
                mandatoryOAuthProviderType != null ? mandatoryOAuthProviderType.toString() : null
        );

        for (NetworkType type : NetworkType.values()) {
            Boolean useCompression = compressionMap.get(type);
            if (useCompression != null) {
                editor.putBoolean(accountUuid + ".useCompression." + type, useCompression);
            }
        }
        saveIdentities(preferences.getStorage(), editor);

        editor.commit();

    }

    private void resetVisibleLimits() {
        try {
            getLocalStore().resetVisibleLimits(getDisplayCount());
        } catch (MessagingException e) {
            Timber.e(e, "Unable to reset visible limits");
        }

    }

    /**
     * @return <code>null</code> if not available
     * @throws MessagingException
     * @see {@link #isAvailable(Context)}
     */
    public AccountStats getStats(Context context) throws MessagingException {
        if (!isAvailable(context)) {
            return null;
        }

        AccountStats stats = new AccountStats();

        ContentResolver cr = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(EmailProvider.CONTENT_URI,
                "account/" + getUuid() + "/stats");

        String[] projection = {
                StatsColumns.UNREAD_COUNT,
                StatsColumns.FLAGGED_COUNT
        };

        // Create LocalSearch instance to exclude special folders (Trash, Drafts, Spam, Outbox,
        // Sent) and limit the search to displayable folders.
        LocalSearch search = new LocalSearch();
        excludeSpecialFolders(search);
        limitToDisplayableFolders(search);

        // Use the LocalSearch instance to create a WHERE clause to query the content provider
        StringBuilder query = new StringBuilder();
        List<String> queryArgs = new ArrayList<>();
        ConditionsTreeNode conditions = search.getConditions();
        SqlQueryBuilderInvoker.buildWhereClause(this, conditions, query, queryArgs);

        String selection = query.toString();
        String[] selectionArgs = queryArgs.toArray(new String[0]);

        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                stats.unreadMessageCount = cursor.getInt(0);
                stats.flaggedMessageCount = cursor.getInt(1);
            }
        } finally {
            Utility.closeQuietly(cursor);
        }

        LocalStore localStore = getLocalStore();
        if (K9.measureAccounts()) {
            stats.size = localStore.getSize();
        }

        return stats;
    }


    public synchronized void setChipColor(int color) {
        chipColor = color;
    }

    public synchronized int getChipColor() {
        return chipColor;
    }

    @Override
    public String getUuid() {
        return accountUuid;
    }

    public synchronized String getStoreUri() {
        return storeUri;
    }

    public synchronized void setStoreUri(String storeUri) {
        this.storeUri = storeUri;
    }

    public synchronized String getTransportUri() {
        return transportUri;
    }

    public synchronized boolean doesAppendSentMessages() {
        return doesAppendSentMessages;
    }

    @Override
    public OAuth2TokenProvider getOAuth2TokenProvider() throws MessagingException {
        return getRemoteStore().getOauthTokenProvider();
    }

    public synchronized void setTransportUri(String transportUri) {
        this.transportUri = transportUri;
        doesAppendSentMessages = SmtpChecker.Companion.doesAppendSentMessages(transportUri);
    }

    @Override
    public synchronized String getDescription() {
        return description.getValue();
    }

    @Override
    public synchronized void setDescription(String description) {
        this.description.setValue(description);
    }

    public synchronized ManageableSetting<String> getLockableDescription() {
        return description;
    }

    public synchronized void setDescription(ManageableSetting<String> description) {
        this.description = description;
    }

    public synchronized String getName() {
        return identities.get(0).getName();
    }

    public synchronized void setName(String name) {
        identities.get(0).setName(name);
    }

    public synchronized boolean getSignatureUse() {
        return identities.get(0).getSignatureUse();
    }

    public synchronized void setSignatureUse(boolean signatureUse) {
        identities.get(0).setSignatureUse(signatureUse);
    }

    public synchronized String getSignature() {
        return identities.get(0).getSignature();
    }

    public synchronized void setSignature(String signature) {
        identities.get(0).setSignature(signature);
    }

    @Override
    public synchronized String getEmail() {
        return identities.get(0).getEmail();
    }

    @Override
    public synchronized void setEmail(String email) {
        identities.get(0).setEmail(email);
    }

    public synchronized String getAlwaysBcc() {
        return alwaysBcc;
    }

    public synchronized void setAlwaysBcc(String alwaysBcc) {
        this.alwaysBcc = alwaysBcc;
    }

    /* Have we sent a new mail notification on this account */
    public boolean isRingNotified() {
        return ringNotified;
    }

    public void setRingNotified(boolean ringNotified) {
        this.ringNotified = ringNotified;
    }

    public String getLocalStorageProviderId() {
        return localStorageProviderId;
    }

    public void setLocalStorageProviderId(String id) {

        if (!localStorageProviderId.equals(id)) {

            boolean successful = false;
            try {
                switchLocalStorage(id);
                successful = true;
            } catch (MessagingException e) {
                Timber.e(e, "Switching local storage provider from %s to %s failed.", localStorageProviderId, id);
            }

            // if migration to/from SD-card failed once, it will fail again.
            if (!successful) {
                return;
            }

            localStorageProviderId = id;
        }

    }

    /**
     * Returns -1 for never.
     */
    public synchronized int getAutomaticCheckIntervalMinutes() {
        //https://developer.android.com/reference/androidx/work/PeriodicWorkRequest#MIN_PERIODIC_INTERVAL_MILLIS
        if (automaticCheckIntervalMinutes != -1) return Math.max(15, automaticCheckIntervalMinutes);
        return automaticCheckIntervalMinutes;
    }

    /**
     * @param automaticCheckIntervalMinutes or -1 for never.
     */
    public synchronized boolean setAutomaticCheckIntervalMinutes(int automaticCheckIntervalMinutes) {
        int oldInterval = this.automaticCheckIntervalMinutes;
        this.automaticCheckIntervalMinutes = automaticCheckIntervalMinutes;

        return (oldInterval != automaticCheckIntervalMinutes);
    }

    public synchronized int getDisplayCount() {
        return displayCount.getValue();
    }

    public synchronized void setDisplayCount(int displayCount) {
        if (displayCount != -1) {
            this.displayCount.setValue(displayCount);
        } else {
            this.displayCount.setValue(K9.DEFAULT_VISIBLE_LIMIT);
        }
        if(setupState == SetupState.READY) {
            resetVisibleLimits();
        }
    }

    public synchronized ManageableSetting<Integer> getLockableDisplayCount() {
        return displayCount;
    }

    public synchronized void setDisplayCount(ManageableSetting<Integer> displayCount) {
        this.displayCount = displayCount;
        if (displayCount.getValue() == -1) {
            displayCount.setValue(K9.DEFAULT_VISIBLE_LIMIT);
        }
        if(setupState == SetupState.READY) {
            resetVisibleLimits();
        }
    }

    public void setOptionsOnInstall() {
        resetVisibleLimits();
    }

    public synchronized long getLatestOldMessageSeenTime() {
        return latestOldMessageSeenTime;
    }

    public synchronized void setLatestOldMessageSeenTime(long latestOldMessageSeenTime) {
        this.latestOldMessageSeenTime = latestOldMessageSeenTime;
    }

    public synchronized boolean isNotifyNewMail() {
        return notifyNewMail;
    }

    public synchronized void setNotifyNewMail(boolean notifyNewMail) {
        this.notifyNewMail = notifyNewMail;
    }

    public synchronized FolderMode getFolderNotifyNewMailMode() {
        return folderNotifyNewMailMode;
    }

    public synchronized void setFolderNotifyNewMailMode(FolderMode folderNotifyNewMailMode) {
        this.folderNotifyNewMailMode = folderNotifyNewMailMode;
    }

    public synchronized DeletePolicy getDeletePolicy() {
        return deletePolicy;
    }

    public synchronized void setDeletePolicy(DeletePolicy deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public boolean isSpecialFolder(String folderName) {
        return (folderName != null && (folderName.equalsIgnoreCase(getInboxFolderName()) ||
                folderName.equals(getTrashFolderName()) ||
                folderName.equals(getDraftsFolderName()) ||
                folderName.equals(getArchiveFolderName()) ||
                folderName.equals(getSpamFolderName()) ||
                folderName.equals(getOutboxFolderName()) ||
                folderName.equals(getSentFolderName()) ||
                folderName.equals(getCurrentpEpSyncFolderName())
        ));
    }

    public String getCurrentpEpSyncFolderName() {
        if (K9.isUsingpEpSyncFolder()) {
            return Store.PLANCK_FOLDER;
        } else {
            return inboxFolderName;
        }
    }

    public String getDefaultpEpSyncFolderName() {
        return Store.PLANCK_FOLDER;
    }

    public String getPlanckSuspiciousFolderName() {
        return Store.PLANCK_SUSPICIOUS_FOLDER;
    }

    public synchronized String getDraftsFolderName() {
        return draftsFolderName;
    }

    public synchronized void setDraftsFolderName(String name) {
        draftsFolderName = name;
    }

    /**
     * Checks if this account has a drafts folder set.
     * @return true if account has a drafts folder set.
     */
    public synchronized boolean hasDraftsFolder() {
        return !K9.FOLDER_NONE.equalsIgnoreCase(draftsFolderName);
    }

    public synchronized String getSentFolderName() {
        return sentFolderName;
    }

    public synchronized void setSentFolderName(String name) {
        sentFolderName = name;
    }

    /**
     * Checks if this account has a sent folder set.
     * @return true if account has a sent folder set.
     */
    public synchronized boolean hasSentFolder() {
        return !K9.FOLDER_NONE.equalsIgnoreCase(sentFolderName);
    }


    public synchronized String getTrashFolderName() {
        return trashFolderName;
    }

    public synchronized void setTrashFolderName(String name) {
        trashFolderName = name;
    }

    /**
     * Checks if this account has a trash folder set.
     * @return true if account has a trash folder set.
     */
    public synchronized boolean hasTrashFolder() {
        return !K9.FOLDER_NONE.equalsIgnoreCase(trashFolderName);
    }

    public synchronized String getArchiveFolderName() {
        return archiveFolderName;
    }

    public synchronized void setArchiveFolderName(String archiveFolderName) {
        this.archiveFolderName = archiveFolderName;
    }

    /**
     * Checks if this account has an archive folder set.
     * @return true if account has an archive folder set.
     */
    public synchronized boolean hasArchiveFolder() {
        return !K9.FOLDER_NONE.equalsIgnoreCase(archiveFolderName);
    }

    public synchronized String getSpamFolderName() {
        return spamFolderName;
    }

    public synchronized void setSpamFolderName(String name) {
        spamFolderName = name;
    }

    /**
     * Checks if this account has a spam folder set.
     * @return true if account has a spam folder set.
     */
    public synchronized boolean hasSpamFolder() {
        return !K9.FOLDER_NONE.equalsIgnoreCase(spamFolderName);
    }

    public synchronized String getOutboxFolderName() {
        return OUTBOX;
    }

    public synchronized String getAutoExpandFolderName() {
        return autoExpandFolderName;
    }

    public synchronized void setAutoExpandFolderName(String name) {
        autoExpandFolderName = name;
    }

    public synchronized int getAccountNumber() {
        return accountNumber;
    }

    public synchronized FolderMode getFolderDisplayMode() {
        return folderDisplayMode;
    }

    public synchronized boolean setFolderDisplayMode(FolderMode displayMode) {
        FolderMode oldDisplayMode = folderDisplayMode;
        folderDisplayMode = displayMode;
        return oldDisplayMode != displayMode;
    }

    public synchronized FolderMode getFolderSyncMode() {
        return folderSyncMode;
    }

    public synchronized boolean setFolderSyncMode(FolderMode syncMode) {
        FolderMode oldSyncMode = folderSyncMode;
        folderSyncMode = syncMode;

        if (syncMode == FolderMode.NONE && oldSyncMode != FolderMode.NONE) {
            return true;
        }
        if (syncMode != FolderMode.NONE && oldSyncMode == FolderMode.NONE) {
            return true;
        }
        return false;
    }

    public synchronized FolderMode getFolderPushMode() {
        return folderPushMode;
    }

    public synchronized boolean setFolderPushMode(FolderMode pushMode) {
        FolderMode oldPushMode = folderPushMode;

        folderPushMode = pushMode;
        return pushMode != oldPushMode;
    }

    public synchronized boolean isShowOngoing() {
        return notifySync;
    }

    public synchronized void setShowOngoing(boolean showOngoing) {
        this.notifySync = showOngoing;
    }

    public synchronized SortType getSortType() {
        return sortType;
    }

    public synchronized void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    public synchronized boolean isSortAscending(SortType sortType) {
        if (sortAscending.get(sortType) == null) {
            sortAscending.put(sortType, sortType.isDefaultAscending());
        }
        return sortAscending.get(sortType);
    }

    public synchronized void setSortAscending(SortType sortType, boolean sortAscending) {
        this.sortAscending.put(sortType, sortAscending);
    }

    public synchronized ShowPictures getShowPictures() {
        return showPictures;
    }

    public synchronized void setShowPictures(ShowPictures showPictures) {
        this.showPictures = showPictures;
    }

    public synchronized FolderMode getFolderTargetMode() {
        return folderTargetMode;
    }

    public synchronized void setFolderTargetMode(FolderMode folderTargetMode) {
        this.folderTargetMode = folderTargetMode;
    }

    public synchronized boolean isSignatureBeforeQuotedText() {
        return isSignatureBeforeQuotedText;
    }

    public synchronized void setSignatureBeforeQuotedText(boolean mIsSignatureBeforeQuotedText) {
        this.isSignatureBeforeQuotedText = mIsSignatureBeforeQuotedText;
    }

    public synchronized boolean isNotifySelfNewMail() {
        return notifySelfNewMail;
    }

    public synchronized void setNotifySelfNewMail(boolean notifySelfNewMail) {
        this.notifySelfNewMail = notifySelfNewMail;
    }

    public synchronized boolean isNotifyContactsMailOnly() {
        return notifyContactsMailOnly;
    }

    public synchronized void setNotifyContactsMailOnly(boolean notifyContactsMailOnly) {
        this.notifyContactsMailOnly = notifyContactsMailOnly;
    }

    public synchronized Expunge getExpungePolicy() {
        return expungePolicy;
    }

    public synchronized void setExpungePolicy(Expunge expungePolicy) {
        this.expungePolicy = expungePolicy;
    }

    public synchronized int getMaxPushFolders() {
        return maxPushFolders;
    }

    public synchronized boolean setMaxPushFolders(int maxPushFolders) {
        int oldMaxPushFolders = this.maxPushFolders;
        this.maxPushFolders = maxPushFolders;
        return oldMaxPushFolders != maxPushFolders;
    }

    public LocalStore getLocalStore() throws MessagingException {
        return LocalStore.getInstance(this, K9.app);
    }

    public RemoteStore getRemoteStore() throws MessagingException {
        return RemoteStore.getInstance(K9.app, this, getOAuthTokenProviderFactory());
    }

    private OAuthTokenProviderFactory getOAuthTokenProviderFactory() {
        return new RealOAuthTokenProviderFactory(K9.app, Preferences.getPreferences(K9.app));
    }

    // It'd be great if this actually went into the store implementation
    // to get this, but that's expensive and not easily accessible
    // during initialization
    public boolean isSearchByDateCapable() {
        return (getStoreUri().startsWith("imap"));
    }


    @NonNull
    @Override
    public synchronized String toString() {
        return description.getValue();
    }

    public synchronized void setCompression(NetworkType networkType, boolean useCompression) {
        compressionMap.put(networkType, useCompression);
    }

    public synchronized boolean useCompression(NetworkType networkType) {
        Boolean useCompression = compressionMap.get(networkType);
        if (useCompression == null) {
            return true;
        }

        return useCompression;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Account) {
            return ((Account)o).accountUuid.equals(accountUuid);
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return accountUuid.hashCode();
    }

    private synchronized List<Identity> loadIdentities(Storage storage) {
        List<Identity> newIdentities = new ArrayList<>();
        int ident = 0;
        boolean gotOne;
        do {
            gotOne = false;
            String name = storage.getString(accountUuid + "." + IDENTITY_NAME_KEY + "." + ident, null);
            String email = storage.getString(accountUuid + "." + IDENTITY_EMAIL_KEY + "." + ident, null);
            boolean signatureUse = storage.getBoolean(accountUuid + ".signatureUse." + ident, true);
            String signature = storage.getString(accountUuid + ".signature." + ident, null);
            String description = storage.getString(accountUuid + "." + IDENTITY_DESCRIPTION_KEY + "." + ident, null);
            final String replyTo = storage.getString(accountUuid + ".replyTo." + ident, null);
            if (email != null) {
                Identity identity = new Identity();
                identity.setName(name);
                identity.setEmail(email);
                identity.setSignatureUse(signatureUse);
                identity.setSignature(signature);
                identity.setDescription(description);
                identity.setReplyTo(replyTo);
                newIdentities.add(identity);
                gotOne = true;
            }
            ident++;
        } while (gotOne);

        if (newIdentities.isEmpty()) {
            String name = storage.getString(accountUuid + ".name", null);
            String email = storage.getString(accountUuid + ".email", null);
            boolean signatureUse = storage.getBoolean(accountUuid + ".signatureUse", true);
            String signature = storage.getString(accountUuid + ".signature", null);
            Identity identity = new Identity();
            identity.setName(name);
            identity.setEmail(email);
            identity.setSignatureUse(signatureUse);
            identity.setSignature(signature);
            identity.setDescription(email);
            newIdentities.add(identity);
        }

        return newIdentities;
    }

    private synchronized void deleteIdentities(Storage storage, StorageEditor editor) {
        int ident = 0;
        boolean gotOne;
        do {
            gotOne = false;
            String email = storage.getString(accountUuid + "." + IDENTITY_EMAIL_KEY + "." + ident, null);
            if (email != null) {
                editor.remove(accountUuid + "." + IDENTITY_NAME_KEY + "." + ident);
                editor.remove(accountUuid + "." + IDENTITY_EMAIL_KEY + "." + ident);
                editor.remove(accountUuid + ".signatureUse." + ident);
                editor.remove(accountUuid + ".signature." + ident);
                editor.remove(accountUuid + "." + IDENTITY_DESCRIPTION_KEY + "." + ident);
                editor.remove(accountUuid + ".replyTo." + ident);
                gotOne = true;
            }
            ident++;
        } while (gotOne);
    }

    private synchronized void saveIdentities(Storage storage, StorageEditor editor) {
        deleteIdentities(storage, editor);
        int ident = 0;

        for (Identity identity : identities) {
            editor.putString(accountUuid + "." + IDENTITY_NAME_KEY + "." + ident, identity.getName());
            editor.putString(accountUuid + "." + IDENTITY_EMAIL_KEY + "." + ident, identity.getEmail());
            editor.putBoolean(accountUuid + ".signatureUse." + ident, identity.getSignatureUse());
            editor.putString(accountUuid + ".signature." + ident, identity.getSignature());
            editor.putString(accountUuid + "." + IDENTITY_DESCRIPTION_KEY + "." + ident, identity.getDescription());
            editor.putString(accountUuid + ".replyTo." + ident, identity.getReplyTo());
            ident++;
        }
    }

    public synchronized List<Identity> getIdentities() {
        return identities;
    }

    public synchronized void setIdentities(List<Identity> newIdentities) {
        identities = new ArrayList<>(newIdentities);
    }

    public synchronized Identity getIdentity(int i) {
        if (i < identities.size()) {
            return identities.get(i);
        }
        throw new IllegalArgumentException("Identity with index " + i + " not found");
    }

    public boolean isAnIdentity(Address[] addrs) {
        if (addrs == null) {
            return false;
        }
        for (Address addr : addrs) {
            if (findIdentity(addr) != null) {
                return true;
            }
        }

        return false;
    }

    public boolean isAnIdentity(Address addr) {
        return findIdentity(addr) != null;
    }

    public synchronized Identity findIdentity(Address addr) {
        for (Identity identity : identities) {
            String email = identity.getEmail();
            if (email != null && email.equalsIgnoreCase(addr.getAddress())) {
                return identity;
            }
        }
        return null;
    }

    public synchronized Searchable getSearchableFolders() {
        return searchableFolders;
    }

    public synchronized void setSearchableFolders(Searchable searchableFolders) {
        this.searchableFolders = searchableFolders;
    }

    public synchronized int getIdleRefreshMinutes() {
        return idleRefreshMinutes;
    }

    public synchronized void setIdleRefreshMinutes(int idleRefreshMinutes) {
        this.idleRefreshMinutes = idleRefreshMinutes;
    }

    public synchronized boolean isPushPollOnConnect() {
        return pushPollOnConnect;
    }

    public synchronized void setPushPollOnConnect(boolean pushPollOnConnect) {
        this.pushPollOnConnect = pushPollOnConnect;
    }

    /**
     * Are we storing out localStore on the SD-card instead of the local device
     * memory?<br/>
     * Only to be called during initial account-setup!<br/>
     * Side-effect: changes {@link #localStorageProviderId}.
     *
     * @param newStorageProviderId
     *            Never <code>null</code>.
     * @throws MessagingException
     */
    private void switchLocalStorage(final String newStorageProviderId) throws MessagingException {
        if (!localStorageProviderId.equals(newStorageProviderId)) {
            getLocalStore().switchLocalStorage(newStorageProviderId);
        }
    }

    public synchronized boolean goToUnreadMessageSearch() {
        return goToUnreadMessageSearch;
    }

    public synchronized void setGoToUnreadMessageSearch(boolean goToUnreadMessageSearch) {
        this.goToUnreadMessageSearch = goToUnreadMessageSearch;
    }

    public synchronized boolean subscribedFoldersOnly() {
        return subscribedFoldersOnly;
    }

    public synchronized void setSubscribedFoldersOnly(boolean subscribedFoldersOnly) {
        this.subscribedFoldersOnly = subscribedFoldersOnly;
    }

    public synchronized int getMaximumPolledMessageAge() {
        return maximumPolledMessageAge;
    }

    public synchronized void setMaximumPolledMessageAge(int maximumPolledMessageAge) {
        this.maximumPolledMessageAge = maximumPolledMessageAge;
    }

    public synchronized int getMaximumAutoDownloadMessageSize() {
        //return 0;                                                       // pEp: always fetch everything...
        return maximumAutoDownloadMessageSize;
    }

    public synchronized void setMaximumAutoDownloadMessageSize(int maximumAutoDownloadMessageSize) {
        this.maximumAutoDownloadMessageSize = maximumAutoDownloadMessageSize;
    }

    public Date getEarliestPollDate() {
        int age = getMaximumPolledMessageAge();
        if (age >= 0) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY, 0);
            now.set(Calendar.MINUTE, 0);
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            if (age < 28) {
                now.add(Calendar.DATE, age * -1);
            } else switch (age) {
                case 28:
                    now.add(Calendar.MONTH, -1);
                    break;
                case 56:
                    now.add(Calendar.MONTH, -2);
                    break;
                case 84:
                    now.add(Calendar.MONTH, -3);
                    break;
                case 168:
                    now.add(Calendar.MONTH, -6);
                    break;
                case 365:
                    now.add(Calendar.YEAR, -1);
                    break;
                }

            return now.getTime();
        }

        return null;
    }

    public MessageFormat getMessageFormat() {
        return messageFormat;
    }

    public void setMessageFormat(MessageFormat messageFormat) {
        this.messageFormat = messageFormat;
    }

    public QuoteStyle getQuoteStyle() {
        return quoteStyle;
    }

    public void setQuoteStyle(QuoteStyle quoteStyle) {
        this.quoteStyle = quoteStyle;
    }

    public synchronized String getQuotePrefix() {
        return quotePrefix;
    }

    public synchronized void setQuotePrefix(String quotePrefix) {
        this.quotePrefix = quotePrefix;
    }

    public synchronized boolean isDefaultQuotedTextShown() {
        return defaultQuotedTextShown.getValue();
    }

    public synchronized void setDefaultQuotedTextShown(boolean shown) {
        defaultQuotedTextShown.setValue(shown);
    }

    public synchronized ManageableSetting<Boolean> getDefaultQuotedTextShown() {
        return defaultQuotedTextShown;
    }

    public synchronized void setDefaultQuotedTextShown(ManageableSetting<Boolean> shown) {
        defaultQuotedTextShown = shown;
    }

    public synchronized boolean isReplyAfterQuote() {
        return replyAfterQuote;
    }

    public synchronized void setReplyAfterQuote(boolean replyAfterQuote) {
        this.replyAfterQuote = replyAfterQuote;
    }

    public synchronized boolean isStripSignature() {
        return stripSignature;
    }

    public synchronized void setStripSignature(boolean stripSignature) {
        this.stripSignature = stripSignature;
    }


    @Nullable
    public String getOpenPgpProvider() {
        if (TextUtils.isEmpty(openPgpProvider)) {
            return null;
        }
        return openPgpProvider;
    }

    public void setOpenPgpProvider(String openPgpProvider) {
        this.openPgpProvider = "";
    }

    public long getOpenPgpKey() {
        return openPgpKey;
    }

    public boolean hasOpenPgpKey() {
        return false;
    }

    public boolean getOpenPgpHideSignOnly() {
        return openPgpHideSignOnly;
    }

    public void setOpenPgpHideSignOnly(boolean openPgpHideSignOnly) {
        this.openPgpHideSignOnly = openPgpHideSignOnly;
    }

    public boolean allowRemoteSearch() {
        return allowRemoteSearch.getValue();
    }

    public void setAllowRemoteSearch(boolean val) {
        allowRemoteSearch.setValue(val);
    }

    public ManageableSetting<Boolean> getAllowRemoteSearch() {
        return allowRemoteSearch;
    }

    public void setAllowRemoteSearch(ManageableSetting<Boolean> val) {
        allowRemoteSearch = val;
    }

    public int getRemoteSearchNumResults() {
        return remoteSearchNumResults.getValue();
    }

    public void setRemoteSearchNumResults(int val) {
        remoteSearchNumResults.setValue(Math.max(val, 0));
    }

    public ManageableSetting<Integer> getLockableRemoteSearchNumResults() {
        return remoteSearchNumResults;
    }

    public void setRemoteSearchNumResults(ManageableSetting<Integer> val) {
        remoteSearchNumResults = val;
        remoteSearchNumResults.setValue(Math.max(val.getValue(), 0));
    }

    public String getInboxFolderName() {
        return inboxFolderName;
    }

    public void setInboxFolderName(String name) {
        this.inboxFolderName = name;
    }

    public synchronized boolean syncRemoteDeletions() {
        return syncRemoteDeletions;
    }

    public synchronized void setSyncRemoteDeletions(boolean syncRemoteDeletions) {
        this.syncRemoteDeletions = syncRemoteDeletions;
    }

    public synchronized String getLastSelectedFolderName() {
        return lastSelectedFolderName;
    }

    public synchronized void setLastSelectedFolderName(String folderName) {
        lastSelectedFolderName = folderName;
    }

    public synchronized boolean isOpenPgpProviderConfigured() {
        return false;
    }

    public synchronized NotificationSetting getNotificationSetting() {
        return notificationSetting;
    }

    /**
     * @return <code>true</code> if our {@link StorageProvider} is ready. (e.g.
     *         card inserted)
     */
    public boolean isAvailable(Context context) {
        String localStorageProviderId = getLocalStorageProviderId();
        boolean storageProviderIsInternalMemory = localStorageProviderId == null;
        return storageProviderIsInternalMemory || StorageManager.getInstance(context).isReady(localStorageProviderId);
    }

    public synchronized boolean isEnabled() {
        return isEnabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public synchronized boolean isMarkMessageAsReadOnView() {
        return markMessageAsReadOnView;
    }

    public synchronized void setMarkMessageAsReadOnView(boolean value) {
        markMessageAsReadOnView = value;
    }

    public synchronized boolean isAlwaysShowCcBcc() {
        return alwaysShowCcBcc;
    }

    public synchronized void setAlwaysShowCcBcc(boolean show) {
        alwaysShowCcBcc = show;
    }
    public boolean isRemoteSearchFullText() {
        return false;   // Temporarily disabled
        //return remoteSearchFullText;
    }

    public void setRemoteSearchFullText(boolean val) {
        remoteSearchFullText = val;
    }

    /**
     * Modify the supplied {@link LocalSearch} instance to limit the search to displayable folders.
     *
     * <p>
     * This method uses the current folder display mode to decide what folders to include/exclude.
     * </p>
     *
     * @param search
     *         The {@code LocalSearch} instance to modify.
     *
     * @see #getFolderDisplayMode()
     */
    public void limitToDisplayableFolders(LocalSearch search) {
        final Account.FolderMode displayMode = getFolderDisplayMode();

        switch (displayMode) {
            case FIRST_CLASS: {
                // Count messages in the INBOX and non-special first class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name(),
                        Attribute.EQUALS);
                break;
            }
            case FIRST_AND_SECOND_CLASS: {
                // Count messages in the INBOX and non-special first and second class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.FIRST_CLASS.name(),
                        Attribute.EQUALS);

                // TODO: Create a proper interface for creating arbitrary condition trees
                SearchCondition searchCondition = new SearchCondition(SearchField.DISPLAY_CLASS,
                        Attribute.EQUALS, FolderClass.SECOND_CLASS.name());
                ConditionsTreeNode root = search.getConditions();
                if (root.mRight != null) {
                    root.mRight.or(searchCondition);
                } else {
                    search.or(searchCondition);
                }
                break;
            }
            case NOT_SECOND_CLASS: {
                // Count messages in the INBOX and non-special non-second-class folders
                search.and(SearchField.DISPLAY_CLASS, FolderClass.SECOND_CLASS.name(),
                        Attribute.NOT_EQUALS);
                break;
            }
            default:
            case ALL: {
                // Count messages in the INBOX and non-special folders
                break;
            }
        }
    }

    /**
     * Modify the supplied {@link LocalSearch} instance to exclude special folders.
     *
     * <p>
     * Currently the following folders are excluded:
     * <ul>
     *   <li>Trash</li>
     *   <li>Drafts</li>
     *   <li>Spam</li>
     *   <li>Outbox</li>
     *   <li>Sent</li>
     * </ul>
     * The Inbox will always be included even if one of the special folders is configured to point
     * to the Inbox.
     * </p>
     *
     * @param search
     *         The {@code LocalSearch} instance to modify.
     */
    public void excludeSpecialFolders(LocalSearch search) {
        excludeSpecialFolder(search, getTrashFolderName());
        excludeSpecialFolder(search, getDraftsFolderName());
        excludeSpecialFolder(search, getSpamFolderName());
        excludeSpecialFolder(search, getOutboxFolderName());
        excludeSpecialFolder(search, getSentFolderName());
        search.or(new SearchCondition(SearchField.FOLDER, Attribute.EQUALS, getInboxFolderName()));
    }

    /**
     * Modify the supplied {@link LocalSearch} instance to exclude "unwanted" folders.
     *
     * <p>
     * Currently the following folders are excluded:
     * <ul>
     *   <li>Trash</li>
     *   <li>Spam</li>
     *   <li>Outbox</li>
     * </ul>
     * The Inbox will always be included even if one of the special folders is configured to point
     * to the Inbox.
     * </p>
     *
     * @param search
     *         The {@code LocalSearch} instance to modify.
     */
    public void excludeUnwantedFolders(LocalSearch search) {
        excludeSpecialFolder(search, getTrashFolderName());
        excludeSpecialFolder(search, getSpamFolderName());
        excludeSpecialFolder(search, getOutboxFolderName());
        search.or(new SearchCondition(SearchField.FOLDER, Attribute.EQUALS, getInboxFolderName()));
    }

    private void excludeSpecialFolder(LocalSearch search, String folderName) {
        if (folderName != null && !K9.FOLDER_NONE.equals(folderName)) {
            search.and(SearchField.FOLDER, folderName, Attribute.NOT_EQUALS);
        }
    }

    /**
     * Add a new certificate for the incoming or outgoing server to the local key store.
     */
    public void addCertificate(CheckDirection direction, X509Certificate certificate) throws CertificateException {
        Uri uri;
        if (direction == CheckDirection.INCOMING) {
            uri = Uri.parse(getStoreUri());
        } else {
            uri = Uri.parse(getTransportUri());
        }
        LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
        localKeyStore.addCertificate(uri.getHost(), uri.getPort(), certificate);
    }

    /**
     * Examine the existing settings for an account.  If the old host/port is different from the
     * new host/port, then try and delete any (possibly non-existent) certificate stored for the
     * old host/port.
     */
    public void deleteCertificate(String newHost, int newPort, CheckDirection direction) {
        Uri uri;
        if (direction == CheckDirection.INCOMING) {
            uri = Uri.parse(getStoreUri());
        } else {
            uri = Uri.parse(getTransportUri());
        }
        String oldHost = uri.getHost();
        int oldPort = uri.getPort();
        if (oldPort == -1) {
            // This occurs when a new account is created
            return;
        }
        if (!newHost.equals(oldHost) || newPort != oldPort) {
            LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
            localKeyStore.deleteCertificate(oldHost, oldPort);
        }
    }

    /**
     * Examine the settings for the account and attempt to delete (possibly non-existent)
     * certificates for the incoming and outgoing servers.
     */
    private void deleteCertificates() {
        LocalKeyStore localKeyStore = LocalKeyStore.getInstance();

        String storeUri = getStoreUri();
        if (storeUri != null) {
            Uri uri = Uri.parse(storeUri);
            localKeyStore.deleteCertificate(uri.getHost(), uri.getPort());
        }
        String transportUri = getTransportUri();
        if (transportUri != null) {
            Uri uri = Uri.parse(transportUri);
            localKeyStore.deleteCertificate(uri.getHost(), uri.getPort());
        }
    }
}
