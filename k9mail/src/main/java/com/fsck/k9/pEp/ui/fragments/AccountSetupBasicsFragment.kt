package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.ContentLoadingProgressBar
import com.fsck.k9.*
import com.fsck.k9.account.AccountCreator
import com.fsck.k9.activity.setup.*
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.Companion.actionCheckSettings
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
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject
import security.pEp.provisioning.ProvisioningSettings
import timber.log.Timber
import java.net.URISyntaxException
import javax.inject.Inject

class AccountSetupBasicsFragment : PEpFragment() {
    private val preferences: Preferences by inject()
    private val emailValidator: EmailAddressValidator by inject()
    private val accountSetupBasicsViewModel: AccountSetupBasicsViewModel by viewModel()

    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var advancedOptionsContainer: View
    private lateinit var mOAuth2CheckBox: CheckBox
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var passwordLayout: View
    private var uiState = UiState.EMAIL_ADDRESS_ONLY
    private var account: Account? = null
    private var checkedIncoming = false
    private lateinit var nextProgressBar: ContentLoadingProgressBar
    private lateinit var rootView: View
    private lateinit var accountSetupNavigator: AccountSetupNavigator
    private lateinit var pEpUIArtefactCache: PePUIArtefactCache
    private var wasLoading = false
    private var oAuthProviderType: OAuthProviderType? = null

    @Inject
    lateinit var pEpSettingsChecker: PEpSettingsChecker

    @Inject
    lateinit var setupAccountType: SetupAccountType

