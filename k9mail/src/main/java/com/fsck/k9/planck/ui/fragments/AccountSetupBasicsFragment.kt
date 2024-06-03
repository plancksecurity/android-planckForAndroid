package com.fsck.k9.planck.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.view.isVisible
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.EmailAddressValidator
import com.fsck.k9.R
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.databinding.FragmentAccountLoginBinding
import com.fsck.k9.databinding.WizardSetupBinding
import com.fsck.k9.helper.SimpleTextWatcher
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.ui.ConnectionSettings
import com.fsck.k9.planck.ui.tools.AccountSetupNavigator
import com.fsck.k9.planck.ui.tools.FeedbackTools
import com.fsck.k9.view.ClientCertificateSpinner
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountSetupBasicsFragment : AccountSetupBasicsFragmentBase() {

    private var _binding: FragmentAccountLoginBinding? = null
    private val binding get() = _binding!!
    private var _wizardSetupBinding: WizardSetupBinding? = null
    private val wizardSetupBinding get() = _wizardSetupBinding!!

    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var advancedOptionsContainer: View
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var passwordLayout: View
    private lateinit var pEpUIArtefactCache: PlanckUIArtefactCache

    @Inject
    lateinit var emailValidator: EmailAddressValidator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        toolbarCustomizer.setDefaultStatusBarColor()
        _binding = FragmentAccountLoginBinding.inflate(inflater, container, false)
        _wizardSetupBinding = WizardSetupBinding.bind(binding.root)
        setupViews()
        setupToolbar(!requireActivity().isTaskRoot
                || parentFragmentManager.backStackEntryCount > 1)

        initializeViewListeners()
        validateFields()
        pEpUIArtefactCache = PlanckUIArtefactCache.getInstance(requireContext().applicationContext)
        val email = pEpUIArtefactCache.emailInPreferences
        val password = pEpUIArtefactCache.passwordInPreferences
        if (email != null && password != null) {
            emailView.setText(email)
            passwordView.setText(password)
        }
        if (k9.isRunningOnWorkProfile) {
            updateUiFromProvisioningSettings()
        }
        setHasOptionsMenu(!BuildConfig.IS_OFFICIAL)
        return binding.root
    }

    private fun setupViews() {
        emailView = binding.accountEmail
        passwordView = binding.accountPassword
        clientCertificateCheckBox = binding.accountClientCertificate
        clientCertificateSpinner = binding.accountClientCertificateSpinner
        advancedOptionsContainer = binding.foldableAdvancedOptions
        nextButton = wizardSetupBinding.next
        manualSetupButton = wizardSetupBinding.manualSetup
        passwordLayout = binding.accountPasswordLayout

        manualSetupButton.setOnClickListener { onManualSetup(true) }
        nextButton.setOnClickListener { attemptAutoSetup() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AccountSetupBasics).configurePasswordFlowScreen()
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
        emailView.setText(accountProvisioningSettings?.email)
        emailView.isFocusable = false
        val provisionSettings = accountProvisioningSettings?.provisionedMailSettings
        if (provisionSettings != null) {
            val isExternalAuth =
                provisionSettings.incoming.authType === security.planck.mdm.AuthType.EXTERNAL
            clientCertificateCheckBox.isChecked = isExternalAuth
        }
        manualSetupButton.visibility = View.INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.account_setup_basic_option, menu)
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
            if (BuildConfig.IS_OFFICIAL) {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _wizardSetupBinding = null
    }

    companion object {
        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 0
    }
}