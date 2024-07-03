package com.fsck.k9.activity.setup

sealed interface AccountSetupNamesState {
    object Idle: AccountSetupNamesState
    object Loading: AccountSetupNamesState
    object Done: AccountSetupNamesState
}