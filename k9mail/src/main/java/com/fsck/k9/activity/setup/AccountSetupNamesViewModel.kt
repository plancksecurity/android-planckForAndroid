package com.fsck.k9.activity.setup

import androidx.lifecycle.ViewModel
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AccountSetupNamesViewModel @Inject constructor(
    private val preferences: Preferences,
) : ViewModel() {
    lateinit var account: Account
        private set
    var manualSetup = false
        private set

    fun initialize(accountUuid: String, manualSetup: Boolean) {
        account = preferences.getAccountAllowingIncomplete(accountUuid)
        this.manualSetup = manualSetup
    }
}