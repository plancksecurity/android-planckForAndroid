package com.fsck.k9.activity.setup

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.activity.setup.AccountSetupCheckSettings.CheckDirection
import com.fsck.k9.pEp.DefaultDispatcherProvider
import com.fsck.k9.pEp.DispatcherProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.serversettings.ServerSettingsChecker

class CheckSettingsViewModel(
    private val serverSettingsChecker: ServerSettingsChecker,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
) : ViewModel() {

    private val _state = MutableLiveData<CheckSettingsState>(CheckSettingsState.Idle)
    val state: LiveData<CheckSettingsState> = _state

    fun start(
        context: Context,
        account: Account,
        direction: CheckDirection,
        edit: Boolean
    ) {
        if (_state.value == CheckSettingsState.Idle) { // check if start() was already called
            setStateForDirection(direction)
            viewModelScope.launch {
                startCheckServerSettings(context, account, direction, edit).onFailure {
                    _state.value = CheckSettingsState.Error(it)
                }.onSuccess {
                    _state.value = CheckSettingsState.Success
                }
            }
        }
    }

    fun cancel() {
        viewModelScope.cancel()
    }

    private fun setStateForDirection(direction: CheckDirection) {
        _state.value = when (direction) {
            CheckDirection.INCOMING -> CheckSettingsState.CheckingIncoming
            CheckDirection.OUTGOING -> CheckSettingsState.CheckingOutgoing
        }
    }


    private suspend fun startCheckServerSettings(
        context: Context,
        account: Account,
        direction: CheckDirection,
        edit: Boolean
    ): Result<Unit> = withContext(dispatcherProvider.io()) {
        serverSettingsChecker.checkServerSettings(context, account, direction, edit)
    }

    public override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }
}

sealed class CheckSettingsState {
    override fun toString(): String {
        return javaClass.simpleName
    }

    object Idle : CheckSettingsState()
    object CheckingIncoming : CheckSettingsState()
    object CheckingOutgoing : CheckSettingsState()
    data class Error(val throwable: Throwable) : CheckSettingsState()
    object Success : CheckSettingsState()
}