    @Inject
    lateinit var provisioningSettings: ProvisioningSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        oAuthProviderType = arguments?.getString(EXTRA_OAUTH_PROVIDER_TYPE)?.let { OAuthProviderType.valueOf(it) }
        uiState =
            if (oAuthProviderType == null) UiState.PASSWORD_FLOW
            else UiState.EMAIL_ADDRESS_ONLY
    }

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
        passwordLayout = rootView.findViewById(R.id.account_password_layout)
        clientCertificateCheckBox = rootView.findViewById(R.id.account_client_certificate)
        clientCertificateSpinner = rootView.findViewById(R.id.account_client_certificate_spinner)
        advancedOptionsContainer = rootView.findViewById(R.id.foldable_advanced_options)
        nextButton = rootView.findViewById(R.id.next)
        manualSetupButton = rootView.findViewById(R.id.manual_setup)

        manualSetupButton.setOnClickListener { onManualSetup() }

        nextProgressBar = rootView.findViewById(R.id.next_progressbar)

        initializeViewListeners()
        validateFields()
        pEpUIArtefactCache = PePUIArtefactCache.getInstance(requireContext().applicationContext)
        val email = pEpUIArtefactCache.emailInPreferences
        val password = pEpUIArtefactCache.passwordInPreferences
        if (email != null && password != null) {
            emailView.setText(email)
            passwordView.setText(password)
        }
        if (BuildConfig.IS_ENTERPRISE) {
            updateUiFromProvisioningSettings()
        }
        setHasOptionsMenu(BuildConfig.IS_END_USER)
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

        accountSetupBasicsViewModel.connectionSettings.observe(viewLifecycleOwner) { pair ->
            val ready = pair.second
            val event = pair.first
            if (ready) {
                if (!event.hasBeenHandled) {
                    val connectionSettings = event.getContent()
                    if (connectionSettings == null) {
                        onManualSetup()
                    } else {
                        when(uiState) {
                            UiState.EMAIL_ADDRESS_ONLY -> { // this is the original, intial state
                                attemptAutoSetupUsingOnlyEmailAddress(connectionSettings)
                            }
                            UiState.PASSWORD_FLOW -> {
                                finishAutoSetup(connectionSettings)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateUiFromProvisioningSettings() {
        emailView.setText(provisioningSettings.email)
        emailView.isFocusable = false
        val provisionSettings = provisioningSettings.provisionedMailSettings
        if (provisionSettings != null) {
            val isOAuth = (provisionSettings.incoming.authType === security.pEp.mdm.AuthType.XOAUTH2
                    && provisioningSettings.oAuthType != null)
            mOAuth2CheckBox.isChecked = isOAuth
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
            }
        }
        nextButton.setOnClickListener { attemptAutomaticSetup() }
    }

    private fun attemptAutomaticSetup() {
        val email = emailView.text?.toString() ?: error("Email missing")
        if (accountWasAlreadySet(email)) return
        if (uiState == UiState.PASSWORD_FLOW && clientCertificateCheckBox.isChecked) {
            // Auto-setup doesn't support client certificates.
            onManualSetup()
            return
        }
        showLoading(true)
        accountSetupBasicsViewModel.discoverMailSettingsAsync(
            email,
            if (uiState == UiState.EMAIL_ADDRESS_ONLY) oAuthProviderType
            else null
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putEnum(STATE_KEY_UI_STATE, uiState)
        outState.putString(EXTRA_ACCOUNT, account?.uuid)
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
        outState.putBoolean(WAS_LOADING, wasLoading)
    }

    private fun restoreScreenState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            uiState = savedInstanceState.getEnum(STATE_KEY_UI_STATE, UiState.EMAIL_ADDRESS_ONLY)

            val accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT)
            if (accountUuid != null) {
                account = preferences.getAccountAllowingIncomplete(accountUuid)
            }

            checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
            wasLoading = savedInstanceState.getBoolean(WAS_LOADING)
            updateViewVisibility(clientCertificateCheckBox.isChecked)
        }
    }

    private fun updateViewVisibility(usingCertificates: Boolean) {
        clientCertificateSpinner.isVisible = usingCertificates
        if (usingCertificates) {
            if (BuildConfig.IS_ENTERPRISE) {
                passwordLayout.isVisible = false
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

    private fun attemptAutoSetupUsingOnlyEmailAddress(connectionSettings: ConnectionSettings) {
        if (connectionSettings.incoming.authenticationType == AuthType.XOAUTH2 &&
            connectionSettings.outgoing.authenticationType == AuthType.XOAUTH2
        ) {
            startOAuthFlow(connectionSettings)
        } else {
            startPasswordFlow()
        }
    }

    private fun startOAuthFlow(connectionSettings: ConnectionSettings) {
        val account = createAccount(connectionSettings)
        account.mandatoryOAuthProviderType = oAuthProviderType
        val intent = OAuthFlowActivity.buildLaunchIntent(requireContext(), account.uuid)
        requireActivity().startActivityForResult(intent, REQUEST_CODE_OAUTH)
    }

    private fun startPasswordFlow() {
        uiState = UiState.PASSWORD_FLOW

        updateUi()
        validateFields()

        passwordView.requestFocus()
    }

    private fun finishAutoSetup(connectionSettings: ConnectionSettings) {
        val account = createAccount(connectionSettings)

        // Check incoming here. Then check outgoing in onActivityResult()
        actionCheckSettings(requireActivity(), account, CheckDirection.INCOMING)
    }

    private fun createAccount(connectionSettings: ConnectionSettings): Account {
        val email = emailView.text?.toString() ?: error("Email missing")
        val password = passwordView.text?.toString()

        val account = initAccount(email)

        val incomingServerSettings = connectionSettings.incoming.newPassword(password)
        account.storeUri = RemoteStore.createStoreUri(incomingServerSettings)

        val outgoingServerSettings = connectionSettings.outgoing.newPassword(password)
        account.transportUri = Transport.createTransportUri(outgoingServerSettings)
        account.deletePolicy = AccountCreator.getDefaultDeletePolicy(incomingServerSettings.type)


        setupFolderNames(incomingServerSettings.host.lowercase())

        return account
    }

    private fun initAccount(email: String): Account {
        val account = this.account ?: createAccount().also { this.account = it }

        account.name = getOwnerName()
        account.email = email
        return account
    }

    private fun createAccount(): Account {
        return preferences.newAccount()
    }

    private fun getOwnerName(): String {
        return preferences.defaultAccount?.name ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!isAdded) {
            return
        }
        showLoading(false)
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
        if (resultCode != Activity.RESULT_OK) return

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

    private fun checkSettings(direction: CheckDirection = CheckDirection.INCOMING) {
        actionCheckSettings(requireActivity(), account!!, direction)
    }

    private fun saveCredentialsInPreferences() {
        pEpUIArtefactCache.saveCredentialsInPreferences(
            emailView.text.toString(),
            passwordView.text.toString()
        )
    }

    override fun onStart() {
        super.onStart()
        accountSetupNavigator = (requireActivity() as AccountSetupBasics).accountSetupNavigator
        accountSetupNavigator.setCurrentStep(AccountSetupNavigator.Step.BASICS, account)
        restoreViewsEnabledState()
    }

    private fun restoreViewsEnabledState() {
        showLoading(wasLoading)
        wasLoading = false
    }

    private fun enableViewGroup(enable: Boolean, viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                enableViewGroup(enable, child)
            } else {
                child.isEnabled = enable
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        wasLoading = loading
        if (loading) {
            nextProgressBar.show()
            nextButton.isVisible = false
        } else {
            nextProgressBar.hide()
            nextButton.isVisible = true
        }
        accountSetupNavigator.isLoading = loading
        enableViewGroup(!loading, rootView as ViewGroup)
        manualSetupButton.isEnabled = !loading
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
        val accounts = preferences.accounts
        for (account in accounts) {
            if (account.email.equals(email, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun goForward() {
        showLoading(false)
        try {
            setupAccountType.setupStoreAndSmtpTransport(
                account,
                ServerSettings.Type.IMAP,
                "imap+ssl+"
            )
            accountSetupNavigator.goForward(parentFragmentManager, account, false)
        } catch (e: URISyntaxException) {
            Timber.e(e)
        }
    }

    private fun onManualSetup() {
        (activity as AccountSetupBasics?)!!.setManualSetupRequired(true)
        val email = emailView.text?.toString() ?: error("Email missing")
        if (accountWasAlreadySet(email)) {
            return
        }
        var password: String? = null
        var clientCertificateAlias: String? = null
        val authenticationType: AuthType

        val account = initAccount(email)

        val emailParts = splitEmail(email)
        val domain = emailParts[1]
        val imapHost = "mail.$domain"
        val smtpHost = "mail.$domain"
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
        account.name = getOwnerName()
        account.email = email

        // set default uris
        // NOTE: they will be changed again in AccountSetupAccountType!
        val storeServer = ServerSettings(
            ServerSettings.Type.IMAP,
            imapHost,
            -1,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType,
            email,
            password,
            clientCertificateAlias
        )
        val transportServer = ServerSettings(
            ServerSettings.Type.SMTP,
            smtpHost,
            -1,
            ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType,
            email,
            password,
            clientCertificateAlias
        )
        val storeUri = RemoteStore.createStoreUri(storeServer)
        val transportUri = Transport.createTransportUri(transportServer)
        account.storeUri = storeUri
        account.transportUri = transportUri
        setupFolderNames(domain)
        saveCredentialsInPreferences()
        goForward()
    }

    private fun setupFolderNames(domain: String?) {
        account!!.draftsFolderName = getString(R.string.special_mailbox_name_drafts)
        account!!.trashFolderName = getString(R.string.special_mailbox_name_trash)
        account!!.sentFolderName = getString(R.string.special_mailbox_name_sent)
        account!!.archiveFolderName = getString(R.string.special_mailbox_name_archive)

        // Yahoo! has a special folder for Spam, called "Bulk Mail".
        if (domain!!.endsWith(".yahoo.com")) {
            account!!.spamFolderName = "Bulk Mail"
        } else {
            account!!.spamFolderName = getString(R.string.special_mailbox_name_spam)
        }
    }

    private fun splitEmail(email: String): Array<String?> {
        val retParts = arrayOfNulls<String>(2)
        val emailParts = email.split("@").toTypedArray()
        retParts[0] = if (emailParts.isNotEmpty()) emailParts[0] else ""
        retParts[1] = if (emailParts.size > 1) emailParts[1] else ""
        return retParts
    }

    override fun injectFragment() {
        getpEpComponent().inject(this)
    }

    override fun onStop() {
        super.onStop()
        wasLoading = !nextButton.isVisible
        nextProgressBar.hide()
    }

    private enum class UiState {
        EMAIL_ADDRESS_ONLY,
        PASSWORD_FLOW
    }

    companion object {
        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 0
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val EXTRA_OAUTH_PROVIDER_TYPE = "com.fsck.k9.AccountSetupBasics.oAuthProviderType"
        private const val STATE_KEY_UI_STATE = "com.fsck.k9.AccountSetupBasics.uiState"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val REQUEST_CODE_CHECK_SETTINGS = AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
        private const val WAS_LOADING = "wasLoading"

        fun newInstance(oAuthProviderType: OAuthProviderType?): AccountSetupBasicsFragment {
            return AccountSetupBasicsFragment().apply {
                oAuthProviderType?.let {oAuthProviderType ->
                    arguments = bundleOf(
                        EXTRA_OAUTH_PROVIDER_TYPE to
                                oAuthProviderType.toString()
                    )
                }
            }
        }
    }
}
