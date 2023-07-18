package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.activity.observe
import com.fsck.k9.fragment.ConfirmationDialogFragment
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject

class OAuthFlowActivity : K9Activity(), ConfirmationDialogFragment.ConfirmationDialogFragmentListener {
    private val authViewModel: AuthViewModel by viewModel()
    private val accountManager: Preferences by inject()

    private lateinit var errorText: TextView
    private lateinit var signInButton: Button
    private lateinit var signInProgress: ProgressBar
    private lateinit var explanationText: TextView

    private val isTokenRevoked: Boolean
        get() = intent.getBooleanExtra(EXTRA_TOKEN_REVOKED, false)

    private val account: Account
        get() {
            val accountUUid = intent.getStringExtra(EXTRA_ACCOUNT_UUID) ?: error("Missing account UUID")
            return if (isTokenRevoked) {
                accountManager.getAccount(accountUUid)
            } else {
                accountManager.getAccountAllowingIncomplete(accountUUid)
            }  ?: error("Account not found")
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_setup_oauth)

        setUpToolbar(true)

        val title =
            if(isTokenRevoked) R.string.account_setup_oauth_title_retry_login
            else R.string.account_setup_oauth_sign_in
        setTitle(title)

        val account = this.account

        errorText = findViewById(R.id.error_text)
        signInProgress = findViewById(R.id.sign_in_progress)
        explanationText = findViewById(R.id.oauth_login_explanation_txt)
        if(isTokenRevoked) {
            explanationText.text = getString(
                R.string.account_setup_oauth_description_retry_login,
                account.email
            )
        } else if (account.mandatoryOAuthProviderType != null) {
            explanationText.text = ""
        }
        signInButton = if (authViewModel.isUsingGoogle(account)) {
            findViewById(R.id.google_sign_in_button)
        } else {
            findViewById(R.id.oauth_sign_in_button)
        }

        signInButton.setOnClickListener { startOAuthFlow(account) }


        savedInstanceState?.let {
            val signInRunning = it.getBoolean(STATE_PROGRESS)
            signInButton.isVisible = !signInRunning
            signInProgress.isVisible = signInRunning
        }

        authViewModel.init(activityResultRegistry, lifecycle)

        authViewModel.uiState.observe(this) { state ->
            handleUiUpdates(state)
        }

        when {
            !account.automaticOAuthMode || isTokenRevoked -> {
                signInButton.isVisible = true
            }
            savedInstanceState == null ->
                startOAuthFlow(account) // start directly on AccountSetup flow
        }
    }

    private fun handleUiUpdates(state: AuthFlowState) {
        when (state) {
            AuthFlowState.Idle -> {
                return
            }
            AuthFlowState.Success -> {
                if (isTokenRevoked) {
                    SettingsActivity.actionBasicStart(this)
                } else {
                    setResult(RESULT_OK)
                }
                finish()
            }
            AuthFlowState.Canceled -> {
                if (account.automaticOAuthMode) {
                    finish()
                } else {
                    displayErrorText(R.string.account_setup_failed_dlg_oauth_flow_canceled)
                }
            }
            is AuthFlowState.Failed -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_flow_failed, state)
            }
            AuthFlowState.NotSupported -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_not_supported)
            }
            AuthFlowState.BrowserNotFound -> {
                displayErrorText(R.string.account_setup_failed_dlg_browser_not_found)
            }
            is AuthFlowState.WrongEmailAddress -> {
                showWrongEmailErrorDialog(state.adminEmail, state.userWrongEmail)
            }
        }

        authViewModel.authResultConsumed()
    }

    private val Account.automaticOAuthMode: Boolean
        get() = mandatoryOAuthProviderType != null

    private fun displayErrorText(errorTextResId: Int, vararg args: Any?) {
        signInProgress.isVisible = false
        signInButton.isVisible = true
        errorText.text = getString(errorTextResId, *args)
    }

    private fun showWrongEmailErrorDialog(adminEmail: String, userWrongEmail: String) {
        signInProgress.isVisible = false
        signInButton.isVisible = true
        val fragment: DialogFragment =
            ConfirmationDialogFragment.newInstance(
                DIALOG_WRONG_EMAIL,
                getString(R.string.account_setup_failed_dlg_title),
                getString(R.string.account_setup_failed_dlg_oauth_wrong_email_address, userWrongEmail, adminEmail),
                getString(R.string.close)
            )

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, DIALOG_WRONG_EMAIL_TAG)
        fragmentTransaction.commitAllowingStateLoss()
    }

    private fun startOAuthFlow(account: Account) {
        signInButton.isVisible = false
        signInProgress.isVisible = true
        errorText.text = ""

        authViewModel.login(account)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_PROGRESS, signInProgress.isVisible)
    }

    override fun onTokenRevoked(accountUuid: String) {
        val currentAccountUuid = intent.getStringExtra(EXTRA_ACCOUNT_UUID)
        if (currentAccountUuid != null && currentAccountUuid != accountUuid) {
            super.onTokenRevoked(accountUuid)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val EXTRA_TOKEN_REVOKED = "tokenRevoked"

        private const val STATE_PROGRESS = "signInProgress"
        private const val DIALOG_WRONG_EMAIL = 1
        private const val DIALOG_WRONG_EMAIL_TAG = "wrongEmailDialog"

        fun buildLaunchIntent(context: Context, accountUuid: String): Intent {
            return Intent(context, OAuthFlowActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
            }
        }

        fun startOAuthFlowOnTokenRevoked(context: Context, accountUuid: String) {
            Intent(context, OAuthFlowActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
                putExtra(EXTRA_TOKEN_REVOKED, true)
            }.also { context.startActivity(it) }
        }
    }

    override fun doPositiveClick(dialogId: Int) {

    }

    override fun doNegativeClick(dialogId: Int) {
        finish()
    }

    override fun dialogCancelled(dialogId: Int) {

    }
}
