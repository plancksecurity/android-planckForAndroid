package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import butterknife.OnTextChanged
import com.fsck.k9.*
import com.fsck.k9.account.AccountCreator
import com.fsck.k9.activity.setup.AccountSetupBasics
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.Companion.actionCheckSettings
import com.fsck.k9.activity.setup.AccountSetupNames
import com.fsck.k9.activity.setup.OAuthFlowActivity.Companion.buildLaunchIntent
import com.fsck.k9.helper.UrlEncodingHelper
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.pEp.PePUIArtefactCache
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.pEp.ui.tools.SetupAccountType
import com.fsck.k9.view.ClientCertificateSpinner
import com.fsck.k9.view.ClientCertificateSpinner.OnClientCertificateChangedListener
import security.pEp.provisioning.ProvisioningSettings
import security.pEp.provisioning.SimpleMailSettings
import timber.log.Timber
import java.io.Serializable
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

class AccountSetupBasicsFragment : PEpFragment(), View.OnClickListener, TextWatcher,
    CompoundButton.OnCheckedChangeListener, OnClientCertificateChangedListener {
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var clientCertificateCheckBox: CheckBox
    private lateinit var clientCertificateSpinner: ClientCertificateSpinner
    private lateinit var oAuth2CheckBox: CheckBox
    private lateinit var nextButton: Button
    private lateinit var manualSetupButton: Button
    private lateinit var passwordLayout: View
    private var account: Account? = null
    private var provider: Provider? = null
    private val emailValidator = EmailAddressValidator()
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
        oAuth2CheckBox = rootView.findViewById(R.id.account_oauth2)
        nextButton = rootView.findViewById(R.id.next)
        manualSetupButton = rootView.findViewById(R.id.manual_setup)
        nextButton.setOnClickListener(this)
        manualSetupButton.setOnClickListener(this)
        passwordLayout = rootView.findViewById(R.id.account_password_layout)
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

    private fun updateUiFromProvisioningSettings() {
        emailView.setText(provisioningSettings.email)
        emailView.isFocusable = false
        val provisionSettings = provisioningSettings.provisionedMailSettings
        if (provisionSettings != null) {
            val isOAuth = (provisionSettings.incoming.authType === security.pEp.mdm.AuthType.XOAUTH2
                    && provisioningSettings.oAuthType != null)
            oAuth2CheckBox.isChecked = isOAuth
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
        emailView.addTextChangedListener(this)
        passwordView.addTextChangedListener(this)
        clientCertificateCheckBox.setOnCheckedChangeListener(this)
        clientCertificateSpinner.setOnClientCertificateChangedListener(this)
        clientCertificateSpinner.setOnClientCertificateChangedListener(this)
        oAuth2CheckBox.setOnCheckedChangeListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (account != null) {
            outState.putString(EXTRA_ACCOUNT, account!!.uuid)
        }
        if (provider != null) {
            outState.putSerializable(STATE_KEY_PROVIDER, provider)
        }
        outState.putBoolean(STATE_KEY_CHECKED_INCOMING, checkedIncoming)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_ACCOUNT)) {
                val accountUuid = savedInstanceState.getString(EXTRA_ACCOUNT)
                account =
                    Preferences.getPreferences(activity).getAccountAllowingIncomplete(accountUuid)
            }
            if (savedInstanceState.containsKey(STATE_KEY_PROVIDER)) {
                provider = savedInstanceState.getSerializable(STATE_KEY_PROVIDER) as Provider?
            }
            checkedIncoming = savedInstanceState.getBoolean(STATE_KEY_CHECKED_INCOMING)
            updateViewVisibility(
                clientCertificateCheckBox.isChecked,
                oAuth2CheckBox.isChecked
            )
        }
    }

    override fun afterTextChanged(s: Editable) {
        validateFields()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    override fun onClientCertificateChanged(alias: String) {
        validateFields()
    }

    /**
     * Called when checking the client certificate CheckBox
     */
    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        updateViewVisibility(clientCertificateCheckBox.isChecked, oAuth2CheckBox.isChecked)
        validateFields()

        // Have the user select (or confirm) the client certificate
        if (buttonView == clientCertificateCheckBox && isChecked) {
            oAuth2CheckBox.isChecked = false
            clientCertificateSpinner.chooseCertificate()
        } else if (buttonView == oAuth2CheckBox && isChecked) {
            clientCertificateCheckBox.isChecked = false
        }
    }

    private fun updateViewVisibility(usingCertificates: Boolean, usingXoauth: Boolean) {
        if (usingCertificates) {
            clientCertificateSpinner.visibility = View.VISIBLE
            if (k9.isRunningOnWorkProfile) {
                passwordLayout.visibility = View.GONE
            }
        } else if (usingXoauth) {
            clientCertificateSpinner.visibility = View.GONE
            emailView.visibility = View.VISIBLE
            passwordLayout.visibility = View.GONE
        } else {
            // show username & password fields, hide client certificate spinner
            emailView.visibility = View.VISIBLE
            passwordLayout.visibility = View.VISIBLE
            clientCertificateSpinner.visibility = View.GONE
            clientCertificateCheckBox.isEnabled = true
        }
    }

    private fun validateFields() {
        val clientCertificateAlias = clientCertificateSpinner.alias
        val email = emailView.text.toString().trim()
        val clientCertificateChecked = clientCertificateCheckBox.isChecked
        val oauth2Checked = oAuth2CheckBox.isChecked
        val emailValid =
            Utility.requiredFieldValid(emailView) && emailValidator.isValidAddressOnly(email)
        val passwordValid = Utility.requiredFieldValid(passwordView)
        val oauth2 = oauth2Checked &&
                emailValid
        val certificateAuth =
            clientCertificateChecked && clientCertificateAlias != null && emailValid
        val emailPasswordAuth = (!oauth2Checked
                && !certificateAuth
                && !clientCertificateChecked
                && emailValid
                && passwordValid)
        val valid = oauth2 || certificateAuth || emailPasswordAuth
        nextButton.isEnabled = valid
        manualSetupButton.isEnabled = valid
        /*
         * Dim the next button's icon to 50% if the button is disabled.
         * TODO this can probably be done with a stateful drawable. Check into it.
         * android:state_enabled
         */Utility.setCompoundDrawablesAlpha(nextButton, if (nextButton.isEnabled) 255 else 128)
    }

    @OnTextChanged(R.id.account_email)
    fun onEmailChanged() {
        validateFields()
    }

    @OnTextChanged(R.id.account_password)
    fun onPasswordChanged() {
        validateFields()
    }

    private val ownerName: String?
        get() {
            var name: String? = null
            if (k9.isRunningOnWorkProfile) {
                if (provisioningSettings.senderName != null) {
                    name = provisioningSettings.senderName
                }
            } else {
                try {
                    name = defaultAccountName
                } catch (e: Exception) {
                    Log.e(K9.LOG_TAG, "Could not get default account name", e)
                }
                if (name == null) {
                    name = ""
                }
            }
            return name
        }
    private val defaultAccountName: String?
        get() {
            var name: String? = null
            val account = Preferences.getPreferences(activity).defaultAccount
            if (account != null) {
                name = account.name
            }
            return name
        }

    private fun onCreateDialog(id: Int): Dialog? {
        if (id == DIALOG_NOTE) {
            if (provider != null && provider!!.note != null) {
                return AlertDialog.Builder(requireActivity())
                    .setMessage(provider!!.note)
                    .setPositiveButton(
                        getString(R.string.okay_action)
                    ) { _, _ -> finishAutoSetup() }
                    .setNegativeButton(
                        getString(R.string.cancel_action),
                        null
                    )
                    .create()
            }
        }
        return null
    }

    private fun finishAutoSetup() {
        val email = emailView.text.toString().trim()
        val password = passwordView.text.toString()
        val emailParts = splitEmail(email)
        val user = emailParts[0]
        val domain = emailParts[1]
        val usingXOAuth2 = isOAuth(domain)
        try {
            val userEnc = UrlEncodingHelper.encodeUtf8(user)
            val passwordEnc = UrlEncodingHelper.encodeUtf8(password)
            var incomingUsername = provider!!.incomingUsernameTemplate
            incomingUsername = incomingUsername!!.replace("\\\$email".toRegex(), email)
            incomingUsername = incomingUsername.replace("\\\$user".toRegex(), userEnc)
            incomingUsername = incomingUsername.replace("\\\$domain".toRegex(), domain!!)
            val incomingUriTemplate = provider!!.incomingUriTemplate
            var port = RemoteStore.decodeStoreUri(incomingUriTemplate.toString()).port
            var incomingUserInfo = "$incomingUsername:$passwordEnc"
            if (usingXOAuth2) incomingUserInfo =
                AuthType.XOAUTH2.toString() + ":" + incomingUserInfo
            val incomingUri = URI(
                incomingUriTemplate!!.scheme, incomingUserInfo,
                incomingUriTemplate.host, port,
                null, null, null
            )
            var outgoingUsername = provider!!.outgoingUsernameTemplate
            val outgoingUriTemplate = provider!!.outgoingUriTemplate
            port = Transport.decodeTransportUri(outgoingUriTemplate.toString()).port
            val outgoingUri: URI
            if (outgoingUsername != null) {
                outgoingUsername = outgoingUsername.replace("\\\$email".toRegex(), email)
                outgoingUsername = outgoingUsername.replace("\\\$user".toRegex(), userEnc)
                outgoingUsername = outgoingUsername.replace("\\\$domain".toRegex(), domain)
                var outgoingUserInfo = "$outgoingUsername:$passwordEnc"
                if (usingXOAuth2) {
                    outgoingUserInfo = outgoingUserInfo + ":" + AuthType.XOAUTH2
                }
                outgoingUri = URI(
                    outgoingUriTemplate!!.scheme, outgoingUserInfo,
                    outgoingUriTemplate.host, port, null,
                    null, null
                )
            } else {
                outgoingUri = URI(
                    outgoingUriTemplate!!.scheme,
                    null, outgoingUriTemplate.host, port, null,
                    null, null
                )
            }
            initializeAccount()
            account!!.email = email
            account!!.name = ownerName
            if (k9.isRunningOnWorkProfile && account!!.name == null) {
                account!!.name = account!!.email
            }
            if (k9.isRunningOnWorkProfile) {
                account!!.description =
                    if (!Utility.isNullOrBlank(provisioningSettings.accountDescription))
                        provisioningSettings.accountDescription
                    else account!!.email
            }
            account!!.storeUri = incomingUri.toString()
            account!!.transportUri = outgoingUri.toString()
            setupFolderNames(incomingUriTemplate.host.lowercase())
            val incomingSettings = RemoteStore.decodeStoreUri(incomingUri.toString())
            val outgoingSettings = Transport.decodeTransportUri(outgoingUri.toString())
            account!!.deletePolicy = AccountCreator.getDefaultDeletePolicy(incomingSettings!!.type)
            saveCredentialsInPreferences()
            if (incomingSettings.authenticationType == AuthType.XOAUTH2 && outgoingSettings.authenticationType == AuthType.XOAUTH2) {
                startOAuthFlow()
            } else {
                checkSettings()
            }

            // Check incoming here.  Then check outgoing in onActivityResult()
        } catch (use: URISyntaxException) {
            /*
             * If there is some problem with the URI we give up and go on to
             * manual setup.
             */
            onManualSetup()
        }
    }

    private fun isOAuth(domain: String?): Boolean {
        return oAuth2CheckBox.isChecked || domain.equals(GMAIL_DOMAIN, ignoreCase = true)
    }

    private fun startOAuthFlow() {
        val intent = buildLaunchIntent(requireContext(), account!!.uuid)
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

    private fun onNext() {
        val email = emailView.text.toString().trim()
        // TODO: 9/8/22 REVIEW/RENAME THIS METHOD ISAVALIDADDRESS
        if (isAValidAddress(email)) return
        setup(email)
    }

    private fun isAValidAddress(email: String): Boolean {
        return avoidAddingAlreadyExistingAccount(email) ||
                isEmailNull(email)
    }

    private fun isEmailNull(email: String?): Boolean {
        if (email == null || email.isEmpty()) {
            resetView("You must enter an email address")
            return true
        }
        return false
    }

    private fun getProvisionedProvider(domain: String?): Provider? {
        try {
            val (incoming, outgoing) = provisioningSettings.provisionedMailSettings ?: return null
            val provider = Provider()
            provider.domain = domain
            provider.id = "provisioned"
            provider.label = "enterprise provisioned provider"
            provider.incomingUriTemplate = getServerUriTemplate(
                incoming,
                false
            )
            provider.outgoingUriTemplate = getServerUriTemplate(
                outgoing,
                true
            )
            provider.incomingUsernameTemplate = incoming.userName
            provider.outgoingUsernameTemplate = outgoing.userName
            return provider
        } catch (ex: Exception) {
            Timber.e(
                ex,
                "%s: Error while trying to parse provisioned provider.",
                K9.LOG_TAG
            )
        }
        return null
    }

    @Throws(URISyntaxException::class)
    private fun getServerUriTemplate(
        settings: SimpleMailSettings,
        outgoing: Boolean
    ): URI {
        val uri = (if (outgoing) "smtp" else "imap") +
                "+" +
                settings.getConnectionSecurityString() +
                "+" +
                "://" +
                settings.server +
                ":" +
                settings.port
        return URI(uri)
    }

    private fun setup(email: String) {
        if (clientCertificateCheckBox.isChecked) {
            // Auto-setup doesn't support client certificates.
            onManualSetup()
            return
        }
        val emailParts = splitEmail(email)
        val domain = emailParts[1]
        provider = findProviderForDomain(domain)
        if (provider == null) {
            /*
             * We don't have default settings for this account, start the manual
             * setup process.
             */
            onManualSetup()
            return
        }
        Log.i(K9.LOG_TAG, "Provider found, using automatic set-up")
        if (provider!!.note != null) {
            onCreateDialog(DIALOG_NOTE)
        } else {
            finishAutoSetup()
        }
    }

    private fun avoidAddingAlreadyExistingAccount(email: String): Boolean {
        if (accountAlreadyExists(email)) {
            resetView(getString(R.string.account_already_exists))
            return true
        }
        return false
    }

    private fun resetView(feedback: String) {
        FeedbackTools.showLongFeedback(view, feedback)
        nextButton.visibility = View.VISIBLE
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
            (activity as AccountSetupBasics?)!!.onImport(data!!.data)
        } else if (requestCode == AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE) {
            handleCheckSettingsResult(resultCode)
        } else if (requestCode == REQUEST_CODE_OAUTH) {
            handleSignInResult(resultCode)
        } /*else {
            super.onActivityResult(requestCode, resultCode, data);
        }*/
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

    private fun onManualSetup() {
        (requireActivity() as AccountSetupBasics).setManualSetupRequired(true)
        val email = emailView.text.toString().trim()
        if (isAValidAddress(email)) return
        val emailParts = splitEmail(email)
        val domain = emailParts[1]
        var password: String? = null
        var clientCertificateAlias: String? = null
        val authenticationType: AuthType
        var imapHost = "mail.$domain"
        var smtpHost = "mail.$domain"
        if (clientCertificateCheckBox.isChecked) {
            if (passwordView.text.toString().trim().isEmpty()) {
                authenticationType = AuthType.EXTERNAL
            } else {
                authenticationType = AuthType.EXTERNAL_PLAIN
                password = passwordView.text.toString()
            }
            clientCertificateAlias = clientCertificateSpinner.alias
        } else if (oAuth2CheckBox.isChecked) {
            authenticationType = AuthType.XOAUTH2
            imapHost = "imap.gmail.com"
            smtpHost = "smtp.gmail.com"
        } else {
            authenticationType = AuthType.PLAIN
            password = passwordView.text.toString()
        }
        initializeAccount()
        account!!.name = ownerName
        account!!.email = email

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
        account!!.storeUri = storeUri
        account!!.transportUri = transportUri
        setupFolderNames(domain)
        saveCredentialsInPreferences()
        goForward()
    }

    private fun initializeAccount() {
        if (account == null || Preferences.getPreferences(activity).getAccountAllowingIncomplete(
                account!!.uuid
            ) == null
        ) {
            account = Preferences.getPreferences(activity).newAccount()
        }
    }

    private fun setupFolderNames(domain: String?) {
        account?.let { account ->
            account.draftsFolderName = getString(R.string.special_mailbox_name_drafts)
            account.trashFolderName = getString(R.string.special_mailbox_name_trash)
            account.sentFolderName = getString(R.string.special_mailbox_name_sent)
            account.archiveFolderName = getString(R.string.special_mailbox_name_archive)

            // Yahoo! has a special folder for Spam, called "Bulk Mail".
            if (domain?.endsWith(".yahoo.com") == true) {
                account.spamFolderName = "Bulk Mail"
            } else {
                account.spamFolderName = getString(R.string.special_mailbox_name_spam)
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.next -> onNext()
            R.id.manual_setup -> onManualSetup()
        }
    }

    /**
     * Attempts to get the given attribute as a String resource first, and if it fails
     * returns the attribute as a simple String value.
     *
     * @param xml
     * @param name
     * @return
     */
    private fun getXmlAttribute(xml: XmlResourceParser, name: String): String {
        val resId = xml.getAttributeResourceValue(null, name, 0)
        return if (resId == 0) {
            xml.getAttributeValue(null, name)
        } else {
            getString(resId)
        }
    }

    private fun findProviderForDomain(domain: String?): Provider? {
        var provider = getProvisionedProvider(domain)
        if (provider != null) {
            return provider
        }
        try {
            val xml = resources.getXml(R.xml.providers)
            var xmlEventType: Int
            provider = null
            while (xml.next().also { xmlEventType = it } != XmlResourceParser.END_DOCUMENT) {
                if (xmlEventType == XmlResourceParser.START_TAG && "provider" == xml.name && domain.equals(
                        getXmlAttribute(xml, "domain"),
                        ignoreCase = true
                    )
                ) {
                    provider = Provider()
                    provider.id = getXmlAttribute(xml, "id")
                    provider.label = getXmlAttribute(xml, "label")
                    provider.domain = getXmlAttribute(xml, "domain")
                    provider.note = getXmlAttribute(xml, "note")
                } else if (xmlEventType == XmlResourceParser.START_TAG && "incoming" == xml.name && provider != null) {
                    provider.incomingUriTemplate = URI(getXmlAttribute(xml, "uri"))
                    provider.incomingUsernameTemplate = getXmlAttribute(xml, "username")
                } else if (xmlEventType == XmlResourceParser.START_TAG && "outgoing" == xml.name && provider != null) {
                    provider.outgoingUriTemplate = URI(getXmlAttribute(xml, "uri"))
                    provider.outgoingUsernameTemplate = getXmlAttribute(xml, "username")
                } else if (xmlEventType == XmlResourceParser.END_TAG && "provider" == xml.name && provider != null) {
                    return provider
                }
            }
        } catch (e: Exception) {
            Log.e(K9.LOG_TAG, "Error while trying to load provider settings.", e)
        }
        return null
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

    internal class Provider : Serializable {
        var id: String? = null
        var label: String? = null
        var domain: String? = null
        var incomingUriTemplate: URI? = null
        var incomingUsernameTemplate: String? = null
        var outgoingUriTemplate: URI? = null
        var outgoingUsernameTemplate: String? = null
        var note: String? = null

        companion object {
            private const val serialVersionUID = 8511656164616538989L
        }
    }

    companion object {
        private const val ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 0
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val DIALOG_NOTE = 1
        private const val STATE_KEY_PROVIDER = "com.fsck.k9.AccountSetupBasics.provider"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_OAUTH = Activity.RESULT_FIRST_USER + 1
        private const val GMAIL_DOMAIN = "gmail.com"
    }
}