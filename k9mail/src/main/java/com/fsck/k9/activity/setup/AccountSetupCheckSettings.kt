package com.fsck.k9.activity.setup

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.activity.observe
import com.fsck.k9.fragment.ConfirmationDialogFragment
import com.fsck.k9.fragment.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.filter.Hex
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.pEp.infrastructure.exceptions.DeviceOfflineException
import org.koin.android.architecture.ext.viewModel
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*

/**
 * Checks the given settings to make sure that they can be used to send and receive mail.
 *
 * XXX NOTE: The manifest for this app has it ignore config changes, because it doesn't correctly deal with restarting
 * while its thread is running.
 */
class AccountSetupCheckSettings : K9Activity(), ConfirmationDialogFragmentListener {
    private val authViewModel: AuthViewModel by viewModel()
    private val checkSettingsViewModel: CheckSettingsViewModel by viewModel()
    private val preferences: Preferences by inject()
    //private val localKeyStoreManager: LocalKeyStoreManager by inject()

    private val handler = Handler(Looper.myLooper()!!)

    private lateinit var progressBar: ProgressBar
    private lateinit var messageView: TextView

    private lateinit var account: Account
    private lateinit var direction: CheckDirection
    private var errorResultCode = RESULT_CANCELED
    private var edit = false

    @Volatile
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            errorResultCode = savedInstanceState.getInt(STATE_ERROR_RESULT_CODE, RESULT_CANCELED)
        }
        setContentView(R.layout.account_setup_check_settings)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
            ?: error("K9 layouts must provide a toolbar with id='toolbar'.")

        setSupportActionBar(toolbar)

        direction = intent.getSerializableExtra(EXTRA_CHECK_DIRECTION) as CheckDirection?
            ?: error("Missing CheckDirection")
        val mailSettingsDiscoveryRequired = intent.getBooleanExtra(
            EXTRA_MAIL_SETTINGS_DISCOVERY_REQUIRED, false)
                && direction == CheckDirection.INCOMING
        edit = intent.getBooleanExtra(EXTRA_EDIT, false)

        observeCheckSettingsViewModel()

        authViewModel.init(activityResultRegistry, lifecycle)

        authViewModel.uiState.observe(this) { state ->
            when (state) {
                AuthFlowState.Idle -> {
                    return@observe
                }
                AuthFlowState.Success -> {
                    if (authViewModel.needsMailSettingsDiscovery && mailSettingsDiscoveryRequired) { // ONLY CASE OF PASSWORD FLOW THAT ACTUALLY NEEDS OAUTH (GOOGLE)
                        discoverMailSettings()
                    } else {
                        startCheckServerSettings()
                    }
                }
                AuthFlowState.Canceled -> {
                    showErrorDialog(R.string.account_setup_failed_dlg_oauth_flow_canceled)
                }
                is AuthFlowState.Failed -> {
                    showErrorDialog(R.string.account_setup_failed_dlg_oauth_flow_failed, state)
                }
                AuthFlowState.NotSupported -> {
                    showErrorDialog(R.string.account_setup_failed_dlg_oauth_not_supported)
                }
                AuthFlowState.BrowserNotFound -> {
                    showErrorDialog(R.string.account_setup_failed_dlg_browser_not_found)
                }
                is AuthFlowState.WrongEmailAddress -> {
                    showErrorDialog(R.string.account_setup_failed_dlg_oauth_wrong_email_address, state.userWrongEmail, state.adminEmail)
                }
            }

            authViewModel.authResultConsumed()
        }

        observeMailSettingsDiscoverResult()

        messageView = findViewById(R.id.message)
        progressBar = findViewById(R.id.progress)
        findViewById<View>(R.id.cancel).setOnClickListener { onCancel() }

        setMessage(R.string.account_setup_check_settings_retr_info_msg)
        progressBar.isIndeterminate = true

        val accountUuid = intent.getStringExtra(EXTRA_ACCOUNT) ?: error("Missing account UUID")
        account = preferences.getAccountAllowingIncomplete(accountUuid) ?: error("Could not find account")

        if (savedInstanceState == null) {
            if (mailSettingsDiscoveryRequired) {
                discoverMailSettings()
            } else {
                startLoginOrSettingsCheck()
            }
        }
    }

    private fun observeCheckSettingsViewModel() {
        checkSettingsViewModel.state.observe(this) { checkSettingsState ->
            when(checkSettingsState) {
                CheckSettingsState.CheckingIncoming ->
                    setMessage(R.string.account_setup_check_settings_check_incoming_msg)
                CheckSettingsState.CheckingOutgoing ->
                    setMessage(R.string.account_setup_check_settings_check_outgoing_msg)
                CheckSettingsState.Success -> {
                    setResult(RESULT_OK)
                    finish()
                }
                is CheckSettingsState.Error -> {
                    handleCheckSettingsError(checkSettingsState.throwable)
                }
                else -> {
                    // NOP
                }
            }
        }
    }

    private fun handleCheckSettingsError(throwable: Throwable) {
        when (throwable) {
            is AuthenticationFailedException -> {
                showErrorDialog(
                    R.string.account_setup_failed_dlg_auth_message_fmt,
                    throwable.message.orEmpty()
                )
            }
            is CertificateValidationException -> {
                handleCertificateValidationException(throwable)
            }
            is DeviceOfflineException -> {
                showErrorDialog(R.string.device_offline_warning)
            }
            else -> {
                showErrorDialog(
                    R.string.account_setup_failed_dlg_server_message_fmt,
                    throwable.message.orEmpty()
                )
            }
        }
    }

    private fun observeMailSettingsDiscoverResult() {
        authViewModel.connectionSettings.observe(this) { event ->
            if (event.isReady && !event.hasBeenHandled) {
                val connectionSettings = event.getContent()
                if (connectionSettings == null) {
                    errorResultCode = RESULT_CODE_MANUAL_SETUP_NEEDED
                    showErrorDialog(R.string.account_setup_failed_dlg_could_not_discover_mail_settings)
                } else {
                    authViewModel.discoverMailSettingsSuccess()
                    account.setMailSettings(this, connectionSettings, true)
                    startLoginOrSettingsCheck()
                }
            }
        }
    }

    private fun startLoginOrSettingsCheck() {
        if (needsAuthorization()) {
            login()
        } else {
            startCheckServerSettings()
        }
    }

    private fun discoverMailSettings() {
        setMessage(R.string.account_setup_check_settings_retr_info_msg)
        progressBar.isIndeterminate = true
        authViewModel.discoverMailSettingsAsync(account.email, account.mandatoryOAuthProviderType)
    }

    private fun login() {
        setMessage(R.string.account_setup_check_settings_authenticate)
        authViewModel.login(account)
    }

    private fun needsAuthorization(): Boolean {
        return (
                account.mandatoryOAuthProviderType != null ||
                (account.storeUri != null
                        && RemoteStore.decodeStoreUri(account.storeUri).authenticationType == AuthType.XOAUTH2
                        && account.transportUri != null
                        && Transport.decodeTransportUri(account.transportUri).authenticationType == AuthType.XOAUTH2)
                ) && !authViewModel.isAuthorized(account)
    }

    private fun startCheckServerSettings() {
        checkSettingsViewModel.start(applicationContext, account, direction, edit)
    }

    private fun handleCertificateValidationException(exception: CertificateValidationException) {
        Timber.e(exception, "Error while testing settings")

        val chain = exception.certChain

        // Avoid NullPointerException in acceptKeyDialog()
        if (chain != null) {
            acceptKeyDialog(
                R.string.account_setup_failed_dlg_certificate_message_fmt,
                exception
            )
        } else {
            showErrorDialog(
                R.string.account_setup_failed_dlg_server_message_fmt,
                errorMessageForCertificateException(exception)!!
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        destroyed = true
    }

    private fun setMessage(resId: Int) {
        messageView.text = getString(resId)
    }

    private fun acceptKeyDialog(msgResId: Int, exception: CertificateValidationException) {
        handler.post {
            if (destroyed) {
                return@post
            }

            val errorMessage =
                exception.cause?.cause?.message ?: exception.cause?.message ?: exception.message

            progressBar.isIndeterminate = false

            val chainInfo = StringBuilder()
            val chain = exception.certChain

            // We already know chain != null (tested before calling this method)
            for (i in chain.indices) {
                // display certificate chain information
                // TODO: localize this strings
                chainInfo.append("Certificate chain[").append(i).append("]:\n")
                chainInfo.append("Subject: ").append(chain[i].subjectDN.toString()).append("\n")

                // display SubjectAltNames too
                // (the user may be mislead into mistrusting a certificate
                //  by a subjectDN not matching the server even though a
                //  SubjectAltName matches)
                try {
                    val subjectAlternativeNames = chain[i].subjectAlternativeNames
                    if (subjectAlternativeNames != null) {
                        // TODO: localize this string
                        val altNamesText = StringBuilder()
                        altNamesText.append("Subject has ")
                            .append(subjectAlternativeNames.size)
                            .append(" alternative names\n")

                        // we need these for matching
                        val incomingServerHost = RemoteStore.decodeStoreUri(account.storeUri).host!!
                        val outgoingServerHost =
                            Transport.decodeTransportUri(account.transportUri).host!!
                        for (subjectAlternativeName in subjectAlternativeNames) {
                            val type = subjectAlternativeName[0] as Int
                            val value: Any? = subjectAlternativeName[1]
                            val name: String = when (type) {
                                0 -> {
                                    Timber.w("SubjectAltName of type OtherName not supported.")
                                    continue
                                }
                                1 -> value as String
                                2 -> value as String
                                3 -> {
                                    Timber.w("unsupported SubjectAltName of type x400Address")
                                    continue
                                }
                                4 -> {
                                    Timber.w("unsupported SubjectAltName of type directoryName")
                                    continue
                                }
                                5 -> {
                                    Timber.w("unsupported SubjectAltName of type ediPartyName")
                                    continue
                                }
                                6 -> value as String
                                7 -> value as String
                                else -> {
                                    Timber.w("unsupported SubjectAltName of unknown type")
                                    continue
                                }
                            }

                            // if some of the SubjectAltNames match the store or transport -host, display them
                            if (name.equals(incomingServerHost, ignoreCase = true) ||
                                name.equals(outgoingServerHost, ignoreCase = true)
                            ) {
                                // TODO: localize this string
                                altNamesText.append("Subject(alt): ").append(name).append(",...\n")
                            } else if (name.startsWith("*.") &&
                                (
                                        incomingServerHost.endsWith(name.substring(2)) ||
                                                outgoingServerHost.endsWith(name.substring(2))
                                        )
                            ) {
                                // TODO: localize this string
                                altNamesText.append("Subject(alt): ").append(name).append(",...\n")
                            }
                        }
                        chainInfo.append(altNamesText)
                    }
                } catch (e: Exception) {
                    // don't fail just because of subjectAltNames
                    Timber.w(e, "cannot display SubjectAltNames in dialog")
                }

                chainInfo.append("Issuer: ").append(chain[i].issuerDN.toString()).append("\n")
                for (algorithm in arrayOf("SHA-1", "SHA-256", "SHA-512")) {
                    val digest = try {
                        MessageDigest.getInstance(algorithm)
                    } catch (e: NoSuchAlgorithmException) {
                        Timber.e(e, "Error while initializing MessageDigest ($algorithm)")
                        null
                    }

                    if (digest != null) {
                        digest.reset()
                        try {
                            val hash = Hex.encodeHex(digest.digest(chain[i].encoded))
                            chainInfo.append("Fingerprint ($algorithm): ").append("\n").append(hash)
                                .append("\n")
                        } catch (e: CertificateEncodingException) {
                            Timber.e(e, "Error while encoding certificate")
                        }
                    }
                }
            }

            // TODO: refactor with DialogFragment.
            // This is difficult because we need to pass through chain[0] for onClick()
            AlertDialog.Builder(this@AccountSetupCheckSettings)
                .setTitle(getString(R.string.account_setup_failed_dlg_invalid_certificate_title))
                .setMessage(getString(msgResId, errorMessage) + " " + chainInfo.toString())
                .setCancelable(true)
                .setPositiveButton(R.string.account_setup_failed_dlg_invalid_certificate_accept) { _, _ ->
                    acceptCertificate(chain[0])
                }
                .setNegativeButton(R.string.account_setup_failed_dlg_invalid_certificate_reject) { _, _ ->
                    finish()
                }
                .show()
        }
    }

    /**
     * Permanently accepts a certificate for the INCOMING or OUTGOING direction by adding it to the local key store.
     */
    private fun acceptCertificate(certificate: X509Certificate) {
        try {
            account.addCertificate(direction, certificate)
        } catch (e: CertificateException) {
            showErrorDialog(
                R.string.account_setup_failed_dlg_certificate_message_fmt,
                e.message.orEmpty()
            )
        }

        actionCheckSettings(this@AccountSetupCheckSettings, account, direction, false, edit)
    }

    override fun onActivityResult(reqCode: Int, resCode: Int, data: Intent?) {
        if (reqCode == ACTIVITY_REQUEST_CODE) {
            setResult(resCode)
            finish()
        } else {
            super.onActivityResult(reqCode, resCode, data)
        }
    }

    override fun search(query: String?) {

    }

    private fun onCancel() {
        setMessage(R.string.account_setup_check_settings_canceling_msg)
        checkSettingsViewModel.cancel()

        setResult(RESULT_CANCELED)
        finish()
    }

    private fun showErrorDialog(msgResId: Int, vararg args: Any) {
        handler.post {
            showDialogFragment(R.id.dialog_account_setup_error, getString(msgResId, *args))
        }
    }

    private fun showDialogFragment(dialogId: Int, customMessage: String) {
        if (destroyed) return

        progressBar.isIndeterminate = false

        val fragment: DialogFragment = if (dialogId == R.id.dialog_account_setup_error) {
            ConfirmationDialogFragment.newInstance(
                dialogId,
                getString(R.string.account_setup_failed_dlg_title),
                customMessage,
                getString(R.string.account_setup_failed_dlg_edit_details_action)
            ).apply { isCancelable = false }
        } else {
            throw RuntimeException("Called showDialog(int) with unknown dialog id.")
        }

        // TODO: commitAllowingStateLoss() is used to prevent https://code.google.com/p/android/issues/detail?id=23761
        // but is a bad...
        supportFragmentManager
            .beginTransaction()
            .add(fragment, getDialogTag(dialogId))
            .commitAllowingStateLoss()
    }

    private fun getDialogTag(dialogId: Int): String {
        return String.format(Locale.US, "dialog-%d", dialogId)
    }

    override fun doPositiveClick(dialogId: Int) = Unit

    override fun doNegativeClick(dialogId: Int) {
        if (dialogId == R.id.dialog_account_setup_error) {
            setResult(errorResultCode)
            finish()
        }
    }

    override fun dialogCancelled(dialogId: Int) = Unit

    private fun errorMessageForCertificateException(e: CertificateValidationException): String? {
        return when (e.reason) {
            CertificateValidationException.Reason.Expired -> {
                getString(R.string.client_certificate_expired, e.alias, e.message)
            }
            CertificateValidationException.Reason.MissingCapability -> {
                getString(R.string.auth_external_error)
            }
            CertificateValidationException.Reason.RetrievalFailure -> {
                getString(R.string.client_certificate_retrieval_failure, e.alias)
            }
            CertificateValidationException.Reason.UseMessage -> {
                e.message
            }
            else -> {
                ""
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_ERROR_RESULT_CODE, errorResultCode)
    }

    enum class CheckDirection {
        INCOMING, OUTGOING;
    }

    companion object {
        const val ACTIVITY_REQUEST_CODE = 1
        const val RESULT_CODE_MANUAL_SETUP_NEEDED = 9

        private const val EXTRA_ACCOUNT = "account"
        private const val EXTRA_CHECK_DIRECTION = "checkDirection"
        private const val EXTRA_MAIL_SETTINGS_DISCOVERY_REQUIRED = "mailSettingsDiscoveryRequired"
        private const val EXTRA_EDIT = "edit"
        private const val STATE_ERROR_RESULT_CODE = "errorResultCode"

        @JvmStatic
        @JvmOverloads
        fun actionCheckSettings(
            context: Activity,
            account: Account,
            direction: CheckDirection,
            mailSettingsDiscoveryRequired: Boolean,
            edit: Boolean = false
        ) {
            val intent = Intent(context, AccountSetupCheckSettings::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account.uuid)
                putExtra(EXTRA_CHECK_DIRECTION, direction)
                putExtra(EXTRA_MAIL_SETTINGS_DISCOVERY_REQUIRED, mailSettingsDiscoveryRequired)
                putExtra(EXTRA_EDIT, edit)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }

            context.startActivityForResult(intent, ACTIVITY_REQUEST_CODE)
        }
    }
}
