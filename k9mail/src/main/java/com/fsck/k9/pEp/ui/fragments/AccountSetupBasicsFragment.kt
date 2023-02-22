package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.content.ComponentCallbacks
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.view.isVisible
import com.fsck.k9.*
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.Companion.RESULT_CODE_MANUAL_SETUP_NEEDED
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.Companion.actionCheckSettings
import com.fsck.k9.activity.setup.AccountSetupNames
import com.fsck.k9.activity.setup.OAuthFlowActivity.Companion.buildLaunchIntent
import com.fsck.k9.auth.OAuthProviderType
import com.fsck.k9.helper.SimpleTextWatcher
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.pEp.ui.tools.SetupAccountType
import com.fsck.k9.ui.getEnum
import com.fsck.k9.ui.putEnum
import com.fsck.k9.view.ClientCertificateSpinner
import org.koin.android.ext.android.inject
import security.pEp.provisioning.ProvisioningSettings
import timber.log.Timber
import java.net.URISyntaxException
import javax.inject.Inject

class AccountSetupBasicsFragment : PEpFragment() {
    private val preferences: Preferences by (this as ComponentCallbacks).inject()
    private val emailValidator: EmailAddressValidator by (this as ComponentCallbacks).inject()

    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var advancedOptionsContainer: View
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var passwordLayout: View
    private lateinit var googleButton: Button
    private lateinit var microsoftButton: Button
    private var uiState = UiState.PASSWORD_FLOW
    private var account: Account? = null
    private var checkedIncoming = false
    private lateinit var rootView: View
    private lateinit var accountSetupNavigator: AccountSetupNavigator
    private lateinit var pEpUIArtefactCache: PePUIArtefactCache

    @Inject
    lateinit var pEpSettingsChecker: PEpSettingsChecker

    @Inject
    lateinit var setupAccountType: SetupAccountType

    @Inject
    lateinit var provisioningSettings: ProvisioningSettings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setupPEpFragmentToolbar()
        rootView = inflater.inflate(R.layout.fragment_account_login, container, false)
        setupToolbar()
        emailView = rootView.findViewById(R.id.account_email)
        passwordView = rootView.findViewById(R.id.account_password)
        clientCertificateCheckBox = rootView.findViewById(R.id.account_client_certificate)
        clientCertificateSpinner = rootView.findViewById(R.id.account_client_certificate_spinner)
        advancedOptionsContainer = rootView.findViewById(R.id.foldable_advanced_options)
        nextButton = rootView.findViewById(R.id.next)
        manualSetupButton = rootView.findViewById(R.id.manual_setup)
        passwordLayout = rootView.findViewById(R.id.account_password_layout)
        googleButton = rootView.findViewById(R.id.google_sign_in_button)
        microsoftButton = rootView.findViewById(R.id.microsoft_sign_in_button)
        manualSetupButton.setOnClickListener { onManualSetup(true) }
        googleButton.setOnClickListener { startGoogleFlow() }
        microsoftButton.setOnClickListener { startMicrosoftFlow() }

        initializeViewListeners()
        validateFields()
        pEpUIArtefactCache = PePUIArtefactCache.getInstance(requireContext().applicationContext)
        val email = pEpUIArtefactCache.emailInPreferences
        val password = pEpUIArtefactCache.passwordInPreferences
        if (email != null && password != null) {
            emailView.setText(email)
            passwordView.setText(password)
        }
        if (k9.isRunningOnWorkProfile) {
            updateUiFromProvisioningSettings()
        }
        setHasOptionsMenu(!BuildConfig.IS_ENTERPRISE)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreScreenState(savedInstanceState)
        /*
         * We wait until now to initialize the listeners because we didn't want the OnCheckedChangeListener active
         * while the clientCertificateCheckBox state was being restored because it could trigger the pop-up of a
         * ClientCertificateSpinner.chooseCertificate() dialog.
         */
        initializeViewListeners()
        validateFields()

