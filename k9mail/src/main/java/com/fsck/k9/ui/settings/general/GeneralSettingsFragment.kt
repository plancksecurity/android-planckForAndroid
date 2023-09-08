package com.fsck.k9.ui.settings.general

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.helper.Utility
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.manualsync.PlanckSyncWizard
import com.fsck.k9.planck.ui.keys.PlanckExtraKeys
import com.fsck.k9.planck.ui.tools.FeedbackTools
import com.fsck.k9.planck.ui.tools.ThemeManager
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.withArguments
import com.google.android.material.snackbar.Snackbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.preference_loading_widget.loading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.mdm.ManageableSetting
import security.planck.ui.passphrase.PASSPHRASE_RESULT_CODE
import security.planck.ui.passphrase.PASSPHRASE_RESULT_KEY
import security.planck.ui.passphrase.requestPassphraseForNewKeys
import security.planck.ui.support.export.ExportpEpSupportDataActivity
import javax.inject.Inject

private const val PREFERENCE_PLANCK_MANUAL_SYNC = "planck_key_sync"
@AndroidEntryPoint
class GeneralSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var dataStore: GeneralSettingsDataStore
    @Inject
    lateinit var preferences: Preferences
    @Inject
    lateinit var k9: K9

    private var syncSwitchDialog: AlertDialog? = null
    private var rootkey:String? = null


    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        this.rootkey = rootKey

        setPreferencesFromResource(R.xml.general_settings, rootKey)

        initializePreferences()
    }

    fun refreshPreferences() {
        setPreferencesFromResource(R.xml.general_settings, rootkey)
        initializePreferences()
    }

    private fun initializePreferences() {
        initializeExtraKeysManagement()
        initializeGlobalpEpKeyReset()
        initializeAfterMessageDeleteBehavior()
        initializeGlobalpEpSync()
        initializeExportPEpSupportDataPreference()
        initializeNewKeysPassphrase()
        initializeManualSync()
        initializeUnsecureDeliveryWarning()
        initializeDebugLogging()
        initializeAuditLogDataTimeRetention()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
        dataStore.activity = activity
    }

    private fun initializeNewKeysPassphrase() {
        findPreference<SwitchPreferenceCompat>(PEP_USE_PASSPHRASE_FOR_NEW_KEYS)?.apply {
            this.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                    processNewKeysSwitchClick(preference, newValue)
                }
        }
    }

    private fun initializeManualSync() {
        val preference = findPreference<Preference>(PREFERENCE_PLANCK_MANUAL_SYNC)
        if (!shouldDisplayManualSyncButton()) {
            preference?.isVisible = false
        } else {
            configureManualSync(preference)
        }
    }

    private fun configureManualSync(preference: Preference?) {
        preference?.apply {
            setOnPreferenceClickListener {
                view?.let {
                    if (isDeviceOnline()) {
                        AlertDialog.Builder(it.context)
                            .setTitle(getString(R.string.sync_title))
                            .setMessage(R.string.planck_key_sync_warning)
                            .setCancelable(true)
                            .setPositiveButton(R.string.sync_action) { _, _ ->
                                startManualSync()
                            }.setNegativeButton(R.string.cancel_action, null).show()
                    } else {
                        Snackbar.make(it, R.string.offline, Snackbar.LENGTH_LONG).show()
                    }
                }
                true
            }
        }
    }

    private fun startManualSync() {
        PlanckSyncWizard.startKeySync(requireActivity())
    }

    private fun shouldDisplayManualSyncButton(): Boolean =
        K9.isPlanckSyncEnabled() && preferences.availableAccounts.any { it.isPlanckSyncEnabled }

    private fun isDeviceOnline(): Boolean =
        kotlin.runCatching { Utility.hasConnectivity(K9.app) }.getOrDefault(false)


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

    private fun initializeExtraKeysManagement() {
        findPreference<Preference>(PREFERENCE_PEP_EXTRA_KEYS)?.apply {
            setOnPreferenceClickListener {
                PlanckExtraKeys.actionStart(context)
                true
            }
        }
    }

    private fun initializeGlobalpEpKeyReset() {
        findPreference<Preference>(PREFERENCE_PEP_OWN_IDS_KEY_RESET)?.apply {
            widgetLayoutResource = R.layout.preference_loading_widget
            setOnPreferenceClickListener {
                widgetLayoutResource = R.layout.preference_loading_widget
                AlertDialog.Builder(view?.context,
                    ThemeManager.getAttributeResource(requireContext(), R.attr.resetAllAccountsDialogStyle))
                        .setMessage(R.string.pep_key_reset_all_own_ids_warning)
                        .setTitle(R.string.pep_key_reset_all_own_ids_summary)
                        .setCancelable(false)
                        .setPositiveButton(R.string.reset_all) { _, _ ->
                            dopEpKeyReset()
                        }.setNeutralButton(R.string.cancel_action, null)
                        .show()
                true
            }
        }
    }

    private fun initializeGlobalpEpSync() {

        (findPreference(PREFERENCE_PEP_ENABLE_SYNC) as SwitchPreferenceCompat?)?.apply {
            this.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    processKeySyncSwitchClick(preference, newValue)
                }
        }

    }

    private fun initializeExportPEpSupportDataPreference() {
        findPreference<Preference>(PREFERENCE_EXPORT_PEP_SUPPORT_DATA)?.onClick {
            ExportpEpSupportDataActivity.showExportPEpSupportDataDialog(requireActivity())
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

    private suspend fun ownKeyReset() = withContext(PlanckDispatcher) {
        val pEpProvider = (requireContext().applicationContext as K9).planckProvider
        pEpProvider.keyResetAllOwnKeys()
    }

    private fun initializeUnsecureDeliveryWarning() {
        initializeManagedSettingLockedFeedback(
            K9.getPlanckForwardWarningEnabled(),
            PREFERENCE_UNSECURE_DELIVERY_WARNING
        )
    }

    private fun initializeDebugLogging() {
        initializeManagedSettingLockedFeedback(K9.getDebug(), PREFERENCE_DEBUG_LOGGING)
    }

    private fun initializeAuditLogDataTimeRetention() {
        initializeManagedSettingLockedFeedback(
            k9.auditLogDataTimeRetention,
            PREFERENCE_AUDIT_LOG_TIME_RETENTION
        )
    }

    private fun <T> initializeManagedSettingLockedFeedback(
        setting: ManageableSetting<T>,
        prefKey: String,
    ) {
        if (setting.locked) {
            (findPreference(prefKey) as? Preference)?.apply {
                isEnabled = false
                summary = getString(R.string.preference_summary_locked_by_it_manager, summary)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == PASSPHRASE_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            result?.let { intent ->
                val isChecked = intent.getBooleanExtra(PASSPHRASE_RESULT_KEY, false)
                (findPreference(PEP_USE_PASSPHRASE_FOR_NEW_KEYS) as SwitchPreferenceCompat?)?.isChecked = isChecked
            }
        }
    }

    companion object {
        private const val PREFERENCE_START_IN_UNIFIED_INBOX = "start_integrated_inbox"
        private const val PREFERENCE_PEP_EXTRA_KEYS = "pep_extra_keys"
        private const val PREFERENCE_PEP_OWN_IDS_KEY_RESET = "pep_key_reset"
        private const val PREFERENCE_PEP_ENABLE_SYNC = "pep_enable_sync"
        private const val PREFERENCE_PEP_SYNC_FOLDER = "pep_sync_folder"
        private const val MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list"
        private const val MESSAGEVIEW_SHOW_NEXT_MSG = "messageview_show_next"
        private const val PEP_USE_PASSPHRASE_FOR_NEW_KEYS = "pep_use_passphrase_for_new_keys"
        private const val PREFERENCE_THEME = "theme"
        private const val PREFERENCE_EXPORT_PEP_SUPPORT_DATA = "support_export_pep_data"
        private const val PREFERENCE_UNSECURE_DELIVERY_WARNING = "pep_forward_warning"
        private const val PREFERENCE_DEBUG_LOGGING = "debug_logging"
        private const val PREFERENCE_AUDIT_LOG_TIME_RETENTION = "audit_log_data_time_retention"


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
