package com.fsck.k9.preferences;


import android.os.Environment;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SortType;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.K9.NotificationHideSubject;
import com.fsck.k9.K9.NotificationQuickDelete;
import com.fsck.k9.K9.SplitViewMode;
import com.fsck.k9.planck.ui.tools.AppTheme;
import com.fsck.k9.planck.ui.tools.Theme;
import com.fsck.k9.R;
import com.fsck.k9.preferences.Settings.BooleanSetting;
import com.fsck.k9.preferences.Settings.ColorSetting;
import com.fsck.k9.preferences.Settings.EnumSetting;
import com.fsck.k9.preferences.Settings.FontSizeSetting;
import com.fsck.k9.preferences.Settings.IntegerRangeSetting;
import com.fsck.k9.preferences.Settings.InvalidSettingValueException;
import com.fsck.k9.preferences.Settings.PseudoEnumSetting;
import com.fsck.k9.preferences.Settings.SettingsDescription;
import com.fsck.k9.preferences.Settings.SettingsUpgrader;
import com.fsck.k9.preferences.Settings.StringSetting;
import com.fsck.k9.preferences.Settings.V;
import com.fsck.k9.preferences.Settings.WebFontSizeSetting;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.fsck.k9.K9.LockScreenNotificationVisibility;

import security.planck.mdm.ManageableSetting;
import security.planck.mdm.ManageableSettingKt;


public class GlobalSettings {
    static final Map<String, TreeMap<Integer, SettingsDescription>> SETTINGS;
    private static final Map<Integer, SettingsUpgrader> UPGRADERS;

