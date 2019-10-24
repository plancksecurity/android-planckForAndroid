package com.fsck.k9.ui.settings.general

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import com.fsck.k9.Account
import com.fsck.k9.R
import com.fsck.k9.helper.FileBrowserHelper
import com.fsck.k9.mail.Address
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.filepicker.Utils
import com.fsck.k9.pEp.ui.keys.PepExtraKeys
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.ui.settings.account.AccountSettingsFragment
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.preference_loading_widget.*
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
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
        findPreference(PEP_EXTRA_KEYS)?.apply {
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
                dopEpKeyReset()
                true
            }
        }
    }

    private fun dopEpKeyReset() {
        disableKeyResetClickListener()
        loading.visibility = View.VISIBLE

        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            try {
                ownKeyReset()
                FeedbackTools.showLongFeedback(view,
                        getString(R.string.key_reset_all_own_identitities_feedback))
                initializeGlobalpEpKeyReset()
                loading.visibility = View.GONE
            } catch (e: Exception) {
                Timber.e(e, "Not able to finish all pEp accounts key reset.")
            }

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
        private const val PEP_EXTRA_KEYS = "pep_extra_keys"
        private const val PREFERENCE_PEP_OWN_IDS_KEY_RESET = "pep_key_reset"


        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(
                PreferenceFragmentCompat.ARG_PREFERENCE_ROOT to rootKey)
    }
}
