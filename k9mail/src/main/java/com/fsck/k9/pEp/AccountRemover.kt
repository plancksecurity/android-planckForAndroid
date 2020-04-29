package com.fsck.k9.pEp

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AccountRemover {

    @JvmStatic
    fun launchRemoveAccount(account: Account?, context: Context) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            removeAccount(account, context)
        }
    }

    suspend fun removeAccount(account: Account?, context: Context) {
        if(account == null) return
        account.isDeleted = true
        try {
            account.localStore.delete()
        } catch (e: Exception) {
            // Ignore, this may lead to localStores on sd-cards that
            // are currently not inserted to be left
        }

        MessagingController.getInstance(context.applicationContext)
                .deleteAccount(account)
        Preferences.getPreferences(context)
                .deleteAccount(account)
        K9.setServicesEnabled(context)
    }
}