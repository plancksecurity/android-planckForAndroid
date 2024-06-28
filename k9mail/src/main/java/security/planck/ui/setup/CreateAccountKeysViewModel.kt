package security.planck.ui.setup

import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.mdm.ConfigurationManager
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.provisioning.ProvisioningScope
import security.planck.ui.passphrase.PassphraseViewModel
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract
import javax.inject.Inject

private const val NEW_PASSPHRASE_INDEX = 0
private const val NEW_PASSPHRASE_CONFIRMATION_INDEX = 1

@HiltViewModel
class CreateAccountKeysViewModel @Inject constructor(
    private val k9: K9,
    private val preferences: Preferences,
    private val planckProvider: PlanckProvider,
    private val controller: MessagingController,
    private val uiCache: PlanckUIArtefactCache,
    private val configManager: ConfigurationManager,
    private val dispatcherProvider: DispatcherProvider,
    formatValidator: PassphraseFormatValidator
) : PassphraseViewModel(formatValidator) {
    private lateinit var account: Account
    private var manualSetup = false
    private val newPasswordState: AccountTextFieldState
        get() = textFieldStates[NEW_PASSPHRASE_INDEX] as AccountTextFieldState
    private val newPasswordVerificationState: TextFieldState
        get() = textFieldStates[NEW_PASSPHRASE_CONFIRMATION_INDEX] as TextFieldState

    fun initialize(accountUuid: String, manualSetup: Boolean) {
        account = preferences.getAccountAllowingIncomplete(accountUuid)
        this.manualSetup = manualSetup
        if (K9.isPlanckUsePassphraseForNewKeys()) {
            textFieldStates.add(AccountTextFieldState(account.email))
            textFieldStates.add(TextFieldState())
            updateState()
        } else {
            createAccountKeys()
        }
    }

    fun createAccountKeys() {
        viewModelScope.launch {
            stateLiveData.value = PassphraseState.Processing
            withContext(dispatcherProvider.io()) {
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
            val pair = newPasswordState
            planckProvider.configPassphraseForNewKeys(
                true,
                pair.email,
                pair.text
            ).onFailure {
                stateLiveData.value = PassphraseState.CoreError(it)
            }
        }
    }

    fun updateNewPassphrase(text: String) {
        updateAndValidateText(NEW_PASSPHRASE_INDEX, text)
    }

    fun updateNewPassphraseVerification(text: String) {
        updateAndValidateText(NEW_PASSPHRASE_CONFIRMATION_INDEX, text)
    }

    override fun updateAndValidateInput(
        position: Int,
        text: String
    ): PassphraseVerificationStatus? {
        return when (position) {
            NEW_PASSPHRASE_INDEX -> {
                val npStatus = updateAndValidateNewPassphraseText(text)
                val npVerificationStatus = updateAndValidateNewPassphraseVerificationText(
                    text,
                    newPasswordVerificationState.text
                )
                if (npStatus.isError) {
                    PassphraseVerificationStatus.WRONG_FORMAT
                } else if (npVerificationStatus.isError) {
                    PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
                } else null
            }

            NEW_PASSPHRASE_CONFIRMATION_INDEX -> {
                updateAndGetOverallStatusFromPassphraseVerificationText(newPasswordState.text, text)
            }

            else -> error("UNEXPECTED INDEX: $position")
        }
    }

    private fun updateAndValidateNewPassphraseText(text: String): TextFieldStateContract.ErrorStatus {
        val newPasswordState = newPasswordState
        val status = passphraseFormatValidator.validatePassphrase(text)
        textFieldStates[NEW_PASSPHRASE_INDEX] =
            newPasswordState.copyWith(newText = text, errorStatus = status)
        return status
    }

    private fun updateAndValidateNewPassphraseVerificationText(
        passphrase: String,
        verification: String
    ): TextFieldStateContract.ErrorStatus {
        val newPasswordState = newPasswordVerificationState
        val status = passphraseFormatValidator.verifyNewPassphrase(passphrase, verification)
        textFieldStates[NEW_PASSPHRASE_CONFIRMATION_INDEX] =
            newPasswordState.copyWith(newText = verification, errorStatus = status)
        return status
    }

    private fun updateAndGetOverallStatusFromPassphraseVerificationText(
        passphrase: String,
        verification: String
    ): PassphraseVerificationStatus? {
        val newPasswordVerifyState = newPasswordVerificationState
        var overallError: PassphraseVerificationStatus? = null
        val status: TextFieldStateContract.ErrorStatus
        if (newPasswordState.errorStatus.isError) {
            overallError = PassphraseVerificationStatus.WRONG_FORMAT
            status = TextFieldStateContract.ErrorStatus.ERROR
        } else {
            status = passphraseFormatValidator.verifyNewPassphrase(passphrase, verification)
            if (status.isError) overallError =
                PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
        }
        textFieldStates[NEW_PASSPHRASE_CONFIRMATION_INDEX] =
            newPasswordVerifyState.copyWith(newText = verification, errorStatus = status)
        return overallError
    }


    override fun calculateNewOverallStatus(): PassphraseVerificationStatus {
        var success = 0
        textFieldStates.forEachIndexed { index, state ->
            if (state.errorStatus.isError) {
                return if (index == NEW_PASSPHRASE_CONFIRMATION_INDEX)
                    PassphraseVerificationStatus.NEW_PASSPHRASE_DOES_NOT_MATCH
                else
                    PassphraseVerificationStatus.WRONG_FORMAT
            } else {
                if (state.errorStatus == TextFieldStateContract.ErrorStatus.SUCCESS) {
                    success++
                }
            }
        }
        return if (success == textFieldStates.size) PassphraseVerificationStatus.SUCCESS
        else PassphraseVerificationStatus.NONE
    }

    override fun updateState(errorType: PassphraseVerificationStatus?) {
        stateLiveData.value = PassphraseState.CreatingAccount(
            newPasswordState,
            newPasswordVerificationState,
            status = errorType ?: getCurrentStatusOrDefault(),
        )
    }
}