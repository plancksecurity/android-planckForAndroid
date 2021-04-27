package com.fsck.k9.ui.settings.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartScreenCallback
import androidx.preference.PreferenceScreen
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.ui.fragmentTransaction
import com.fsck.k9.ui.fragmentTransactionWithBackStack
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.settings.account.removeaccount.RemoveAccountActivity
import org.koin.android.architecture.ext.viewModel
import security.pEp.mdm.RestrictionsListener
import timber.log.Timber

class AccountSettingsActivity : K9Activity(), OnPreferenceStartScreenCallback, RestrictionsListener {
    private val viewModel: AccountSettingsViewModel by viewModel()
    private lateinit var accountUuid: String
    private var startScreenKey: String? = null
    private var fragmentAdded = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.account_settings_option, menu)
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
        setConfigurationManagerListener(this)

        loadAccount()
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
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.delete_account -> {
                viewModel.getAccount(accountUuid).value?.let { account ->
                    RemoveAccountActivity.start(this, account)
                }
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun updatedRestrictions() {
        val fragment =
                supportFragmentManager.findFragmentById(R.id.accountSettingsContainer) as AccountSettingsFragment?
        fragment?.refreshPreferences()
    }

    override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat, preferenceScreen: PreferenceScreen
    ): Boolean {
        fragmentTransactionWithBackStack {
            replace(R.id.accountSettingsContainer, AccountSettingsFragment.create(accountUuid, preferenceScreen.key))
        }

        return true
    }

    companion object {
        private const val ARG_ACCOUNT_UUID = "accountUuid"
        private const val ARG_START_SCREEN_KEY = "startScreen"
        const val ACTIVITY_REQUEST_ACCOUNT_SETTINGS = 10012

        @JvmStatic
        fun start(activity: FragmentActivity, accountUuid: String) {
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            intent.putExtra(ARG_ACCOUNT_UUID, accountUuid)
            activity.startActivityForResult(intent, ACTIVITY_REQUEST_ACCOUNT_SETTINGS)
        }

        @JvmStatic
        fun startCryptoSettings(activity: Activity, accountUuid: String) {
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            intent.putExtra(ARG_ACCOUNT_UUID, accountUuid)
            intent.putExtra(ARG_START_SCREEN_KEY, AccountSettingsFragment.PREFERENCE_OPENPGP)
            activity.startActivityForResult(intent, ACTIVITY_REQUEST_ACCOUNT_SETTINGS)
        }
    }

    override fun search(query: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && data != null) {
            if(requestCode == RemoveAccountActivity.ACTIVITY_REQUEST_REMOVE_ACCOUNT) {
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
    }

}
