package security.planck.mdm

const val RESTRICTION_PROVISIONING_URL = "planck_user_provisioning_url"
const val RESTRICTION_PLANCK_EXTRA_KEYS = "planck_extra_keys"
const val RESTRICTION_PLANCK_EXTRA_KEY = "planck_extra_key"
const val RESTRICTION_PLANCK_EXTRA_KEY_FINGERPRINT = "extra_key_fingerprint"
const val RESTRICTION_PLANCK_EXTRA_KEY_MATERIAL = "extra_key_material"
const val RESTRICTION_PLANCK_MEDIA_KEYS = "planck_media_keys"
const val RESTRICTION_PLANCK_MEDIA_KEY = "planck_media_key"
const val RESTRICTION_PLANCK_MEDIA_KEY_ADDRESS_PATTERN = "media_key_address_pattern"
const val RESTRICTION_PLANCK_MEDIA_KEY_FINGERPRINT = "media_key_fingerprint"
const val RESTRICTION_PLANCK_MEDIA_KEY_MATERIAL = "media_key_material"
const val RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING = "unsecure_delivery_warning"
const val RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_VALUE = "unsecure_delivery_warning_value"
const val RESTRICTION_PLANCK_UNSECURE_DELIVERY_WARNING_LOCKED = "unsecure_delivery_warning_locked"
const val RESTRICTION_PLANCK_SYNC_FOLDER = "planck_sync_folder"
const val RESTRICTION_ENABLE_PLANCK_SYNC = "enable_planck_sync"
const val RESTRICTION_ENABLE_PLANCK_SYNC_VALUE = "enable_planck_sync_value"
const val RESTRICTION_ENABLE_PLANCK_SYNC_LOCKED = "enable_planck_sync_locked"
const val RESTRICTION_PLANCK_DEBUG_LOG = "debug_logging"
const val RESTRICTION_PLANCK_DEBUG_LOG_VALUE = "debug_logging_value"
const val RESTRICTION_PLANCK_DEBUG_LOG_LOCKED = "debug_logging_locked"
const val RESTRICTION_ENABLE_ECHO_PROTOCOL = "planck_enable_echo_protocol"
const val RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION = "audit_log_data_time_retention"
const val RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_VALUE = "audit_log_data_time_retention_value"
const val RESTRICTION_AUDIT_LOG_DATA_TIME_RETENTION_LOCKED = "audit_log_data_time_retention_locked"

const val RESTRICTION_PLANCK_ACCOUNTS_SETTINGS = "planck_accounts_settings"
const val RESTRICTION_PLANCK_ACCOUNT_SETTINGS = "planck_account_settings"
const val RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION = "planck_enable_privacy_protection"
const val RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_VALUE = "planck_enable_privacy_protection_value"
const val RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION_LOCKED = "planck_enable_privacy_protection_locked"
const val RESTRICTION_ACCOUNT_DESCRIPTION = "account_description"
const val RESTRICTION_ACCOUNT_DESCRIPTION_VALUE = "account_description_value"
const val RESTRICTION_ACCOUNT_DESCRIPTION_LOCKED = "account_description_locked"
const val RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE = "account_display_count"
const val RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_VALUE = "account_display_count_value"
const val RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE_LOCKED = "account_display_count_locked"
const val RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS = "max_push_folders"
const val RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS = "composition_settings"
const val RESTRICTION_ACCOUNT_COMPOSITION_SENDER_NAME = "composition_sender_name"
const val RESTRICTION_ACCOUNT_COMPOSITION_USE_SIGNATURE = "composition_use_signature"
const val RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE = "composition_signature"
const val RESTRICTION_ACCOUNT_COMPOSITION_SIGNATURE_BEFORE_QUOTED_MESSAGE =
    "composition_signature_before_quoted_message"
