package com.fsck.k9.activity.setup

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.pEp.serversettings.ServerSettingsChecker

class CheckSettingsViewModel(
    private val serverSettingsChecker: ServerSettingsChecker
) : ViewModel() {

    private val _state = MutableLiveData<CheckSettingsState>(CheckSettingsState.Idle)
    val state: LiveData<CheckSettingsState> = _state
    private var job: Job? = null

    fun start(
        context: Context,
        account: Account,
        direction: AccountSetupCheckSettings.CheckDirection,
        edit: Boolean
    ) {
        if (_state.value == CheckSettingsState.Idle) { // check if start() was already called
            setStateForDirection(direction)
            job = viewModelScope.launch {
                startCheckServerSettings(context, account, direction, edit).onFailure {
                    _state.value = CheckSettingsState.Error(it)
                }.onSuccess {
                    _state.value = CheckSettingsState.Success
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
    }

    private fun setStateForDirection(direction: AccountSetupCheckSettings.CheckDirection) {
        _state.value = when (direction) {
            AccountSetupCheckSettings.CheckDirection.INCOMING -> CheckSettingsState.CheckingIncoming
            AccountSetupCheckSettings.CheckDirection.OUTGOING -> CheckSettingsState.CheckingOutgoing
        }
    }


    private suspend fun startCheckServerSettings(
        context: Context,
        account: Account,
        direction: AccountSetupCheckSettings.CheckDirection,
        edit: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        serverSettingsChecker.checkServerSettings(context, account, direction, edit)
    }
}

sealed interface CheckSettingsState {
    object Idle : CheckSettingsState
    object CheckingIncoming : CheckSettingsState
    object CheckingOutgoing : CheckSettingsState
    data class Error(val throwable: Throwable) : CheckSettingsState
    object Success : CheckSettingsState
}