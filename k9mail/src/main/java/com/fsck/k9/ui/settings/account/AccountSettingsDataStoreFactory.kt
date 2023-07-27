package com.fsck.k9.ui.settings.account

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSettingsDataStoreFactory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val preferences: Preferences,
        private val executorService: ExecutorService
) {
    fun create(account: Account): AccountSettingsDataStore {
        return AccountSettingsDataStore(context, preferences, executorService, account)
    }
}