    static {
        Map<String, TreeMap<Integer, SettingsDescription>> s = new LinkedHashMap<>();

        /*
         * When adding new settings here, be sure to increment {@link Settings.VERSION}
         * and use that for whatever you add here.
         */

        s.put("animations", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("attachmentdefaultpath", Settings.versions(
                new V(1, new DirectorySetting(Environment.getExternalStorageDirectory())),
                new V(41, new DirectorySetting(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS)))
        ));
        s.put("backgroundOperations", Settings.versions(
                new V(1, new EnumSetting<>(K9.BACKGROUND_OPS.class, K9.BACKGROUND_OPS.WHEN_CHECKED_AUTO_SYNC))
        ));
        s.put("changeRegisteredNameColor", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("confirmDelete", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("confirmDeleteStarred", Settings.versions(
                new V(2, new BooleanSetting(false))
        ));
        s.put("confirmSpam", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("confirmMarkAllRead", Settings.versions(
                new V(48, new BooleanSetting(true))
        ));
        s.put("countSearchMessages", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("enableDebugLogging", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("enableSensitiveLogging", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("fontSizeAccountDescription", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeAccountName", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeFolderName", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeFolderStatus", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageComposeInput", Settings.versions(
                new V(5, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListDate", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListPreview", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListSender", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageListSubject", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewAdditionalHeaders", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewCC", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewContent", Settings.versions(
                new V(1, new WebFontSizeSetting(3)),
                new V(31, null)
        ));
        s.put("fontSizeMessageViewDate", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewSender", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewSubject", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewTime", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("fontSizeMessageViewTo", Settings.versions(
                new V(1, new FontSizeSetting(FontSizes.FONT_DEFAULT))
        ));
        s.put("gesturesEnabled", Settings.versions(
                new V(1, new BooleanSetting(true)),
                new V(4, new BooleanSetting(false))
        ));
        s.put("hideSpecialAccounts", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("keyguardPrivacy", Settings.versions(
                new V(1, new BooleanSetting(false)),
                new V(12, null)
        ));
        s.put("language", Settings.versions(
                new V(1, new LanguageSetting())
        ));
        s.put("measureAccounts", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("messageListCheckboxes", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("messageListPreviewLines", Settings.versions(
                new V(1, new IntegerRangeSetting(1, 100, 2))
        ));
        s.put("messageListStars", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("messageViewFixedWidthFont", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("messageViewReturnToList", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("messageViewShowNext", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("quietTimeEnabled", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("quietTimeEnds", Settings.versions(
                new V(1, new TimeSetting("7:00"))
        ));
        s.put("quietTimeStarts", Settings.versions(
                new V(1, new TimeSetting("21:00"))
        ));
        s.put("registeredNameColor", Settings.versions(
                new V(1, new ColorSetting(K9.DEFAULT_CONTACT_NAME_COLOR))
        ));
        s.put("showContactName", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("showCorrespondentNames", Settings.versions(
                new V(1, new BooleanSetting(true))
        ));
        s.put("sortTypeEnum", Settings.versions(
                new V(10, new EnumSetting<>(SortType.class, Account.DEFAULT_SORT_TYPE))
        ));
        s.put("sortAscending", Settings.versions(
                new V(10, new BooleanSetting(Account.DEFAULT_SORT_ASCENDING))
        ));
        s.put("startIntegratedInbox", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("theme", Settings.versions(
                new V(1, new LegacyThemeSetting(Theme.LIGHT)),
                new V(52, new ThemeSetting(AppTheme.FOLLOW_SYSTEM))
        ));
        s.put("messageViewTheme", Settings.versions(
                new V(16, new LegacyThemeSetting(Theme.LIGHT)),
                new V(24, new SubThemeSetting(Theme.USE_GLOBAL))
        ));
        s.put("useVolumeKeysForListNavigation", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("useVolumeKeysForNavigation", Settings.versions(
                new V(1, new BooleanSetting(false))
        ));
        s.put("wrapFolderNames", Settings.versions(
                new V(22, new BooleanSetting(false))
        ));
        s.put("notificationHideSubject", Settings.versions(
                new V(12, new EnumSetting<>(NotificationHideSubject.class, NotificationHideSubject.NEVER))
        ));
        s.put("useBackgroundAsUnreadIndicator", Settings.versions(
                new V(19, new BooleanSetting(true))
        ));
        s.put("threadedView", Settings.versions(
                new V(20, new BooleanSetting(true))
        ));
        s.put("splitViewMode", Settings.versions(
                new V(23, new EnumSetting<>(SplitViewMode.class, SplitViewMode.NEVER))
        ));
        s.put("messageComposeTheme", Settings.versions(
                new V(24, new SubThemeSetting(Theme.USE_GLOBAL))
        ));
        s.put("fixedMessageViewTheme", Settings.versions(
                new V(24, new BooleanSetting(true))
        ));
        s.put("showContactPicture", Settings.versions(
                new V(25, new BooleanSetting(true))
        ));
        s.put("autofitWidth", Settings.versions(
                new V(28, new BooleanSetting(true))
        ));
        s.put("colorizeMissingContactPictures", Settings.versions(
                new V(29, new BooleanSetting(K9.DEFAULT_COLORIZE_MISSING_CONTACT_PICTURE))
        ));
        s.put("messageViewDeleteActionVisible", Settings.versions(
                new V(30, new BooleanSetting(true))
        ));
        s.put("messageViewArchiveActionVisible", Settings.versions(
                new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewMoveActionVisible", Settings.versions(
                new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewCopyActionVisible", Settings.versions(
                new V(30, new BooleanSetting(false))
        ));
        s.put("messageViewSpamActionVisible", Settings.versions(
                new V(30, new BooleanSetting(false))
        ));
        s.put("fontSizeMessageViewContentPercent", Settings.versions(
                new V(31, new IntegerRangeSetting(40, 250, 100))
        ));
        s.put("hideUserAgent", Settings.versions(
                new V(32, new BooleanSetting(true))
        ));
        s.put("hideTimeZone", Settings.versions(
                new V(32, new BooleanSetting(false))
        ));
        s.put("lockScreenNotificationVisibility", Settings.versions(
                new V(37, new EnumSetting<>(LockScreenNotificationVisibility.class,
                        LockScreenNotificationVisibility.MESSAGE_COUNT))
        ));
        s.put("confirmDeleteFromNotification", Settings.versions(
                new V(38, new BooleanSetting(true))
        ));
        s.put("messageListSenderAboveSubject", Settings.versions(
                new V(38, new BooleanSetting(true))
        ));
        s.put("notificationQuickDelete", Settings.versions(
                new V(38, new EnumSetting<>(NotificationQuickDelete.class, NotificationQuickDelete.NEVER))
        ));
        s.put("notificationDuringQuietTimeEnabled", Settings.versions(
                new V(39, new BooleanSetting(true))
        ));
        s.put("confirmDiscardMessage", Settings.versions(
                new V(40, new BooleanSetting(true))
        ));
        s.put("pEpExtraAccounts", Settings.versions(
                new V(42, new StringSetting(""))
        ));
        s.put("pEpUseKeyserver", Settings.versions(
                new V(39, new BooleanSetting(true))
        ));
        s.put("pEpPassiveMode", Settings.versions(
                new V(43, new BooleanSetting(true))
        ));
        s.put("pEpForwardWarningEnabled", Settings.versions(
                new V(
                        47,
                        new BooleanSetting(
                                true
                        )
                )
        ));
        s.put("pEpEnableSync", Settings.versions(
                new V(49, new BooleanSetting(true))
        ));

        s.put("pEpSubjectProtection", Settings.versions(
                new V(50, new BooleanSetting(true))
        ));
        s.put("pEpSyncFolder", Settings.versions(
                new V(51, new BooleanSetting(true))
        ));

        SETTINGS = Collections.unmodifiableMap(s);

        Map<Integer, SettingsUpgrader> u = new HashMap<>();
        u.put(12, new SettingsUpgraderV12());
        u.put(24, new SettingsUpgraderV24());
        u.put(31, new SettingsUpgraderV31());
        u.put(50, new SettingsUpgraderV50());
        u.put(52, new SettingsUpgraderV52());

        UPGRADERS = Collections.unmodifiableMap(u);
    }

    static Map<String, Object> validate(int version, Map<String, String> importedSettings) {
        return Settings.validate(version, SETTINGS, importedSettings, false);
    }

    public static Set<String> upgrade(int version, Map<String, Object> validatedSettings) {
        return Settings.upgrade(version, UPGRADERS, SETTINGS, validatedSettings);
    }

    public static Map<String, String> convert(Map<String, Object> settings) {
        return Settings.convert(settings, SETTINGS);
    }

    static Map<String, String> getGlobalSettings(Storage storage) {
        Map<String, String> result = new HashMap<>();
        for (String key : SETTINGS.keySet()) {
            String value = storage.getString(key, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * Upgrades the settings from version 11 to 12
     *
     * Map the 'keyguardPrivacy' value to the new NotificationHideSubject enum.
     */
    private static class SettingsUpgraderV12 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            Boolean keyguardPrivacy = (Boolean) settings.get("keyguardPrivacy");
            if (keyguardPrivacy != null && keyguardPrivacy) {
                // current setting: only show subject when unlocked
                settings.put("notificationHideSubject", NotificationHideSubject.WHEN_LOCKED);
            } else {
                // always show subject [old default]
                settings.put("notificationHideSubject", NotificationHideSubject.NEVER);
            }
            return new HashSet<>(Collections.singletonList("keyguardPrivacy"));
        }
    }

    /**
     * Upgrades the settings from version 23 to 24.
     *
     * <p>
     * Set <em>messageViewTheme</em> to {@link Theme#USE_GLOBAL} if <em>messageViewTheme</em> has
     * the same value as <em>theme</em>.
     * </p>
     */
    private static class SettingsUpgraderV24 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            Theme messageViewTheme = (Theme) settings.get("messageViewTheme");
            Theme theme = (Theme) settings.get("theme");
            if (theme != null && messageViewTheme != null && theme == messageViewTheme) {
                settings.put("messageViewTheme", Theme.USE_GLOBAL);
            }

            return null;
        }
    }

    /**
     * Upgrades the settings from version 30 to 31.
     *
     * <p>
     * Convert value from <em>fontSizeMessageViewContent</em> to
     * <em>fontSizeMessageViewContentPercent</em>.
     * </p>
     */
    public static class SettingsUpgraderV31 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            int oldSize = (Integer) settings.get("fontSizeMessageViewContent");

            int newSize = convertFromOldSize(oldSize);

            settings.put("fontSizeMessageViewContentPercent", newSize);

            return new HashSet<>(Collections.singletonList("fontSizeMessageViewContent"));
        }

        public static int convertFromOldSize(int oldSize) {
            switch (oldSize) {
                case 1: {
                    return 40;
                }
                case 2: {
                    return 75;
                }
                case 4: {
                    return 175;
                }
                case 5: {
                    return 250;
                }
                case 3:
                default: {
                    return 100;
                }
            }
        }
    }

    /**
     * Upgrades the settings from version 49 to 50.
     *
     * <p>
     * Convert value from <em>pEpSubjectUnprotected</em> to
     * <em>pEpSubjectProtection</em>.
     * </p>
     */
    public static class SettingsUpgraderV50 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            Boolean oldValue = settings.get("pEpSubjectUnprotected") != null
                    ? (Boolean) settings.get("pEpSubjectUnprotected")
                    : null;

            if(oldValue != null) {
                settings.put("pEpSubjectProtection", !oldValue);
                return new HashSet<>(Collections.singletonList("pEpSubjectUnprotected"));
            }
            else {
                return null;
            }
        }

    }

    /**
     * Upgrades the settings from version 51 to 52.
     *
     * <p>
     * Set <em>theme</em> to {@link AppTheme#FOLLOW_SYSTEM} if <em>theme</em> has the value {@link AppTheme#LIGHT}.
     * </p>
     */
    private static class SettingsUpgraderV52 implements SettingsUpgrader {

        @Override
        public Set<String> upgrade(Map<String, Object> settings) {
            Theme oldTheme = (Theme) settings.get("theme");
            settings.put("theme", convertTheme(oldTheme));
            return null;
        }

        private AppTheme convertTheme(Theme oldTheme) {
            switch (oldTheme) {
                case DARK: return AppTheme.DARK;
                case LIGHT: return AppTheme.LIGHT;
                default: return AppTheme.FOLLOW_SYSTEM;
            }
        }
    }

    private static class LanguageSetting extends PseudoEnumSetting<String> {
        private final Map<String, String> mapping;

        LanguageSetting() {
            super("");

            Map<String, String> mapping = new HashMap<>();
            String[] values = K9.app.getResources().getStringArray(R.array.language_values);
            for (String value : values) {
                if (value.length() == 0) {
                    mapping.put("", "default");
                } else {
                    mapping.put(value, value);
                }
            }
            this.mapping = Collections.unmodifiableMap(mapping);
        }

        @Override
        protected Map<String, String> getMapping() {
            return mapping;
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (mapping.containsKey(value)) {
                return value;
            }

            throw new InvalidSettingValueException();
        }
    }

    static class ThemeSetting extends SettingsDescription<AppTheme> {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";
        private static final String THEME_FOLLOW_SYSTEM = "follow_system";

        ThemeSetting(AppTheme defaultValue) {
            super(defaultValue);
        }

        @Override
        public AppTheme fromString(String value) throws InvalidSettingValueException {
            try {
                Integer theme = Integer.parseInt(value);
                if (theme == AppTheme.LIGHT.ordinal() ||
                        // We used to store the resource ID of the theme in the preference storage,
                        // but don't use the database upgrade mechanism to update the values. So
                        // we have to deal with the old format here.
                        theme == android.R.style.Theme_Light) {
                    return AppTheme.LIGHT;
                } else if (theme == AppTheme.DARK.ordinal() || theme == android.R.style.Theme) {
                    return AppTheme.DARK;
                }
                else {
                    return AppTheme.FOLLOW_SYSTEM;
                }
            } catch (NumberFormatException e) { throw new InvalidSettingValueException(); }
        }

        @Override
        public AppTheme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return AppTheme.LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return AppTheme.DARK;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(AppTheme value) {
            switch (value) {
                case DARK: {
                    return THEME_DARK;
                }
                case LIGHT: {
                    return THEME_LIGHT;
                }
                default: {
                    return THEME_FOLLOW_SYSTEM;
                }
            }
        }

        @Override
        public String toString(AppTheme value) {
            return Integer.toString(value.ordinal());
        }
    }

    static class LegacyThemeSetting extends SettingsDescription<Theme> {
        private static final String THEME_LIGHT = "light";
        private static final String THEME_DARK = "dark";

        LegacyThemeSetting(Theme defaultValue) {
            super(defaultValue);
        }

        @Override
        public Theme fromString(String value) throws InvalidSettingValueException {
            try {
                Integer theme = Integer.parseInt(value);
                if (theme == Theme.LIGHT.ordinal() ||
                        // We used to store the resource ID of the theme in the preference storage,
                        // but don't use the database upgrade mechanism to update the values. So
                        // we have to deal with the old format here.
                        theme == android.R.style.Theme_Light) {
                    return Theme.LIGHT;
                } else if (theme == Theme.DARK.ordinal() || theme == android.R.style.Theme) {
                    return Theme.DARK;
                }
            } catch (NumberFormatException e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }

        @Override
        public Theme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_LIGHT.equals(value)) {
                return Theme.LIGHT;
            } else if (THEME_DARK.equals(value)) {
                return Theme.DARK;
            }

            throw new InvalidSettingValueException();
        }

        @Override
        public String toPrettyString(Theme value) {
            switch (value) {
                case DARK: {
                    return THEME_DARK;
                }
                default: {
                    return THEME_LIGHT;
                }
            }
        }

        @Override
        public String toString(Theme value) {
            return Integer.toString(value.ordinal());
        }
    }

    private static class SubThemeSetting extends LegacyThemeSetting {
        private static final String THEME_USE_GLOBAL = "use_global";

        SubThemeSetting(Theme defaultValue) {
            super(defaultValue);
        }

        @Override
        public Theme fromString(String value) throws InvalidSettingValueException {
            try {
                Integer theme = Integer.parseInt(value);
                if (theme == Theme.USE_GLOBAL.ordinal()) {
                    return Theme.USE_GLOBAL;
                }

                return super.fromString(value);
            } catch (NumberFormatException e) {
                throw new InvalidSettingValueException();
            }
        }

        @Override
        public Theme fromPrettyString(String value) throws InvalidSettingValueException {
            if (THEME_USE_GLOBAL.equals(value)) {
                return Theme.USE_GLOBAL;
            }

            return super.fromPrettyString(value);
        }

        @Override
        public String toPrettyString(Theme value) {
            if (value == Theme.USE_GLOBAL) {
                return THEME_USE_GLOBAL;
            }

            return super.toPrettyString(value);
        }
    }

    private static class TimeSetting extends SettingsDescription<String> {
        private static final String VALIDATION_EXPRESSION = "[0-2]*[0-9]:[0-5]*[0-9]";

        TimeSetting(String defaultValue) {
            super(defaultValue);
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            if (!value.matches(VALIDATION_EXPRESSION)) {
                throw new InvalidSettingValueException();
            }
            return value;
        }
    }

    private static class DirectorySetting extends SettingsDescription<String> {
        DirectorySetting(File defaultPath) {
            super(defaultPath.toString());
        }

        @Override
        public String fromString(String value) throws InvalidSettingValueException {
            try {
                if (new File(value).isDirectory()) {
                    return value;
                }
            } catch (Exception e) { /* do nothing */ }

            throw new InvalidSettingValueException();
        }
    }
}