        updateUi()
    }

    private fun updateUiFromProvisioningSettings() {
        emailView.setText(provisioningSettings.email)
        emailView.isFocusable = false
        val provisionSettings = provisioningSettings.provisionedMailSettings
        if (provisionSettings != null) {
            val isExternalAuth =
                provisionSettings.incoming.authType === security.pEp.mdm.AuthType.EXTERNAL
            clientCertificateCheckBox.isChecked = isExternalAuth
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.account_setup_basic_option, menu)
    }

    private fun setupToolbar() {
        (activity as AccountSetupBasics?)!!.initializeToolbar(
            !requireActivity().isTaskRoot,
            R.string.account_setup_basics_title
        )
    }

    private fun initializeViewListeners() {
        val textWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable) {
                val checkPassword = uiState == UiState.PASSWORD_FLOW
                validateFields(checkPassword)
            }
        }

        emailView.addTextChangedListener(textWatcher)
        passwordView.addTextChangedListener(textWatcher)

        clientCertificateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            updateViewVisibility(isChecked)
            validateFields()

            // Have the user select the client certificate if not already selected
            if (isChecked && clientCertificateSpinner.alias == null) {
                clientCertificateSpinner.chooseCertificate()
            }
        }

        clientCertificateSpinner.setOnClientCertificateChangedListener {
            validateFields()
        }
    }

    private fun updateUi() {
        when (uiState) {
            UiState.EMAIL_ADDRESS_ONLY -> {
                passwordLayout.isVisible = false
                advancedOptionsContainer.isVisible = false
            }
            UiState.PASSWORD_FLOW -> {
                passwordLayout.isVisible = true
                advancedOptionsContainer.isVisible = true
                nextButton.setOnClickListener { attemptAutoSetup() }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putEnum(STATE_KEY_UI_STATE, uiState)
        outState.putString(EXTRA_ACCOUNT, account?.uuid)
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
    }

    private fun restoreScreenState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            uiState = savedInstanceState.getEnum(STATE_KEY_UI_STATE, UiState.EMAIL_ADDRESS_ONLY)

            val accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT)
            if (accountUuid != null) {
                account = preferences.getAccountAllowingIncomplete(accountUuid)
            }

            checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
            updateViewVisibility(clientCertificateCheckBox.isChecked)
        }
    }

    private fun updateViewVisibility(usingCertificates: Boolean) {
        clientCertificateSpinner.isVisible = usingCertificates
        if (usingCertificates) {
            if (k9.isRunningOnWorkProfile) {
                passwordLayout.visibility = View.GONE
            }
        }
    }

    private fun validateFields(checkPassword: Boolean = true) {
        val email = emailView.text?.toString().orEmpty()
        val valid = Utility.requiredFieldValid(emailView) && emailValidator.isValidAddressOnly(email) &&
                (!checkPassword || isPasswordFieldValid())

        nextButton.isEnabled = valid
        nextButton.isFocusable = valid
        manualSetupButton.isEnabled = valid
    }

    private fun isPasswordFieldValid(): Boolean {
        val clientCertificateChecked = clientCertificateCheckBox.isChecked
        val clientCertificateAlias = clientCertificateSpinner.alias

        return !clientCertificateChecked && Utility.requiredFieldValid(passwordView) ||
                clientCertificateChecked && clientCertificateAlias != null
    }

    private fun startGoogleFlow() {
        initAccount()
        startOAuthFlow(OAuthProviderType.GOOGLE)
    }

    private fun startMicrosoftFlow() {
        initAccount()
        startOAuthFlow(OAuthProviderType.MICROSOFT)
    }

    private fun startOAuthFlow(oAuthProviderType: OAuthProviderType?) {
        val account = account!!.also { it.oAuthProviderType = oAuthProviderType }

        val intent = buildLaunchIntent(requireContext(), account.uuid)
        requireActivity().startActivityForResult(intent, REQUEST_CODE_OAUTH)
    }

    private fun checkSettings(direction: CheckDirection = CheckDirection.INCOMING) {
        actionCheckSettings(requireActivity(), account!!, direction)
    }

    private fun saveCredentialsInPreferences() {
        pEpUIArtefactCache.saveCredentialsInPreferences(
            emailView.text.toString(),
            passwordView.text.toString()
        )
    }

    override fun onResume() {
        super.onResume()
        accountSetupNavigator = (activity as AccountSetupBasics?)!!.accountSetupNavigator
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.BASICS, account)
        validateFields()
    }

    private fun attemptAutoSetup() {
        val email = emailView.text?.toString() ?: error("Email missing")
        if (accountWasAlreadySet(email)) return

        if (clientCertificateCheckBox.isChecked) {
            // Auto-setup doesn't support client certificates.
            onManualSetup(true)
            return
        }

        val initialSettings = defaultConnectionSettings(
            email, passwordView.text?.toString(), null, AuthType.PLAIN)

        finishAutoSetup(initialSettings)
    }

    private fun finishAutoSetup(connectionSettings: ConnectionSettings) {
        createAccount(connectionSettings)

        saveCredentialsInPreferences()
        // Check incoming here. Then check outgoing in onActivityResult()
        checkSettings()
    }

    private fun createAccount(connectionSettings: ConnectionSettings): Account {
        val email = emailView.text?.toString() ?: error("Email missing")

        val account = initAccount(email)

        account.storeUri = RemoteStore.createStoreUri(connectionSettings.incoming)

        account.transportUri = Transport.createTransportUri(connectionSettings.outgoing)

        return account
    }

    private fun initAccount(email: String? = null): Account {
        val account = this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        } ?: createAccount()
        this.account = account

        account.name = ownerName
        account.email = email

        if (k9.isRunningOnWorkProfile) {
            account.name = account.name ?: email
            account.description =
                if (!Utility.isNullOrBlank(provisioningSettings.accountDescription))
                    provisioningSettings.accountDescription
                else email
        }
        return account
    }

    private fun retrieveAccount(): Account? {
        return this.account?.let { currentAccount ->
            preferences.getAccountAllowingIncomplete(currentAccount.uuid)
        }
    }

    private fun createAccount(): Account {
        return preferences.newAccount()
    }

    private val ownerName: String?
        get() = if (k9.isRunningOnWorkProfile) {
            provisioningSettings.senderName
        } else {
            preferences.defaultAccount?.name ?: ""
        }

    private fun accountWasAlreadySet(email: String): Boolean {
        if (accountAlreadyExists(email)) {
            showFeedback(getString(R.string.account_already_exists))
            return true
        }
        return false
    }

    private fun showFeedback(feedback: String) {
        FeedbackTools.showLongFeedback(view, feedback)
    }

    private fun accountAlreadyExists(email: String): Boolean {
        val preferences = Preferences.getPreferences(activity)
        val accounts = preferences.accounts
        for (account in accounts) {
            if (account.email.equals(email, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!isAdded) {
            return
        }
        if (requestCode == ACTIVITY_REQUEST_PICK_SETTINGS_FILE
            && resultCode != Activity.RESULT_CANCELED
        ) {
            data?.let {
                (requireActivity() as AccountSetupBasics).onImport(data.data)
            }
        } else if (requestCode == REQUEST_CODE_CHECK_SETTINGS) {
            handleCheckSettingsResult(resultCode)
        } else if (requestCode == REQUEST_CODE_OAUTH) {
            handleSignInResult(resultCode)
        }
    }

    private fun handleCheckSettingsResult(resultCode: Int) {
        if (resultCode == RESULT_CODE_MANUAL_SETUP_NEEDED) {
            onManualSetup(false)
            return
        } else if (resultCode != Activity.RESULT_OK) return

        checkNotNull(account) { "Account instance missing" }
        if (!checkedIncoming) {
            // We've successfully checked incoming. Now check outgoing.
            checkedIncoming = true
            checkSettings(CheckDirection.OUTGOING)
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

    private fun goForward() {
        try {
            setupAccountType.setupStoreAndSmtpTransport(
                account,
                ServerSettings.Type.IMAP,
                "imap+ssl+"
            )
            accountSetupNavigator.goForward(parentFragmentManager, account)
        } catch (e: URISyntaxException) {
            Timber.e(e)
        }
    }

    private fun onManualSetup(fromUser: Boolean) {
        (requireActivity() as AccountSetupBasics).setManualSetupRequired(true)
        if (fromUser) {
            val email = emailView.text?.toString() ?: error("Email missing")
            if (accountWasAlreadySet(email)) {
                return
            }
            val account = initAccount(email)
            setDefaultSettingsForManualSetup(account)
        } else {
            val account = retrieveAccount() ?: error("Account is null!!")
            if (account.storeUri == null || account.transportUri == null) {
                setDefaultSettingsForManualSetup(account)
            }
        }
        saveCredentialsInPreferences()
        goForward()
    }

    private fun setDefaultSettingsForManualSetup(account: Account) {
        var password: String? = null
        var clientCertificateAlias: String? = null
        val authenticationType: AuthType
        if (clientCertificateCheckBox.isChecked) {
            if (passwordView.text.toString().trim().isEmpty()) {
                authenticationType = AuthType.EXTERNAL
            } else {
                authenticationType = AuthType.EXTERNAL_PLAIN
                password = passwordView.text.toString()
            }
            clientCertificateAlias = clientCertificateSpinner.alias
        } else {
            authenticationType = AuthType.PLAIN
            password = passwordView.text.toString()
        }
        val connectionSettings =
            defaultConnectionSettings(
                account.email,
                password,
                clientCertificateAlias,
                authenticationType
            )
        account.setMailSettings(requireContext(), connectionSettings)
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

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private enum class UiState {
        EMAIL_ADDRESS_ONLY,
        PASSWORD_FLOW
    }

    companion object {
        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 0
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val STATE_KEY_UI_STATE = "com.fsck.k9.AccountSetupBasics.uiState"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val REQUEST_CODE_CHECK_SETTINGS = AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
    }
}