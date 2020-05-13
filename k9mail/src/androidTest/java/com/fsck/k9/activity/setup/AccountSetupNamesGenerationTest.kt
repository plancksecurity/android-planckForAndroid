package com.fsck.k9.activity.setup

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ActivityTestRule
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.Identity
import com.fsck.k9.Preferences
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class AccountSetupNamesGenerationTest {

    private lateinit var account: Account
    private var timesDoInBackground = 0

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
    fun doInBackgroundIsRunOnceAndShowProgressDialogIsRunEveryTimeWithRule() {
        val activity = namesRule.activity

        val fakeGenerateAccountKeysTask = object :
                AccountSetupNames.pEpGenerateAccountKeysTask(namesRule.activity, account) {

            override fun doInBackground(vararg params: Void?): Void? {
                timesDoInBackground ++
                return null
            }

            override fun onPostExecute(aVoid: Void?) {
                assertEquals(1, timesDoInBackground)
            }
        }

        activity.launchGenerateAccountKeysTask(fakeGenerateAccountKeysTask)
        activity.runOnUiThread {
            activity.recreate()
            activity.recreate()
            activity.recreate()
        }
        namesRule.finishActivity()
    }
}