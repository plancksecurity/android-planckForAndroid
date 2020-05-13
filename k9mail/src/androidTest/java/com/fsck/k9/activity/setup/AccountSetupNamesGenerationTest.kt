package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.*
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class AccountSetupNamesGenerationTest {

    private lateinit var account: Account

    @get:Rule
    var namesRule = object : ActivityTestRule<AccountSetupNames>(AccountSetupNames::class.java) {
        override fun getActivityIntent(): Intent {
            val context: Context = ApplicationProvider.getApplicationContext()
            account = Preferences.getPreferences(context).newAccount().apply {
                identities = listOf(Identity().apply { this.email = BuildConfig.PEP_TEST_EMAIL_ADDRESS })
            }
            return Intent(ApplicationProvider.getApplicationContext(), AccountSetupNames::class.java).apply {
                putExtra(AccountSetupNames.EXTRA_ACCOUNT, account.uuid)
            }
        }
    }

    @Test
    fun doInBackgroundIsRunOnceAndShowProgressDialogIsRunEveryTimeWithRuleMock() {
        val activity = namesRule.activity
        val accountKeysGenerator: AccountSetupNames.AccountKeysGenerator = mock()
        val generateAccountKeysTask = AccountSetupNames.pEpGenerateAccountKeysTask (activity, account)
        generateAccountKeysTask.accountKeysGenerator = accountKeysGenerator


        activity.runOnUiThread {
            activity.launchGenerateAccountKeysTask(generateAccountKeysTask)
            activity.recreate()
            activity.recreate()
            activity.recreate()
        }
        namesRule.finishActivity()


        verify(accountKeysGenerator, times(1)).generateAccountKeys()
        verify(accountKeysGenerator, times(1)).onAccountKeysGenerationFinished()
    }
}