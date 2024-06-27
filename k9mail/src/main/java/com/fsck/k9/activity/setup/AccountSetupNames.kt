package com.fsck.k9.activity.setup

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.TextKeyListener
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.SwitchCompat
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.SettingsActivity.Companion.listAccountsOnStartup
import com.fsck.k9.activity.misc.ExtendedAsyncTask
import com.fsck.k9.activity.misc.NonConfigurationInstance
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.Utility
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.ui.tools.KeyboardUtils
import dagger.hilt.android.AndroidEntryPoint
import security.planck.mdm.ConfigurationManager
import security.planck.provisioning.ProvisioningScope.SingleAccountSettings
import security.planck.ui.toolbar.ToolBarCustomizer
import javax.inject.Inject

@AndroidEntryPoint
class AccountSetupNames : K9Activity(), View.OnClickListener {
    private var mDescription: EditText? = null

    private var mName: EditText? = null

    private var mAccount: Account? = null

    private var mDoneButton: Button? = null
    private var pepSyncAccount: SwitchCompat? = null
    private var planckUIArtefactCache: PlanckUIArtefactCache? = null
    private var nonConfigurationInstance: NonConfigurationInstance? = null

    @JvmField
    @Inject
    var toolBarCustomizer: ToolBarCustomizer? = null

    @JvmField
    @Inject
    var configurationManager: ConfigurationManager? = null

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        var retain: Any? = null
        if (nonConfigurationInstance != null && nonConfigurationInstance!!.retain()) {
            retain = nonConfigurationInstance
        }
        return retain
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews(R.layout.account_setup_names)

        initializeToolbar(true, R.string.account_setup_names_title)
        toolBarCustomizer!!.setDefaultStatusBarColor()

        mDescription = findViewById<View>(R.id.account_description) as EditText
        mName = findViewById<View>(R.id.account_name) as EditText
        pepSyncAccount = findViewById(R.id.pep_enable_sync_account)
        pepSyncAccount.setVisibility(if (BuildConfig.IS_OFFICIAL) View.GONE else View.VISIBLE)
        mDoneButton = findViewById<View>(R.id.done) as Button
        mDoneButton!!.setOnClickListener(this)

        val validationTextWatcher: TextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                validateFields()
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }
        }
        mName!!.addTextChangedListener(validationTextWatcher)

        mName!!.keyListener = TextKeyListener.getInstance(false, TextKeyListener.Capitalize.WORDS)

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT)
        mAccount = Preferences.getPreferences(this).getAccountAllowingIncomplete(accountUuid)

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(mAccount.getDescription());
        if (mAccount.getName() != null) {
            mName!!.setText(mAccount.getName())
        }
        if (k9.isRunningOnWorkProfile) {
            mDescription!!.isFocusable = false
            mName!!.isFocusable = false
            mDescription!!.setText(mAccount.getDescription())
        }
        if (!Utility.requiredFieldValid(mName)) {
            mDoneButton!!.isEnabled = false
        }


        planckUIArtefactCache = PlanckUIArtefactCache.getInstance(applicationContext)
        planckUIArtefactCache.removeCredentialsInPreferences()

        restoreNonConfigurationInstance()
    }

    private fun restoreNonConfigurationInstance() {
        nonConfigurationInstance = lastCustomNonConfigurationInstance as NonConfigurationInstance?
        if (nonConfigurationInstance != null) {
            nonConfigurationInstance!!.restore(this)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        when (itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validateFields() {
        mDoneButton!!.isEnabled = Utility.requiredFieldValid(mName)
        Utility.setCompoundDrawablesAlpha(mDoneButton, if (mDoneButton!!.isEnabled) 255 else 128)
    }

    protected fun onNext() {
        KeyboardUtils.hideKeyboard(this@AccountSetupNames)
        if (Utility.requiredFieldValid(mDescription)) {
            mAccount!!.description = mDescription!!.text.toString()
        }
        mAccount!!.name = mName!!.text.toString()
        mAccount!!.setPlanckSyncAccount(pepSyncAccount!!.isChecked)
        val isManualSetup = intent.getBooleanExtra(EXTRA_MANUAL_SETUP, false)
        val accountGenerationTask = PanckGenerateAccountKeysTask(this, mAccount)
        launchGenerateAccountKeysTask(accountGenerationTask, isManualSetup)
    }

    private fun loadConfigurations() {
        configurationManager!!.loadConfigurationsBlocking(SingleAccountSettings(mAccount!!.email))
    }

    @VisibleForTesting
    fun launchGenerateAccountKeysTask(
        accountGenerationTask: PanckGenerateAccountKeysTask,
        manualSetup: Boolean
    ) {
        nonConfigurationInstance = accountGenerationTask
        accountGenerationTask.execute(manualSetup)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.done -> onNext()
        }
    }

    @VisibleForTesting
    class PanckGenerateAccountKeysTask(activity: Activity?, var account: Account?) :
        ExtendedAsyncTask<Boolean?, Void?, Void?>(activity) {
        @VisibleForTesting
        var accountKeysGenerator: AccountKeysGenerator = object : AccountKeysGenerator {
            override fun generateAccountKeys() {
                PlanckUtils.pEpGenerateAccountKeys(mContext, account)
                K9.setServicesEnabled(mContext)
            }

            override fun onAccountKeysGenerationFinished() {
                if ((mProgressDialog != null) && mProgressDialog.isShowing
                    && (mActivity != null) && !mActivity.isDestroyed
                ) {
                    mProgressDialog.dismiss()
                }
                listAccountsOnStartup(mContext)
            }
        }

        public override fun showProgressDialog() {
            mProgressDialog = ProgressDialog(mActivity)
            mProgressDialog.isIndeterminate = true
            mProgressDialog.setCancelable(false)
            mProgressDialog.setMessage(mContext.getString(R.string.pep_account_setup_generating_keys))
            mProgressDialog.show()
        }

        protected override fun onPostExecute(aVoid: Void) {
            super.onPostExecute(aVoid)
            accountKeysGenerator.onAccountKeysGenerationFinished()
        }

        override fun doInBackground(vararg params: Boolean): Void {
            account!!.setupState = Account.SetupState.READY
            val manualSetup = params[0]
            if (manualSetup) {
                account!!.setOptionsOnInstall()
            }
            if ((mContext.applicationContext as K9).isRunningOnWorkProfile) {
                (mActivity as AccountSetupNames).loadConfigurations()
            } else {
                account!!.save(Preferences.getPreferences(mActivity))
            }
            MessagingController.getInstance(mActivity).refreshRemoteSynchronous(account)
            accountKeysGenerator.generateAccountKeys()
            return null
        }
    }

    interface AccountKeysGenerator {
        fun generateAccountKeys()
        fun onAccountKeysGenerationFinished()
    }

    companion object {
        const val EXTRA_ACCOUNT: String = "account"
        private const val EXTRA_MANUAL_SETUP = "manualSetup"

        @JvmStatic
        fun actionSetNames(context: Context, account: Account, manualSetup: Boolean) {
            val i = Intent(context, AccountSetupNames::class.java)
            i.putExtra(EXTRA_ACCOUNT, account.uuid)
            i.putExtra(EXTRA_MANUAL_SETUP, manualSetup)
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}
