package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.databinding.AccountSetupNamesBinding
import com.fsck.k9.databinding.WizardDoneBinding
import com.fsck.k9.helper.Utility
import com.fsck.k9.planck.ui.tools.KeyboardUtils
import dagger.hilt.android.AndroidEntryPoint
import security.planck.ui.toolbar.ToolBarCustomizer
import javax.inject.Inject

@AndroidEntryPoint
open class AccountSetupNames : K9Activity() {
    private lateinit var binding: AccountSetupNamesBinding
    private lateinit var wizardDoneBinding: WizardDoneBinding
    private val viewModel: AccountSetupNamesViewModel by viewModels()

    private lateinit var mDescription: EditText
    private lateinit var mName: EditText
    private lateinit var mDoneButton: Button
    private lateinit var planckSyncAccount: SwitchCompat

    @Inject
    lateinit var toolBarCustomizer: ToolBarCustomizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AccountSetupNamesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wizardDoneBinding = WizardDoneBinding.bind(binding.root)
        initializeToolbar(true, R.string.account_setup_names_title)
        toolBarCustomizer.setDefaultStatusBarColor()
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_ACCOUNT)?.let {
                viewModel.initialize(it, intent.getBooleanExtra(EXTRA_MANUAL_SETUP, false))
            }
        }
        setupViews()
    }

    private fun setupViews() {
        mDescription = binding.accountDescription
        mName = binding.accountName
        mDoneButton = wizardDoneBinding.done
        planckSyncAccount = binding.pepEnableSyncAccount

        planckSyncAccount.isVisible = !BuildConfig.IS_OFFICIAL

        wizardDoneBinding.done.setOnClickListener {
            onNext()
        }
        mName.doAfterTextChanged { validateFields() }

        //mName.keyListener = TextKeyListener.getInstance(false, TextKeyListener.Capitalize.WORDS)

        /*
         * Since this field is considered optional, we don't set this here. If
         * the user fills in a value we'll reset the current value, otherwise we
         * just leave the saved value alone.
         */
        // mDescription.setText(viewModel.account.getDescription());
        viewModel.account.name?.let { mName.setText(it) }
        if (k9.isRunningOnWorkProfile) {
            mDescription.isFocusable = false
            mName.isFocusable = false
            mDescription.setText(viewModel.account.description)
        }
        if (!mName.text.isNullOrBlank()) {
            mDoneButton.isEnabled = false
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
        mDoneButton.isEnabled = Utility.requiredFieldValid(mName)
        Utility.setCompoundDrawablesAlpha(mDoneButton, if (mDoneButton.isEnabled) 255 else 128)
    }

    private fun onNext() {
        KeyboardUtils.hideKeyboard(this@AccountSetupNames)
        if (!mDescription.text.isNullOrBlank()) {
            viewModel.account.description = mDescription.text.toString()
        }
        viewModel.account.name = mName.text.toString()
        viewModel.account.setPlanckSyncAccount(planckSyncAccount.isChecked)
        showCreateAccountKeysDialog(viewModel.account.uuid, viewModel.manualSetup)
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
