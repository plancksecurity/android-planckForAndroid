package com.fsck.k9.pEp.importAccount

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.controller.SimpleMessagingListener
import com.fsck.k9.fragment.ProgressDialogFragment
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.Transport
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.pEp.importAccount.PASSWORD.ACCOUNTS_ID
import com.fsck.k9.pEp.importAccount.PASSWORD.ACCOUNT_ID
import com.fsck.k9.pEp.importAccount.PASSWORD.FRAGMENT_SET_ACCOUNT_PASSWORD
import com.fsck.k9.pEp.manualsync.WizardActivity
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.*

fun showPasswordDialog(context: Context,
                       accounts: ArrayList<String>): Intent {
    val intent = Intent(context, PasswordPrompt::class.java)
    val accountId = accounts.removeAt(0)
    intent.putExtra(ACCOUNT_ID, accountId)
    intent.putStringArrayListExtra(ACCOUNTS_ID, accounts)
    return intent
}

class PasswordPrompt : WizardActivity(), TextWatcher {

    private lateinit var account: Account
    private var remainingAccounts: ArrayList<String>? = null
    private lateinit var incoming: ServerSettings
    private lateinit var outgoing: ServerSettings
    private var configureOutgoingServer: Boolean = false
    private var configureIncomingServer: Boolean = false

    private lateinit var okButton: Button
    private lateinit var cancelButton: Button
    private lateinit var incomingPasswordEditText: EditText
    private lateinit var outgoingPasswordEditText: EditText
    private lateinit var useIncomingCheckbox: CheckBox
    private lateinit var passwordPromptIntro: TextView
    private lateinit var outgoingTIL: TextInputLayout
    private lateinit var incomingTIL: TextInputLayout

    private var progressDialog: ProgressDialogFragment? = null

    val listener = object : SimpleMessagingListener() {

        override fun listFoldersFinished(account: Account?) {
            removeProgressDialog()
            sendDataBack()
        }

        override fun listFoldersFailed(account: Account?, message: String?) {
            this.listFoldersFinished(account)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.password_prompt_dialog)
        getExtras()
        startView()
        setUpFloatingWindow()
    }

    private fun getExtras() {
        if (intent != null) {
            intent.getStringExtra(ACCOUNT_ID).let { accountId ->
                val preferences = Preferences.getPreferences(this)
                account = preferences.getAccount(accountId)
            }
            intent.getStringArrayListExtra(ACCOUNTS_ID).let { remainingAccounts = it }
        }
    }

    private fun startView() {
        okButton = findViewById(R.id.okButton)
        cancelButton = findViewById(R.id.cancelButton)
        outgoingTIL = findViewById(R.id.outgoingTIL)
        incomingTIL = findViewById(R.id.incomingTIL)
        useIncomingCheckbox = findViewById(R.id.useIncomingCheckbox)
        passwordPromptIntro = findViewById(R.id.passwordPromptIntro)
        incomingPasswordEditText = findViewById(R.id.incomingPasswordEditText)
        outgoingPasswordEditText = findViewById(R.id.outgoingPasswordEditText)

        getConfigs()
        setClickListeners()
        setTexts()
        setCheckbox()
        configureIncomingServer()
        configureOutgoingServer()
    }

    private fun getConfigs() {
        incoming = RemoteStore.decodeStoreUri(account.storeUri)
        outgoing = Transport.decodeTransportUri(account.transportUri)

        /*
         * Don't ask for the password to the outgoing server for WebDAV
         * accounts, because incoming and outgoing servers are identical for
         * this account type. Also don't ask when the username is missing.
         * Also don't ask when the AuthType is EXTERNAL or XOAUTH2
         */
        configureOutgoingServer = AuthType.EXTERNAL != outgoing.authenticationType
                && AuthType.XOAUTH2 != outgoing.authenticationType
                && ServerSettings.Type.WebDAV != outgoing.type
                && outgoing.username != null && outgoing.username.isNotEmpty()
                && (outgoing.password == null || outgoing.password.isEmpty())
        configureIncomingServer = AuthType.EXTERNAL != incoming.authenticationType
                && AuthType.XOAUTH2 != incoming.authenticationType
                && (incoming.password == null || incoming.password.isEmpty())
    }

    private fun setClickListeners() {
        okButton.setOnClickListener {
            var incomingPassword: String? = null
            if (configureIncomingServer) {
                incomingPassword = incomingPasswordEditText.text.toString()
            }
            var outgoingPassword: String? = null
            if (configureOutgoingServer) {
                outgoingPassword =
                        if (useIncomingCheckbox.isChecked) incomingPassword
                        else outgoingPasswordEditText.text.toString()
            }
            // Set the server passwords in the background
            showProgressDialog()
            setAccountPassword(incomingPassword, outgoingPassword)
        }
        cancelButton.setOnClickListener { finish() }
    }

