package com.fsck.k9.activity.setup

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.helper.Utility
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.pEp.infrastructure.exceptions.DeviceOfflineException
import com.fsck.k9.pEp.infrastructure.extensions.mapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CheckSettingsViewModel(
    private val controller: MessagingController
) : ViewModel() {

    private lateinit var account: Account

    private val _state = MutableLiveData<CheckSettingsState>(CheckSettingsState.Idle)
    val state: LiveData<CheckSettingsState> = _state
    private var job: Job? = null

    fun start(
        context: Context,
        account: Account,
        direction: AccountSetupCheckSettings.CheckDirection,
        edit: Boolean
    ) {
        if (!::account.isInitialized) { // check if start() was already called
            this.account = account
            job = viewModelScope.launch {
                startCheckServerSettings(context, direction, edit)
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private suspend fun startCheckServerSettings(
        context: Context,
        direction: AccountSetupCheckSettings.CheckDirection,
        edit: Boolean
    ) = withContext(Dispatchers.IO) {
        kotlin.runCatching {
            clearCertificateErrorNotifications(direction)
            checkServerSettings(direction)
            if (edit) {
                account.save(Preferences.getPreferences(context))
                K9.setServicesEnabled(context)
            }
        }.mapError { throwable ->
            when {
                throwable is AuthenticationFailedException ||
                        throwable is CertificateValidationException -> {
                    throwable
                }
                throwable is MessagingException && !Utility.hasConnectivity(context) -> {
                    DeviceOfflineException()
                }
                else -> {
                    Timber.e(throwable, "Error while testing settings")
                    throwable
                }
            }
        }.onFailure {
            _state.postValue(CheckSettingsState.Error(it))
        }.onSuccess {
            _state.postValue(CheckSettingsState.Success)
        }
    }

    private fun clearCertificateErrorNotifications(
        direction: AccountSetupCheckSettings.CheckDirection
    ) {
        controller.clearCertificateErrorNotifications(account, direction)
    }

    private fun checkServerSettings(direction: AccountSetupCheckSettings.CheckDirection) {
        when (direction) {
            AccountSetupCheckSettings.CheckDirection.INCOMING -> checkIncoming()
            AccountSetupCheckSettings.CheckDirection.OUTGOING -> checkOutgoing()
        }
    }

    private fun checkOutgoing() {
        _state.postValue(CheckSettingsState.CheckingOutgoing)
        controller.checkOutgoingServerSettings(account)
    }

    private fun checkIncoming() {
        _state.postValue(CheckSettingsState.CheckingIncoming)
        controller.checkIncomingServerSettings(account)
    }
}

sealed interface CheckSettingsState {
    object Idle : CheckSettingsState
    object CheckingIncoming : CheckSettingsState
    object CheckingOutgoing : CheckSettingsState
    data class Error(val throwable: Throwable) : CheckSettingsState
    object Success : CheckSettingsState
}