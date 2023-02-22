package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.activity.setup.AccountSetupNames
import com.fsck.k9.activity.setup.OAuthFlowActivity
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.databinding.FragmentAccountSelectAuthBinding
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator
import security.pEp.provisioning.ProvisioningSettings
import javax.inject.Inject

class AccountSetupSelectAuthFragment : PEpFragment() {

    private var _binding: FragmentAccountSelectAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleButton: Button
    private lateinit var microsoftButton: Button
    private lateinit var passwordFlowButton: Button

    private var account: Account? = null
    private var checkedIncoming = false

    @Inject
    lateinit var navigator: AccountSetupNavigator

    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var provisioningSettings: ProvisioningSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        restoreDataState(savedInstanceState)
        _binding = FragmentAccountSelectAuthBinding.inflate(inflater, container, false)
        setupViews()
        return binding.root
    }

    private fun setupViews() {
        googleButton = binding.googleSignInButton
        microsoftButton = binding.microsoftSignInButton
        passwordFlowButton = binding.otherMethodSignInButton

        googleButton.setOnClickListener { startGoogleFlow() }
        microsoftButton.setOnClickListener { startMicrosoftFlow() }
        passwordFlowButton.setOnClickListener { startPasswordFlow() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator.setCurrentStep(AccountSetupNavigator.Step.SELECT_AUTH, null)
    }

    private fun startGoogleFlow() {
        startOAuthFlow(OAuthProviderType.GOOGLE)
    }

    private fun startMicrosoftFlow() {
        startOAuthFlow(OAuthProviderType.MICROSOFT)
    }

    private fun startOAuthFlow(oAuthProviderType: OAuthProviderType) {
        val account = initAccount().also { it.mandatoryOAuthProviderType = oAuthProviderType }
        val intent = OAuthFlowActivity.buildLaunchIntent(requireContext(), account.uuid)
        requireActivity().startActivityForResult(intent, REQUEST_CODE_OAUTH)
    }

    private fun startPasswordFlow() {
        navigator.goToAccountSetupBasicsFragment(parentFragmentManager)
    }

    private fun onManualSetup() {
        (requireActivity() as AccountSetupBasics).setManualSetupRequired(true)
        val account = retrieveAccount() ?: error("Account is null!!")
        if (account.storeUri == null || account.transportUri == null) {
            setDefaultSettingsForManualSetup(account)
        }
    }

    private fun retrieveAccount(): Account? {
        return this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        }
    }

    private fun setDefaultSettingsForManualSetup(account: Account) {
        val email = account.email ?: let {
            account.email = "mail@example.com"
            account.email
        }
        val connectionSettings =
            defaultConnectionSettings(email, null, null, AuthType.PLAIN)
        account.setMailSettings(requireContext(), connectionSettings, false)
    }

    private fun defaultConnectionSettings(
        email: String,
        password: String?,
        alias: String?,
        authType: AuthType
    ): ConnectionSettings {

        val emailParts = splitEmail(email)
        val domain = emailParts[1]
        val imapHost = "mail.$domain"
        val smtpHost = "mail.$domain"
        //val email = account.email
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

    private fun handleCheckSettingsResult(resultCode: Int) {
        if (resultCode == AccountSetupCheckSettings.RESULT_CODE_MANUAL_SETUP_NEEDED) {
            onManualSetup()
            return
        } else if (resultCode != Activity.RESULT_OK) return

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

    private fun handleSignInResult(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) return
        checkNotNull(account) { "Account instance missing" }
        checkSettings()
    }

    private fun checkSettings(direction: AccountSetupCheckSettings.CheckDirection = AccountSetupCheckSettings.CheckDirection.INCOMING) {
        AccountSetupCheckSettings.actionCheckSettings(requireActivity(), account!!, direction, true)
    }

    private fun initAccount(email: String? = null): Account {
        val account = this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        } ?: createAccount()
        this.account = account

        account.name = ownerName
        account.email = email
        return account
    }

    private val ownerName: String
        get() {
            var name = ""
            if (k9.isRunningOnWorkProfile) {
                provisioningSettings.senderName?.let {
                    name = it
                }
            } else {
                name = preferences.defaultAccount?.name.orEmpty()
            }
            return name
        }

    private fun createAccount(): Account {
        return preferences.newAccount()
    }

    private fun restoreDataState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            savedInstanceState.getString(EXTRA_ACCOUNT)?.let { accountUuid ->
                account = preferences.getAccountAllowingIncomplete(accountUuid)
            }

            checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(EXTRA_ACCOUNT, account?.uuid)
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    companion object {
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val REQUEST_CODE_CHECK_SETTINGS =
            AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
    }
}