const val RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY = "default_quoted_text_shown"
const val RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY_VALUE = "default_quoted_text_shown_value"
const val RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY_LOCKED = "default_quoted_text_shown_locked"
const val RESTRICTION_ACCOUNT_DEFAULT_FOLDERS = "account_default_folders"
const val RESTRICTION_ACCOUNT_ARCHIVE_FOLDER = "archive_folder"
const val RESTRICTION_ACCOUNT_DRAFTS_FOLDER = "drafts_folder"
const val RESTRICTION_ACCOUNT_SENT_FOLDER = "sent_folder"
const val RESTRICTION_ACCOUNT_SPAM_FOLDER = "spam_folder"
const val RESTRICTION_ACCOUNT_TRASH_FOLDER = "trash_folder"
const val RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH = "remote_search_enabled"
const val RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH_VALUE = "remote_search_enabled_value"
const val RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH_LOCKED = "remote_search_enabled_locked"
const val RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT = "account_remote_search_num_results"
const val RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT_VALUE = "account_remote_search_num_results_value"
const val RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT_LOCKED = "account_remote_search_num_results_locked"
const val RESTRICTION_ACCOUNT_ENABLE_SYNC = "planck_enable_sync_account"
const val RESTRICTION_ACCOUNT_ENABLE_SYNC_VALUE = "planck_enable_sync_account_value"
const val RESTRICTION_ACCOUNT_ENABLE_SYNC_LOCKED = "planck_enable_sync_account_locked"

const val RESTRICTION_ACCOUNT_MAIL_SETTINGS = "planck_mail_settings"
const val RESTRICTION_ACCOUNT_EMAIL_ADDRESS = "account_email_address"
const val RESTRICTION_ACCOUNT_OAUTH_PROVIDER = "account_oauth_provider"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS = "incoming_mail_settings"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SERVER = "incoming_mail_settings_server"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_SECURITY_TYPE =
    "incoming_mail_settings_security_type"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_PORT = "incoming_mail_settings_port"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_USER_NAME = "incoming_mail_settings_user_name"
const val RESTRICTION_ACCOUNT_INCOMING_MAIL_SETTINGS_AUTH_TYPE = "incoming_mail_settings_auth_type"

const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS = "outgoing_mail_settings"
const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SERVER = "outgoing_mail_settings_server"
const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_SECURITY_TYPE =
    "outgoing_mail_settings_security_type"
const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_PORT = "outgoing_mail_settings_port"
const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_USER_NAME = "outgoing_mail_settings_user_name"
const val RESTRICTION_ACCOUNT_OUTGOING_MAIL_SETTINGS_AUTH_TYPE = "outgoing_mail_settings_auth_type"

val INITIALIZED_ENGINE_RESTRICTIONS = listOf(
    RESTRICTION_PLANCK_EXTRA_KEYS,
    RESTRICTION_PLANCK_MEDIA_KEYS,
)

val PROVISIONING_RESTRICTIONS = listOf(
    RESTRICTION_PLANCK_ACCOUNTS_SETTINGS,
)

val ACCOUNT_PROVISIONING_RESTRICTIONS = listOf(
    RESTRICTION_ACCOUNT_MAIL_SETTINGS,
    RESTRICTION_ACCOUNT_DESCRIPTION,
)

val ALL_ACCOUNT_RESTRICTIONS = listOf(
    RESTRICTION_ACCOUNT_DESCRIPTION,
    RESTRICTION_PLANCK_ENABLE_PRIVACY_PROTECTION,
    RESTRICTION_ACCOUNT_LOCAL_FOLDER_SIZE,
    RESTRICTION_ACCOUNT_MAX_PUSH_FOLDERS,
    RESTRICTION_ACCOUNT_COMPOSITION_DEFAULTS,
    RESTRICTION_ACCOUNT_QUOTE_MESSAGES_REPLY,
    RESTRICTION_ACCOUNT_DEFAULT_FOLDERS,
    RESTRICTION_ACCOUNT_ENABLE_SERVER_SEARCH,
    RESTRICTION_ACCOUNT_SERVER_SEARCH_LIMIT,
    RESTRICTION_ACCOUNT_ENABLE_SYNC,
    RESTRICTION_ACCOUNT_MAIL_SETTINGS,
)
