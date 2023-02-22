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
import com.fsck.k9.helper.SimpleTextWatcher
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.view.ClientCertificateSpinner
import org.koin.android.ext.android.inject

class AccountSetupBasicsFragment : AccountSetupBasicsFragmentBase() {

    private val emailValidator: EmailAddressValidator by (this as ComponentCallbacks).inject()

    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var advancedOptionsContainer: View
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var passwordLayout: View
    private lateinit var rootView: View
    private lateinit var pEpUIArtefactCache: PePUIArtefactCache

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
        manualSetupButton.setOnClickListener { onManualSetup(true) }
        nextButton.setOnClickListener { attemptAutoSetup() }

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
        navigator.setCurrentStep(AccountSetupNavigator.Step.BASICS, account)

        restoreScreenState(savedInstanceState)
        /*
         * We wait until now to initialize the listeners because we didn't want the OnCheckedChangeListener active
         * while the clientCertificateCheckBox state was being restored because it could trigger the pop-up of a
         * ClientCertificateSpinner.chooseCertificate() dialog.
         */
        initializeViewListeners()
        validateFields()
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
                validateFields()
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

    private fun restoreScreenState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
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

    private fun validateFields() {
        val email = emailView.text?.toString().orEmpty()
        val valid = Utility.requiredFieldValid(emailView) && emailValidator.isValidAddressOnly(email) &&
                isPasswordFieldValid()

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

    private fun saveCredentialsInPreferences() {
        pEpUIArtefactCache.saveCredentialsInPreferences(
            emailView.text.toString(),
            passwordView.text.toString()
        )
    }

    override fun onResume() {
        super.onResume()
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
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onManualSetup(fromUser: Boolean) {
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
        account.setMailSettings(requireContext(), connectionSettings, false)
    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    companion object {
        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 0
    }
}