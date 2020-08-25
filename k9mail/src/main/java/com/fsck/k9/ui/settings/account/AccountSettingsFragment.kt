package com.fsck.k9.ui.settings.account

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.fsck.k9.*
import com.fsck.k9.activity.ManageIdentities
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.AccountSetupComposition
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.pEp.PEpProviderFactory
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.settings.onClick
import com.fsck.k9.ui.settings.remove
import com.fsck.k9.ui.settings.removeEntry
import com.fsck.k9.ui.withArguments
import com.takisoft.preferencex.PreferenceFragmentCompat
import foundation.pEp.jniadapter.pEpException
import kotlinx.android.synthetic.main.preference_loading_widget.*
import kotlinx.coroutines.*
import org.koin.android.architecture.ext.sharedViewModel
import org.koin.android.ext.android.inject
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.util.OpenPgpProviderUtil
import security.pEp.ui.keyimport.KeyImportActivity.Companion.showImportKeyDialog
import timber.log.Timber

class AccountSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: AccountSettingsViewModel by sharedViewModel()
    private val dataStoreFactory: AccountSettingsDataStoreFactory by inject()
    private val storageManager: StorageManager by inject()
    private val openPgpApiManager: OpenPgpApiManager by inject(parameters = { mapOf("lifecycleOwner" to this) })

    private val accountUuid: String by lazy {
        checkNotNull(arguments?.getString(ARG_ACCOUNT_UUID)) { "$ARG_ACCOUNT_UUID == null" }
    }
    private var title: CharSequence? = null


    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        val account = getAccount()
        val dataStore = dataStoreFactory.create(account)

        preferenceManager.preferenceDataStore = dataStore
        setPreferencesFromResource(R.xml.account_settings, rootKey)
        title = preferenceScreen.title

        initializeIncomingServer()
        initializeComposition()
        initializeManageIdentities()
        initializeOutgoingServer()
        initializeQuoteStyle()
        initializeDeletePolicy(account)
        initializeExpungePolicy(account)
        initializeMessageAge(account)
        initializeAdvancedPushSettings(account)
        initializeLocalStorageProvider()
        initializeCryptoSettings(account)
        initializeFolderSettings(account)
        initializeAccountpEpKeyReset(account)
        initializeNewRingtoneOptions()
        initializeAccountpEpSync(account)
        initializePgpImportKey()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().title = title
    }

    private fun initializeIncomingServer() {
        findPreference<Preference>(PREFERENCE_INCOMING_SERVER)?.onClick {
            AccountSetupBasics.actionEditIncomingSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeComposition() {
        findPreference<Preference>(PREFERENCE_COMPOSITION)?.onClick {
            AccountSetupComposition.actionEditCompositionSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeManageIdentities() {
        findPreference<Preference>(PREFERENCE_MANAGE_IDENTITIES)?.onClick {
            ManageIdentities.start(requireActivity(), accountUuid)
        }
    }

    private fun initializeOutgoingServer() {
        findPreference<Preference>(PREFERENCE_OUTGOING_SERVER)?.onClick {
            AccountSetupBasics.actionEditOutgoingSettings(requireActivity(), accountUuid)
        }
    }

    private fun initializeQuoteStyle() {
        findPreference<Preference>(PREFERENCE_QUOTE_STYLE)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val quoteStyle = Account.QuoteStyle.valueOf(newValue.toString())
                notifyDependencyChange(quoteStyle == Account.QuoteStyle.HEADER)
                true
            }
        }
    }

    private fun initializeDeletePolicy(account: Account) {
        (findPreference(PREFERENCE_DELETE_POLICY) as? ListPreference)?.apply {
            if (!account.remoteStore.isSeenFlagSupported) {
                removeEntry(DELETE_POLICY_MARK_AS_READ)
            }
        }
    }

    private fun initializeExpungePolicy(account: Account) {
        findPreference<Preference>(PREFERENCE_EXPUNGE_POLICY)?.apply {
            if (!account.remoteStore.isExpungeCapable) {
                remove()
            }
        }
    }

    private fun initializeMessageAge(account: Account) {
        findPreference<Preference>(PREFERENCE_MESSAGE_AGE)?.apply {
            if (!account.isSearchByDateCapable) {
                remove()
            }
        }
    }

    private fun initializeAdvancedPushSettings(account: Account) {
        if (!account.remoteStore.isPushCapable) {
            findPreference<Preference>(PREFERENCE_PUSH_MODE)?.remove()
            findPreference<Preference>(PREFERENCE_ADVANCED_PUSH_SETTINGS)?.remove()
            findPreference<Preference>(PREFERENCE_REMOTE_SEARCH)?.remove()
        }
    }

    private fun initializeLocalStorageProvider() {
        (findPreference(PREFERENCE_LOCAL_STORAGE_PROVIDER) as? ListPreference)?.apply {
            val providers = storageManager.availableProviders.entries
            entries = providers.map { it.value }.toTypedArray()
            entryValues = providers.map { it.key }.toTypedArray()
        }
    }

    private fun initializeCryptoSettings(account: Account) {
        findPreference<Preference>(PREFERENCE_OPENPGP)?.let {
            configureCryptoPreferences(account)
        }
    }

    private fun initializeAccountpEpKeyReset(account: Account) {
        findPreference<Preference>(PREFERENCE_PEP_ACCOUNT_KEY_RESET)?.apply {
            widgetLayoutResource = R.layout.preference_loading_widget
            setOnPreferenceClickListener {
                AlertDialog.Builder(view?.context)
                        .setTitle(getString(R.string.pep_key_reset_own_id_warning_title, account.email))
                        .setMessage(R.string.pep_key_reset_own_id_warning)
                        .setCancelable(false)
                        .setPositiveButton(R.string.reset) { _, _ ->
                            dopEpKeyReset(account)
                        }.setNegativeButton(R.string.cancel_action, null).show()
                true
            }
        }
    }

    private fun initializePgpImportKey() {
        val app: K9 = context?.applicationContext as K9
        findPreference<Preference>(PREFERENCE_PGP_KEY_IMPORT)?.apply {
            if (app.isGrouped) {
                isEnabled = false
                summary = getString(R.string.pgp_key_import_disabled_summary)
            } else {
                onClick(::onKeyImportClicked)
            }
        }
    }

    private fun onKeyImportClicked() {
        showImportKeyDialog(activity, accountUuid)
    }

    private fun hideKeySyncOptions() {
        findPreference<Preference>(PREFERENCE_PEP_ENABLE_SYNC_ACCOUNT)?.remove()
    }

    private fun initializeAccountpEpSync(account: Account) {
        if (!BuildConfig.WITH_KEY_SYNC) {
            hideKeySyncOptions()
        } else {

            val app: K9 = context?.applicationContext as K9
            val preference: Preference? = findPreference(PREFERENCE_PEP_ENABLE_SYNC_ACCOUNT)

            //It is only possible to enable/disable sync if the device is not part of device group
            // and is not the only/latest account enabled
            //if grouped sync per Account only can be disabled on setup
            preference?.isEnabled = !app.isGrouped && canSyncAccountBeModified(account)

        }
    }

    private fun canSyncAccountBeModified(account: Account): Boolean {
        // if the account is disabled it can be always enabled

        val accounts = Preferences.getPreferences(context).accounts
        val enabledSyncAccount = accounts.sumBy { if (it.isPepSyncEnabled) 1 else 0 }

        return !account.isPepSyncEnabled || enabledSyncAccount != 1
    }

    private fun initializeNewRingtoneOptions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findPreference<Preference>(PREFERENCE_RINGTONE)?.remove()
        }
    }

    private fun dopEpKeyReset(account: Account) {
        disableKeyResetClickListener()
        loading?.visibility = View.VISIBLE

        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        uiScope.launch {
            val keyReset = keyReset(account)
            if (keyReset) {
                context?.applicationContext?.let {
                    FeedbackTools.showLongFeedback(view,
                            it.getString(R.string.key_reset_own_identity_feedback))
                }
            } else {
                context?.applicationContext?.let {
                    FeedbackTools.showLongFeedback(view,
                            it.getString(R.string.key_reset_own_identity_feedback_error))
                }
            }
            initializeAccountpEpKeyReset(account)
            loading?.visibility = View.GONE
        }
    }

    private fun disableKeyResetClickListener() {
        findPreference<Preference>(PREFERENCE_PEP_ACCOUNT_KEY_RESET)?.onPreferenceClickListener = null
    }

    private suspend fun keyReset(account: Account): Boolean = withContext(Dispatchers.Default) {
        val pEpProvider = PEpProviderFactory.createAndSetupProvider(context)
        try {
            val address = Address(account.email, account.name)
            var id = PEpUtils.createIdentity(address, context)
            id = pEpProvider.updateIdentity(id)
            pEpProvider.keyResetIdentity(id, null)
            true
        } catch (e: pEpException) {
            Timber.e(e, "%s %s", "pEpEngine", "Failed to reset identity")
            false
        } finally {
            pEpProvider.close()
        }
    }

    private fun configureCryptoPreferences(account: Account) {
        var pgpProviderName: String? = null
        var pgpProvider = account.openPgpProvider
        var isPgpConfigured = account.isOpenPgpProviderConfigured

        if (isPgpConfigured) {
            pgpProviderName = getOpenPgpProviderName(pgpProvider)
            if (pgpProviderName == null) {
                Toast.makeText(requireContext(), R.string.account_settings_openpgp_missing, Toast.LENGTH_LONG).show()

                account.openPgpProvider = null
                pgpProvider = null
                isPgpConfigured = false
            }
        }

    }

    private fun getOpenPgpProviderName(pgpProvider: String?): String? {
        val packageManager = requireActivity().packageManager
        return OpenPgpProviderUtil.getOpenPgpProviderName(packageManager, pgpProvider)
    }


    private fun initializeFolderSettings(account: Account) {
        findPreference<Preference>(PREFERENCE_FOLDERS)?.let {
            if (!account.remoteStore.isMoveCapable) {
                findPreference<Preference>(PREFERENCE_ARCHIVE_FOLDER)?.remove()
                findPreference<Preference>(PREFERENCE_DRAFTS_FOLDER)?.remove()
                findPreference<Preference>(PREFERENCE_SENT_FOLDER)?.remove()
                findPreference<Preference>(PREFERENCE_SPAM_FOLDER)?.remove()
                findPreference<Preference>(PREFERENCE_TRASH_FOLDER)?.remove()
            }

            loadFolders(account)
        }
    }

    private fun loadFolders(account: Account) {
        viewModel.getFolders(account).observe(this@AccountSettingsFragment) { folders ->
            if (folders != null) {
                FOLDER_LIST_PREFERENCES.forEach {
                    (findPreference(it) as? FolderListPreference)?.folders = folders
                }
            }
        }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        return when {
            resultCode != Activity.RESULT_OK || data == null ->
                super.onActivityResult(requestCode, resultCode, data)
            else ->
                when (requestCode) {
                  //TODO  ACTIVITY_REQUEST_PICK_KEY_FILE ->
                       // onKeyImport(data.data, accountUuid)
                    else ->
                        super.onActivityResult(requestCode, resultCode, data)

                }
        }
    }

    private fun getAccount(): Account {
        return viewModel.getAccountBlocking(accountUuid)
    }


    companion object {
        internal const val PREFERENCE_OPENPGP = "openpgp"
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val PREFERENCE_INCOMING_SERVER = "incoming"
        private const val PREFERENCE_COMPOSITION = "composition"
        private const val PREFERENCE_MANAGE_IDENTITIES = "manage_identities"
        private const val PREFERENCE_OUTGOING_SERVER = "outgoing"
        private const val PREFERENCE_QUOTE_STYLE = "quote_style"
        private const val PREFERENCE_DELETE_POLICY = "delete_policy"
        private const val PREFERENCE_EXPUNGE_POLICY = "expunge_policy"
        private const val PREFERENCE_MESSAGE_AGE = "account_message_age"
        private const val PREFERENCE_PUSH_MODE = "folder_push_mode"
        private const val PREFERENCE_ADVANCED_PUSH_SETTINGS = "push_advanced"
        private const val PREFERENCE_REMOTE_SEARCH = "search"
        private const val PREFERENCE_LOCAL_STORAGE_PROVIDER = "local_storage_provider"
        private const val PREFERENCE_FOLDERS = "folders"
        private const val PREFERENCE_AUTO_EXPAND_FOLDER = "account_setup_auto_expand_folder"
        private const val PREFERENCE_ARCHIVE_FOLDER = "archive_folder"
        private const val PREFERENCE_DRAFTS_FOLDER = "drafts_folder"
        private const val PREFERENCE_SENT_FOLDER = "sent_folder"
        private const val PREFERENCE_SPAM_FOLDER = "spam_folder"
        private const val PREFERENCE_TRASH_FOLDER = "trash_folder"
        private const val PREFERENCE_RINGTONE = "account_ringtone"
        private const val PREFERENCE_PGP_KEY_IMPORT = "pgp_key_import"

        private const val PREFERENCE_PEP_ACCOUNT_KEY_RESET = "pep_key_reset_account"
        private const val PREFERENCE_PEP_ENABLE_SYNC_ACCOUNT = "pep_enable_sync_account"
        private const val DELETE_POLICY_MARK_AS_READ = "MARK_AS_READ"

        private val FOLDER_LIST_PREFERENCES = listOf(
                PREFERENCE_AUTO_EXPAND_FOLDER,
                PREFERENCE_ARCHIVE_FOLDER,
                PREFERENCE_DRAFTS_FOLDER,
                PREFERENCE_SENT_FOLDER,
                PREFERENCE_SPAM_FOLDER,
                PREFERENCE_TRASH_FOLDER
        )

        fun create(accountUuid: String, rootKey: String?) = AccountSettingsFragment().withArguments(
                ARG_ACCOUNT_UUID to accountUuid,
                ARG_PREFERENCE_ROOT to rootKey)
    }
}
