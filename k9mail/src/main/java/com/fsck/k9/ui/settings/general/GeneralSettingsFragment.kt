package com.fsck.k9.ui.settings.general

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.*
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.helper.FileBrowserHelper
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.filepicker.Utils
import com.fsck.k9.pEp.ui.keys.PepExtraKeys
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.pEp.ui.tools.ThemeManager
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.preference_loading_widget.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import security.pEp.permissions.PermissionChecker
import security.pEp.permissions.PermissionRequester
import security.pEp.ui.passphrase.*
import security.pEp.ui.support.export.ExportpEpSupportDataActivity
import java.io.File

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()
    private val fileBrowserHelper: FileBrowserHelper by inject()
    private val permissionChecker: PermissionChecker by inject()
    private val permissionRequester: PermissionRequester by inject {
        mapOf("activity" to requireActivity())
    }

    private lateinit var attachmentDefaultPathPreference: Preference

    private var syncSwitchDialog: AlertDialog? = null

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore

        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializeAttachmentDefaultPathPreference()
        initializeConfirmActions()
        initializeLockScreenNotificationVisibility()
        initializeNotificationQuickDelete()
        initializeExtraKeysManagement()
        initializeGlobalpEpKeyReset()
        initializeAfterMessageDeleteBehavior()
        initializeGlobalpEpSync()
        initializeExportPEpSupportDataPreference()
        initializeNewKeysPassphrase()
        initializeTheme()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
        dataStore.activity = activity
    }

    private fun initializeTheme() {
        (findPreference(PREFERENCE_THEME) as? ListPreference)?.apply {
            if (Build.VERSION.SDK_INT < 28) {
                setEntries(R.array.theme_entries_legacy)
                setEntryValues(R.array.theme_values_legacy)
            }
        }
    }

    private fun initializeNewKeysPassphrase() {
        findPreference<SwitchPreferenceCompat>(PEP_USE_PASSPHRASE_FOR_NEW_KEYS)?.apply {
            this.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                    processNewKeysSwitchClick(preference, newValue)
                }
        }
    }

    private fun processNewKeysSwitchClick(preference: Preference, newValue: Any): Boolean {
        if (preference is SwitchPreferenceCompat && newValue is Boolean) {
            if (!newValue) {
                preference.isChecked = false
            } else {
                requestPassphraseForNewKeys()
            }
        }
        return false
    }

    private fun initializeAttachmentDefaultPathPreference() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findPreference<Preference>(PREFERENCE_ATTACHMENT_DEFAULT_PATH)?.remove()
        } else {
            findPreference<Preference>(PREFERENCE_ATTACHMENT_DEFAULT_PATH)?.apply {
                attachmentDefaultPathPreference = this

                summary = attachmentDefaultPath()
                onClick {
                    fileBrowserHelper.showFileBrowserActivity(this@GeneralSettingsFragment,
                        File(attachmentDefaultPath()), REQUEST_PICK_DIRECTORY,
                        object : FileBrowserHelper.FileBrowserFailOverCallback {
                            override fun onPathEntered(path: String) {
                                setAttachmentDefaultPath(path)
                            }

                            override fun onCancel() = Unit
                        }
                    )
                }
            }
        }
    }

    private fun initializeConfirmActions() {
        val notificationActionsSupported = NotificationController.platformSupportsExtendedNotifications()
        if (!notificationActionsSupported) {
            (findPreference(PREFERENCE_CONFIRM_ACTIONS) as? MultiSelectListPreference)?.apply {
                removeEntry(CONFIRM_ACTION_DELETE_FROM_NOTIFICATION)
            }
        }
    }

    private fun initializeLockScreenNotificationVisibility() {
        val lockScreenNotificationsSupported = NotificationController.platformSupportsLockScreenNotifications()
        if (!lockScreenNotificationsSupported) {
            findPreference<Preference>(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY)?.apply { remove() }
        }
    }

    private fun initializeNotificationQuickDelete() {
        val notificationActionsSupported = NotificationController.platformSupportsExtendedNotifications()
        if (!notificationActionsSupported) {
            findPreference<Preference>(PREFERENCE_NOTIFICATION_QUICK_DELETE)?.apply { remove() }
        }
    }

    private fun initializeExtraKeysManagement() {
        findPreference<Preference>(PREFERENCE_PEP_EXTRA_KEYS)?.apply {
            setOnPreferenceClickListener {
                PepExtraKeys.actionStart(context)
                true
            }
        }
    }

    private fun initializeGlobalpEpKeyReset() {
        findPreference<Preference>(PREFERENCE_PEP_OWN_IDS_KEY_RESET)?.apply {
            widgetLayoutResource = R.layout.preference_loading_widget
            setOnPreferenceClickListener {
                AlertDialog.Builder(view?.context,
                    ThemeManager.getAttributeResource(requireContext(), R.attr.resetAllAccountsDialogStyle))
                        .setMessage(R.string.pep_key_reset_all_own_ids_warning)
                        .setTitle(R.string.pep_key_reset_all_own_ids_summary)
                        .setCancelable(false)
                        .setPositiveButton(R.string.reset_all) { _, _ ->
                            dopEpKeyReset()
                        }.setNegativeButton(R.string.cancel_action, null)
                        .show()


                true
            }
        }
    }

    private fun initializeGlobalpEpSync() {
        if (!BuildConfig.WITH_KEY_SYNC) {
            findPreference<Preference>(PREFERENCE_PEP_ENABLE_SYNC)?.remove()
        } else {
            (findPreference(PREFERENCE_PEP_ENABLE_SYNC) as SwitchPreferenceCompat?)?.apply {
                this.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                    processKeySyncSwitchClick(preference, newValue)
                }
            }
        }
    }

    private fun initializeExportPEpSupportDataPreference() {
        findPreference<Preference>(PREFERENCE_EXPORT_PEP_SUPPORT_DATA)?.onClick {
            if (permissionChecker.doesntHaveWriteExternalPermission()) {
                permissionRequester.requestStoragePermission((requireActivity() as K9Activity).rootView)
            }
            if (permissionChecker.hasWriteExternalPermission()) {
                ExportpEpSupportDataActivity.showExportPEpSupportDataDialog(requireActivity())
            }
        }
    }

    private fun processKeySyncSwitchClick(preference: Preference, newValue: Any): Boolean {
        if (preference is SwitchPreferenceCompat && newValue is Boolean) {
            if (!newValue) {
                if (syncSwitchDialog == null) {
                    syncSwitchDialog = AlertDialog.Builder(view?.context,
                            ThemeManager.getAttributeResource(requireContext(), R.attr.syncDisableDialogStyle))
                            .setTitle(R.string.keysync_disable_warning_title)
                            .setMessage(R.string.keysync_disable_warning_explanation)
                            .setCancelable(false)
                            .setPositiveButton(R.string.keysync_disable_warning_action_disable) { _, _ -> preference.isChecked = false }
                            .setNegativeButton(R.string.cancel_action) { _, _ -> }
                            .create()
                }
                syncSwitchDialog?.let { dialog -> if (!dialog.isShowing) dialog.show() }
            } else {
                preference.isChecked = true
            }
        }

        return false
    }

    private fun initializeAfterMessageDeleteBehavior() {
        val returnToList: CheckBoxPreference? = findPreference(MESSAGEVIEW_RETURN_TO_LIST) as? CheckBoxPreference
        val showNextMsg: CheckBoxPreference? = findPreference(MESSAGEVIEW_SHOW_NEXT_MSG) as? CheckBoxPreference

        returnToList?.setOnPreferenceChangeListener { _, check ->
            showNextMsg?.isChecked = !(check as Boolean)
            true
        }

        showNextMsg?.setOnPreferenceChangeListener { _, check ->
            returnToList?.isChecked = !(check as Boolean)
            true
        }

    }

    private fun dopEpKeyReset() {
        disableKeyResetClickListener()
        loading?.visibility = View.VISIBLE

        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            ownKeyReset()
            context?.applicationContext?.let {
                FeedbackTools.showLongFeedback(view,
                        it.getString(R.string.key_reset_all_own_identitities_feedback))
            }
            initializeGlobalpEpKeyReset()
            loading?.visibility = View.GONE
        }
    }

    private fun disableKeyResetClickListener() {
        findPreference<Preference>(PREFERENCE_PEP_OWN_IDS_KEY_RESET)?.onPreferenceClickListener = null
    }

    private suspend fun ownKeyReset() = withContext(Dispatchers.Default) {
        val pEpProvider = PEpProviderFactory.createAndSetupProvider(context)
        pEpProvider.apply {
            keyResetAllOwnKeys()
            close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == REQUEST_PICK_DIRECTORY && resultCode == Activity.RESULT_OK && result != null) {
            result.data?.path?.let {
                setAttachmentDefaultPath(it)
            }
        }
        //TODO: merge
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            val files = Utils.getSelectedFilesFromResult(result!!)
            for (uri in files) {
                val file = Utils.getFileForUri(uri)
                setAttachmentDefaultPath(file.path)
            }
        }
        if (requestCode == PASSPHRASE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            result?.let { intent ->
                val isChecked = intent.getBooleanExtra(PASSPHRASE_RESULT_KEY, false)
                (findPreference(PEP_USE_PASSPHRASE_FOR_NEW_KEYS) as SwitchPreferenceCompat?)?.isChecked = isChecked
            }
        }
    }

    private fun attachmentDefaultPath() = dataStore.getString(PREFERENCE_ATTACHMENT_DEFAULT_PATH, "")

    private fun setAttachmentDefaultPath(path: String) {
        attachmentDefaultPathPreference.summary = path
        dataStore.putString(PREFERENCE_ATTACHMENT_DEFAULT_PATH, path)
    }

    companion object {
        private const val REQUEST_PICK_DIRECTORY = 1
        const val FILE_CODE = 2
        private const val PREFERENCE_ATTACHMENT_DEFAULT_PATH = "attachment_default_path"
        private const val PREFERENCE_START_IN_UNIFIED_INBOX = "start_integrated_inbox"
        private const val PREFERENCE_CONFIRM_ACTIONS = "confirm_actions"
        private const val PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY = "lock_screen_notification_visibility"
        private const val PREFERENCE_NOTIFICATION_QUICK_DELETE = "notification_quick_delete"
        private const val CONFIRM_ACTION_DELETE_FROM_NOTIFICATION = "delete_notif"
        private const val PREFERENCE_PEP_EXTRA_KEYS = "pep_extra_keys"
        private const val PREFERENCE_PEP_OWN_IDS_KEY_RESET = "pep_key_reset"
        private const val PREFERENCE_PEP_ENABLE_SYNC = "pep_enable_sync"
        private const val MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list"
        private const val MESSAGEVIEW_SHOW_NEXT_MSG = "messageview_show_next"
        private const val PEP_USE_PASSPHRASE_FOR_NEW_KEYS = "pep_use_passphrase_for_new_keys"
        private const val PREFERENCE_THEME = "theme"
        private const val PREFERENCE_EXPORT_PEP_SUPPORT_DATA = "support_export_pep_data"


        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(
                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey)
    }

    fun setLanguage(newLanguage: String?) {
        K9.setK9Language(newLanguage)
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            dataStore.saveLanguageSettings()
            restartFromMainScreen()
        }
    }

    private fun restartFromMainScreen() {
        SettingsActivity.actionBasicStart(requireContext())
        requireActivity().finishAffinity()
    }

}