    private fun sendDataBack() {
        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra(ACCOUNTS_ID, remainingAccounts)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun setTexts() {
        val serverPasswords = resources
                .getQuantityString(R.plurals.settings_import_server_passwords,
                        if (configureIncomingServer && configureOutgoingServer) 2 else 1)
        passwordPromptIntro.text = getString(R.string.settings_import_activate_account_intro,
                account.description, serverPasswords)
    }

    private fun setCheckbox() {
        useIncomingCheckbox.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                outgoingPasswordEditText.text = null
                outgoingPasswordEditText.isEnabled = false
                outgoingTIL.visibility = View.INVISIBLE
            } else {
                outgoingPasswordEditText.text = incomingPasswordEditText.text
                outgoingPasswordEditText.isEnabled = true
                outgoingTIL.visibility = View.VISIBLE
            }
        }
        useIncomingCheckbox.visibility = if (configureIncomingServer && configureOutgoingServer) View.VISIBLE else View.GONE
        useIncomingCheckbox.isChecked = configureIncomingServer
    }

    private fun configureIncomingServer() {
        if (configureIncomingServer) {
            incomingTIL.hint = getString(R.string.settings_import_incoming_server, incoming.host)
            incomingPasswordEditText.addTextChangedListener(this)
        } else {
            incomingTIL.visibility = View.GONE
        }
    }

    private fun configureOutgoingServer() {
        if (configureOutgoingServer) {
            outgoingTIL.hint = getString(R.string.settings_import_outgoing_server, outgoing.host)
            outgoingPasswordEditText.addTextChangedListener(this)
        } else {
            outgoingTIL.visibility = View.GONE
        }
    }

    override fun search(query: String) {}

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onBackPressed() {}

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { // Not used
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { // Not used
    }

    override fun afterTextChanged(s: Editable) {
        var enable = false
        if (configureIncomingServer) {
            if (incomingPasswordEditText.text.isNotEmpty()) {
                if (!configureOutgoingServer) {
                    enable = true
                } else if (useIncomingCheckbox.isChecked ||
                        outgoingPasswordEditText.text.isNotEmpty()) {
                    enable = true
                }
            }
        } else {
            enable = outgoingPasswordEditText.text.isNotEmpty()
        }
        okButton.isEnabled = enable
    }

    private fun showProgressDialog() {
        val outgoingPassword = outgoingPasswordEditText.text?.toString()

        val title = getString(R.string.settings_import_activate_account_header)
        val passwordCount = outgoingPassword?.length ?: 1
        val message = resources.getQuantityString(R.plurals.settings_import_setting_passwords, passwordCount)
        progressDialog = ProgressDialogFragment.newInstance(title, message)
        progressDialog?.show(supportFragmentManager, FRAGMENT_SET_ACCOUNT_PASSWORD)
    }

    private fun setAccountPassword(incomingPassword: String?, outgoingPassword: String?) = runBlocking {
        launch(Dispatchers.IO) {
            try {
                if (incomingPassword != null) {
                    val storeUri = account.storeUri
                    val incoming = RemoteStore.decodeStoreUri(storeUri)
                    val newIncoming = incoming.newPassword(incomingPassword)
                    val newStoreUri = RemoteStore.createStoreUri(newIncoming)
                    account.storeUri = newStoreUri
                }
                if (outgoingPassword != null) {
                    val transportUri = account.transportUri
                    val outgoing = Transport.decodeTransportUri(transportUri)
                    val newOutgoing = outgoing.newPassword(outgoingPassword)
                    val newTransportUri = Transport.createTransportUri(newOutgoing)
                    account.transportUri = newTransportUri
                }
                account.isEnabled = true
                account.save(Preferences.getPreferences(this@PasswordPrompt))
                K9.setServicesEnabled(this@PasswordPrompt)
                MessagingController.getInstance(this@PasswordPrompt.applicationContext)
                        .listFolders(account, true, listener)
            } catch (e: Exception) {
                Timber.e(e, "Something happened while setting account passwords")
            }
        }
    }

    private fun removeProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
        }
        progressDialog = null
    }
}

object PASSWORD {
    const val FRAGMENT_SET_ACCOUNT_PASSWORD = "FRAGMENT_SET_ACCOUNT_PASSWORD"
    const val ACTIVITY_REQUEST_PROMPT_SERVER_PASSWORDS = 12342
    const val ACCOUNTS_ID = "ACCOUNTS_ID"
    const val ACCOUNT_ID = "ACCOUNT_ID"
}