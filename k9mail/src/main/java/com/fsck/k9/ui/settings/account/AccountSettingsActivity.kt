package com.fsck.k9.ui.settings.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceScreen
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import dagger.hilt.android.AndroidEntryPoint
import security.planck.ui.removeaccount.RemoveAccountDialog
import security.planck.ui.removeaccount.showRemoveAccountDialog
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
        initializeDeleteAccountDialogListener()
    }

    private fun initializeDeleteAccountDialogListener() {
        supportFragmentManager.setFragmentResultListener(
            RemoveAccountDialog.REQUEST_KEY,
            this
        ) { requestKey, bundle ->
            if (requestKey == RemoveAccountDialog.REQUEST_KEY) {
                val result = bundle.getBoolean(RemoveAccountDialog.RESULT_ACCOUNT_REMOVED)
                if (result) {
                    accountDeleted()
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
            showRemoveAccountDialog(accountUuid)
            return true
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
