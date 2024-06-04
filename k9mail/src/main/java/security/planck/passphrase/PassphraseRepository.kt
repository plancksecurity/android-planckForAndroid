package security.planck.passphrase

import android.util.Log
import com.fsck.k9.Account
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.PlanckProvider
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PassphraseRepository @Inject constructor(
    private val planckProvider: Provider<PlanckProvider>,
    private val preferences: Preferences,
    private val k9: K9,
) {
    fun initializeBlocking() {
        runBlocking {
            getAccountsWithPassPhrase().onFailure {
                if (BuildConfig.DEBUG) {
                    Log.e("PASSPHRASE", "LOAD FAILURE: ${it.stackTraceToString()}")
                }
            }.onSuccess { list ->
                if (list.isEmpty()) {
                    unlockPassphrase()
                }
            }
        }
    }

    suspend fun getAccountsWithPassPhrase(): Result<List<Account>> {
        val accounts = preferences.availableAccounts.filter { account ->
            planckProvider.get().hasPassphrase(account.email).fold(
                onFailure = {
                    return Result.failure(it)
                },
                onSuccess = {
                    it
                }
            )
        }
        return Result.success(accounts)
    }

    fun unlockPassphrase() {
        passphraseUnlocked = true
        // initialize all mail services etc etc
        k9.startAllServices()
    }

    fun resetPassphraseLock() {
        passphraseUnlocked = false
    }

    companion object {
        @Volatile
        @JvmStatic
        var passphraseUnlocked = false
            private set
    }
}
