package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.observe
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject

class OAuthFlowActivity : K9Activity() {
    private val authViewModel: AuthViewModel by viewModel()
    private val accountManager: Preferences by inject()

    private lateinit var errorText: TextView
    private lateinit var signInButton: Button
    private lateinit var signInProgress: ProgressBar

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account_setup_oauth)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("K9 layouts must provide a toolbar with id='toolbar'.")

        setSupportActionBar(toolbar)
        setTitle(R.string.account_setup_basics_title)

        val accountUUid = intent.getStringExtra(EXTRA_ACCOUNT_UUID) ?: error("Missing account UUID")
        val account = accountManager.getAccountAllowingIncomplete(accountUUid) ?: error("Account not found")

        errorText = findViewById(R.id.error_text)
        signInProgress = findViewById(R.id.sign_in_progress)
        signInButton = if (authViewModel.isUsingGoogle(account)) {
            findViewById(R.id.google_sign_in_button)
        } else {
            findViewById(R.id.oauth_sign_in_button)
        }

        signInButton.isVisible = true
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
    }

    private fun handleUiUpdates(state: AuthFlowState) {
        when (state) {
            AuthFlowState.Idle -> {
                return
            }
            AuthFlowState.Success -> {
                setResult(RESULT_OK)
                finish()
            }
            AuthFlowState.Canceled -> {
                displayErrorText(R.string.account_setup_failed_dlg_oauth_flow_canceled)
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
        }

        authViewModel.authResultConsumed()
    }

    private fun displayErrorText(errorTextResId: Int, vararg args: Any?) {
        signInProgress.isVisible = false
        signInButton.isVisible = true
        errorText.text = getString(errorTextResId, *args)
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

    override fun search(query: String?) {}

    companion object {
        private const val EXTRA_ACCOUNT_UUID = "accountUuid"

        private const val STATE_PROGRESS = "signInProgress"

        fun buildLaunchIntent(context: Context, accountUuid: String): Intent {
            return Intent(context, OAuthFlowActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT_UUID, accountUuid)
            }
        }
    }
}
