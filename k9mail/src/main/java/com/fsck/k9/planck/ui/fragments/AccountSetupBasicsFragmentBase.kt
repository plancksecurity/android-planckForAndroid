package com.fsck.k9.planck.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.activity.setup.AccountSetupNames
import com.fsck.k9.activity.setup.OAuthFlowActivity
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.planck.ui.ConnectionSettings
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator
import com.fsck.k9.planck.ui.tools.SetupAccountType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.provisioning.AccountProvisioningSettings
import security.planck.provisioning.ProvisioningSettings
import security.planck.ui.toolbar.ToolBarCustomizer
import timber.log.Timber
import java.net.URISyntaxException
import javax.inject.Inject

@AndroidEntryPoint
abstract class AccountSetupBasicsFragmentBase : Fragment() {

    protected var account: Account? = null
    private var checkedIncoming = false

    @Inject
    lateinit var navigator: AccountSetupNavigator

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var provisioningSettings: ProvisioningSettings

    @Inject
    lateinit var setupAccountType: SetupAccountType

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer

    @Inject
    lateinit var k9: K9

    protected val accountProvisioningSettings: AccountProvisioningSettings? by lazy {
        provisioningSettings.findNextAccountToInstall()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreDataState(savedInstanceState)
    }

    protected fun setupToolbar(showBackArrow: Boolean = true) {
        (requireActivity() as AccountSetupBasics).initializeToolbar(
            !requireActivity().isTaskRoot
                    || parentFragmentManager.backStackEntryCount > 1,
            R.string.account_setup_basics_title
        )
    }

    private fun handleSignInResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) {
            deleteAccount()
            return
        }
        checkNotNull(account) { "Account instance missing" }
        checkSettings()
    }

    private fun handleCheckSettingsResult(resultCode: Int) {
        if (resultCode == AccountSetupCheckSettings.RESULT_CODE_MANUAL_SETUP_NEEDED) {
            onManualSetup(false)
            return
        } else if (resultCode == Activity.RESULT_CANCELED) {
            deleteAccount()
            return
        }

        checkNotNull(account) { "Account instance missing" }
        if (!checkedIncoming) {
            // We've successfully checked incoming. Now check outgoing.
            checkedIncoming = true
            checkSettings(AccountSetupCheckSettings.CheckDirection.OUTGOING)
        } else {
            // We've successfully checked outgoing as well.
            AccountSetupNames.actionSetNames(requireActivity(), account, false)
        }
    }

    protected fun checkSettings(direction: AccountSetupCheckSettings.CheckDirection = AccountSetupCheckSettings.CheckDirection.INCOMING) {
        AccountSetupCheckSettings.actionCheckSettings(requireActivity(), account!!, direction, true)
    }

    open fun onManualSetup(fromUser: Boolean) {
        (requireActivity() as AccountSetupBasics).setManualSetupRequired(true)
        val account = retrieveAccount() ?: error("Account is null!!")
        if (account.storeUri == null || account.transportUri == null) {
            setDefaultSettingsForManualSetup(account)
        }
        goForward()
    }

    private fun setDefaultSettingsForManualSetup(account: Account) {
        val email = account.email ?: let {
            account.email = DEFAULT_EMAIL
            account.email
        }
        val connectionSettings =
            defaultConnectionSettings(email, null, null, AuthType.PLAIN)
        account.setMailSettings(requireContext(), connectionSettings, false)
    }

    protected fun defaultConnectionSettings(
        email: String,
        password: String?,
        alias: String?,
        authType: AuthType
    ): ConnectionSettings {

        val emailParts = splitEmail(email)
        val domain = emailParts[1]
        val imapHost = "mail.$domain"
        val smtpHost = "mail.$domain"
        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        val storeServer = ServerSettings(
            ServerSettings.Type.IMAP,
            imapHost,
            -1,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            authType,
            email,
            password,
            alias
        )
        val transportServer = ServerSettings(
            ServerSettings.Type.SMTP,
            smtpHost,
            -1,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            authType,
            email,
            password,
            alias
        )
        return ConnectionSettings(storeServer, transportServer)
    }

    private fun splitEmail(email: String): Array<String?> {
        val retParts = arrayOfNulls<String>(2)
        val emailParts = email.split("@").toTypedArray()
        retParts[0] = if (emailParts.isNotEmpty()) emailParts[0] else ""
        retParts[1] = if (emailParts.size > 1) emailParts[1] else ""
        return retParts
    }

    protected fun initAccount(email: String? = null): Account {
        val account = this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        } ?: createAccount()
        this.account = account

        account.name = ownerName
        account.email = email

        if (k9.isRunningOnWorkProfile) {
            account.description =
                if (!Utility.isNullOrBlank(accountProvisioningSettings?.accountDescription))
                    accountProvisioningSettings?.accountDescription
                else email
        }
        return account
    }

    protected fun retrieveAccount(): Account? {
        return this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        }
    }

    protected fun goForward() {
        try {
            setupAccountType.setupStoreAndSmtpTransport(
                account,
                ServerSettings.Type.IMAP,
                "imap+ssl+"
            )
            navigator.goForward(parentFragmentManager, account)
        } catch (e: URISyntaxException) {
            Timber.e(e)
        }
    }

    private val ownerName: String
        get() {
            var name = ""
            if (k9.isRunningOnWorkProfile) {
                if (!accountProvisioningSettings?.senderName.isNullOrBlank()) {
                    name = accountProvisioningSettings?.senderName!!
                } else if (!accountProvisioningSettings?.email.isNullOrBlank()) {
                    name = accountProvisioningSettings?.email!!
                }
            } else {
                name = preferences.defaultAccount?.name.orEmpty()
            }
            return name
        }

    private fun createAccount(): Account {
        return preferences.newAccount().also { navigator.setCurrentStep(navigator.currentStep, it) }
    }

    protected fun deleteAccount() = CoroutineScope(Dispatchers.Main).launch {
        withContext(Dispatchers.IO) {
            retrieveAccount()?.let { preferences.deleteAccount(it) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(EXTRA_ACCOUNT, account?.uuid)
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
    }

    private fun restoreDataState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            savedInstanceState.getString(EXTRA_ACCOUNT)?.let { accountUuid ->
                account = preferences.getAccountAllowingIncomplete(accountUuid)
            }

            checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
        }
    }

    protected fun startGoogleFlow() {
        startOAuthFlow(OAuthProviderType.GOOGLE)
    }

    protected fun startMicrosoftFlow() {
        startOAuthFlow(OAuthProviderType.MICROSOFT)
    }

    private fun startOAuthFlow(oAuthProviderType: OAuthProviderType) {
        val email = if (k9.isRunningOnWorkProfile) accountProvisioningSettings?.email else null
        val account = initAccount(email).also { it.mandatoryOAuthProviderType = oAuthProviderType }
        val intent = OAuthFlowActivity.buildLaunchIntent(requireContext(), account.uuid)
        requireActivity().startActivityForResult(intent,
            REQUEST_CODE_OAUTH
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!isAdded) {
            return
        }
        if (requestCode == REQUEST_CODE_CHECK_SETTINGS) {
            handleCheckSettingsResult(resultCode)
        } else if (requestCode == REQUEST_CODE_OAUTH) {
            handleSignInResult(resultCode)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_CHECK_SETTINGS =
            AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val DEFAULT_EMAIL = "mail@example.com"
    }
}