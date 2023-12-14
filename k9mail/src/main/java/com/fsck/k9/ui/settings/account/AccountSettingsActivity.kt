package com.fsck.k9.ui.settings.account

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceScreen
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import security.planck.dialog.ConfirmationDialog
import security.planck.dialog.showConfirmationDialog
import timber.log.Timber

@AndroidEntryPoint
class AccountSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback {
    private val viewModel: AccountSettingsViewModel by viewModels()
    private lateinit var accountUuid: String
    private var startScreenKey: String? = null
    private var fragmentAdded = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_account_settings_option, menu)
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.activity_account_settings)

        initializeActionBar()

        if (!decodeArguments()) {
            Timber.d("Invalid arguments")
            finish()
            return
        }

        loadAccount()
        initializeDeleteAccountConfirmationListener()
    }

    private fun initializeDeleteAccountConfirmationListener() {
        supportFragmentManager.setFragmentResultListener(
            DELETE_ACCOUNT_CONFIRMATION_DIALOG_TAG,
            this
        ) { requestKey, bundle ->
            if (requestKey == DELETE_ACCOUNT_CONFIRMATION_DIALOG_TAG) {
                val result = bundle.getInt(ConfirmationDialog.RESULT_KEY)
                if (result == DialogInterface.BUTTON_POSITIVE) {
                    deleteAccountWork()
                }
            }
        }
    }

    private fun initializeActionBar() {
       setUpToolbar(true)
    }

    private fun decodeArguments(): Boolean {
        accountUuid = intent.getStringExtra(ARG_ACCOUNT_UUID) ?: return false
        startScreenKey = intent.getStringExtra(ARG_START_SCREEN_KEY)
        return true
    }

    private fun loadAccount() {
        viewModel.getAccount(accountUuid).observe(this) { account ->
            if (account == null) {
                Timber.w("Account with UUID %s not found", accountUuid)
                finish()
                return@observe
            }

            toolbar?.subtitle = account.email
            addAccountSettingsFragment()
        }
    }

    private fun addAccountSettingsFragment() {
        val needToAddFragment = supportFragmentManager.findFragmentById(R.id.accountSettingsContainer) == null
        if (needToAddFragment && !fragmentAdded) {
            fragmentAdded = true
            fragmentTransaction {
                add(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, startScreenKey))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        } else if (item.itemId == R.id.delete_account) {
            val description = viewModel.getAccount(accountUuid).value?.description ?: ""
            showConfirmationDialog(
                DELETE_ACCOUNT_CONFIRMATION_DIALOG_TAG,
                getString( R.string.account_delete_dlg_title),
                getString(R.string.account_delete_dlg_instructions_fmt, description),
                getString(R.string.okay_action),
                getString(R.string.cancel_action)
            )
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, preferenceScreen.key))
        }

        return true
    }

    private fun deleteAccountWork() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {

            val account = viewModel.getAccount(accountUuid).value

            if (account is Account) {
                val realAccount = account as Account?
                try {
                    realAccount?.localStore?.delete()
                } catch (e: Exception) {
                    // Ignore, this may lead to localStores on sd-cards that
                    // are currently not inserted to be left
                }

                MessagingController.getInstance(application).deleteAccount(realAccount)
                Preferences.getPreferences(this@AccountSettingsActivity).deleteAccount(realAccount)
                K9.setServicesEnabled(this@AccountSettingsActivity)

                accountDeleted()
            }
        }
    }

    private fun accountDeleted() {
        val intent = Intent()
        intent.putExtra(EXTRA_ACCOUNT_DELETED, true)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    companion object {
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val ARG_START_SCREEN_KEY = "startScreen"
        private const val DELETE_ACCOUNT_CONFIRMATION_DIALOG_TAG = "deleteAccountConfirmationDialog"
        const val EXTRA_ACCOUNT_DELETED = "extra_account_deleted"
        const val ACTIVITY_REQUEST_ACCOUNT_SETTINGS = 10012

        @JvmStatic
        fun start(context: Activity, accountUuid: String) {
            val intent = Intent(context, AccountSettingsActivity::class.java).apply {
                putExtra(ARG_ACCOUNT_UUID, accountUuid)
            }
            context.startActivityForResult(intent, ACTIVITY_REQUEST_ACCOUNT_SETTINGS)
        }

        @JvmStatic
        fun startCryptoSettings(context: Context, accountUuid: String) {
            val intent = Intent(context, AccountSettingsActivity::class.java).apply {
                putExtra(ARG_ACCOUNT_UUID, accountUuid)
                putExtra(ARG_START_SCREEN_KEY, AccountSettingsFragment.PREFERENCE_OPENPGP)
            }
            context.startActivity(intent)
        }
    }

    override fun search(query: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
