package com.fsck.k9.pEp.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.setup.AccountSetupCheckSettings
import com.fsck.k9.activity.setup.AccountSetupNames
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.pEp.ui.ConnectionSettings
import com.fsck.k9.pEp.ui.tools.AccountSetupNavigator
import com.fsck.k9.pEp.ui.tools.SetupAccountType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.provisioning.ProvisioningSettings
import timber.log.Timber
import java.net.URISyntaxException
import javax.inject.Inject

abstract class AccountSetupBasicsFragmentBase : PEpFragment() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreDataState(savedInstanceState)
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

    abstract fun onManualSetup(fromUser: Boolean)

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
            account.name = account.name ?: email
            account.description =
                if (!Utility.isNullOrBlank(provisioningSettings.accountDescription))
                    provisioningSettings.accountDescription
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
                provisioningSettings.senderName?.let {
                    name = it
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CHECK_SETTINGS) {
            handleCheckSettingsResult(resultCode)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "com.fsck.k9.AccountSetupBasics.account"
        private const val STATE_KEY_CHECKED_INCOMING =
            "com.fsck.k9.AccountSetupBasics.checkedIncoming"
        private const val REQUEST_CODE_CHECK_SETTINGS =
            AccountSetupCheckSettings.ACTIVITY_REQUEST_CODE
    }
}