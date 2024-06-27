package com.fsck.k9.activity.setup

import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.mdm.ConfigurationManager
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.provisioning.ProvisioningScope
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseUnlockState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import javax.inject.Inject

@HiltViewModel
class CreateAccountKeysViewModel @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val controller: MessagingController,
    private val uiCache: PlanckUIArtefactCache,
    private val configManager: ConfigurationManager,
    formatValidator: PassphraseFormatValidator
) : PassphraseViewModel(formatValidator) {
    private lateinit var account: Account
    private var manualSetup = false
    private val passwordStates get() = textFieldStates.filterIsInstance<AccountTextFieldState>()

    fun start() {
        account =
            preferences.accountsAllowingIncomplete.first { it.setupState == Account.SetupState.INITIAL }
        if (K9.isPlanckUsePassphraseForNewKeys()) {
            stateLiveData.value =
                PassphraseUnlockState.UnlockingPassphrases(
                    listOf(
                        AccountTextFieldState(account.email)
                    )
                )
        } else {
            createAccountKeys()
        }
    }

    fun initialize(accountUuid: String, manualSetup: Boolean) {
        account = preferences.getAccountAllowingIncomplete(accountUuid)
        this.manualSetup = manualSetup
        if (K9.isPlanckUsePassphraseForNewKeys()) {
            textFieldStates.add(AccountTextFieldState(account.email))
            updateState()
        } else {
            createAccountKeys()
        }
    }

    fun createAccountKeys() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseState.Processing
            withContext(Dispatchers.IO) {
                uiCache.removeCredentialsInPreferences()
                configPassphraseIfNeeded()
                account.setupState = Account.SetupState.READY
                if (manualSetup) {
                    account.setOptionsOnInstall()
                }
                if (k9.isRunningOnWorkProfile) {
                    configManager.loadConfigurationsSuspend(
                        ProvisioningScope.SingleAccountSettings(
                            account.email
                        )
                    )
                } else {
                    account.save(preferences)
                }
                controller.refreshRemoteSynchronous(account)
                PlanckUtils.pEpGenerateAccountKeys(k9, account)
                K9.setServicesEnabled(k9)
            }
            stateLiveData.value = PassphraseState.Success
        }
    }

    private suspend fun configPassphraseIfNeeded() {
        if (K9.isPlanckUsePassphraseForNewKeys()) {
            if (passwordStates.isNotEmpty()) {
                val pair = passwordStates.first()
                planckProvider.configPassphraseForNewKeys(
                    true,
                    pair.email,
                    pair.text
                ).onFailure {
                    stateLiveData.value = PassphraseState.CoreError(it)
                }

            }
        }
    }

    override fun updateState(errorType: PassphraseVerificationStatus?) {
        stateLiveData.value = PassphraseUnlockState.UnlockingPassphrases(
            passwordStates,
            status = errorType ?: getCurrentStatusOrDefault()
        )
    }
}