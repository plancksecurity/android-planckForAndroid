package com.fsck.k9.ui.account

import android.arch.lifecycle.LiveData
import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AccountsLiveData(context: Context) : LiveData<List<Account>>() {
    init {
        loadAccountsAsync(context)
    }

    private fun loadAccountsAsync(context: Context) {
        GlobalScope.launch(Dispatchers.Main) {
            val accounts = async {
                loadAccounts(context)
            }

            value = accounts.await()
        }
    }

    private fun loadAccounts(context: Context): List<Account> {
        return Preferences.getPreferences(context).accounts
    }
}
