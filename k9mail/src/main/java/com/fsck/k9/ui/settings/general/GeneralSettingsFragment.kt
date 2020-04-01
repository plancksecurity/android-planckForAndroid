package com.fsck.k9.ui.settings.general

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.CheckBoxPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.TwoStatePreference
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.helper.FileBrowserHelper
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.filepicker.Utils
import com.fsck.k9.pEp.ui.keys.PepExtraKeys
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.preference_loading_widget.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.io.File

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val dataStore: GeneralSettingsDataStore by inject()
    private val fileBrowserHelper: FileBrowserHelper by inject()

    private lateinit var attachmentDefaultPathPreference: Preference


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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.title = preferenceScreen.title
        dataStore.activity = activity
    }

    private fun initializeAttachmentDefaultPathPreference() {
        findPreference(PREFERENCE_ATTACHMENT_DEFAULT_PATH)?.apply {
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
            findPreference(PREFERENCE_LOCK_SCREEN_NOTIFICATION_VISIBILITY)?.apply { remove() }
        }
    }

    private fun initializeNotificationQuickDelete() {
        val notificationActionsSupported = NotificationController.platformSupportsExtendedNotifications()
        if (!notificationActionsSupported) {
            findPreference(PREFERENCE_NOTIFICATION_QUICK_DELETE)?.apply { remove() }
        }
    }

    private fun initializeExtraKeysManagement() {
        findPreference(PREFERENCE_PEP_EXTRA_KEYS)?.apply {
            setOnPreferenceClickListener {
                PepExtraKeys.actionStart(context)
                true
            }
        }
    }

    private fun initializeGlobalpEpKeyReset() {
        findPreference(PREFERENCE_PEP_OWN_IDS_KEY_RESET)?.apply {
            widgetLayoutResource = R.layout.preference_loading_widget
            setOnPreferenceClickListener {
                AlertDialog.Builder(view?.context)
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
            findPreference(PREFERENCE_PEP_ENABLE_SYNC)?.remove()

        } else {
            (findPreference(PREFERENCE_PEP_ENABLE_SYNC) as TwoStatePreference?)?.apply {
                onPreferenceClickListener = Preference.OnPreferenceClickListener { preference ->
                    processKeySyncSwitchClick(preference as TwoStatePreference)
                }
            }
        }
    }

    private fun processKeySyncSwitchClick(preference: TwoStatePreference): Boolean {
        // IF we are disabling sync, warning, if not just set it.
        // 1 uncheck to return to "current state
        preference.isChecked = !preference.isChecked

        //2 If we are disabling (which means it is checked)
        if (preference.isChecked) {
            AlertDialog.Builder(view?.context, R.style.SyncDisableDialog)
                    .setTitle(R.string.keysync_disable_warning_title)
                    .setMessage(R.string.keysync_disable_warning_explanation)
                    .setCancelable(false)
                    .setPositiveButton(R.string.keysync_disable_warning_action_disable) { _, _ ->
                        preference.isChecked = false
                    }.setNegativeButton(R.string.cancel_action) { _, _ ->
                        preference.isChecked = true
                    }
                    .show()
        } else {
            preference.isChecked = true
        }
        return true
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
        findPreference(PREFERENCE_PEP_OWN_IDS_KEY_RESET).onPreferenceClickListener = null
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


        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(
                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey)
    }
}
