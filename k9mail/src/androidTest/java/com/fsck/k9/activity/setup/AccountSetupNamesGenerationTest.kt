package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class AccountSetupNamesGenerationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var account: Account
    private val preferences = Preferences.getPreferences(context)

    @get:Rule
    var namesRule = object : ActivityTestRule<AccountSetupNames>(AccountSetupNames::class.java) {
        override fun getActivityIntent(): Intent {
            account = preferences.newAccount().apply {
                identities = listOf(Identity().apply { this.email = BuildConfig.PEP_TEST_EMAIL_ADDRESS })
            }

            return Intent(ApplicationProvider.getApplicationContext(), AccountSetupNames::class.java).apply {
                putExtra(AccountSetupNames.EXTRA_ACCOUNT, account.uuid)
            }
        }
    }

    @After
    fun tearDown() {
        preferences.deleteAccount(account)
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