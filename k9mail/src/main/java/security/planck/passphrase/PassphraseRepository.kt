package security.planck.passphrase

import android.util.Log
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class PassphraseRepository @Inject constructor(
    private val planckProvider: Provider<PlanckProvider>,
    private val preferences: Preferences,
    private val k9: K9,
    private val dispatcherProvider: DispatcherProvider,
) {
    //private val passphraseUnlockNeededMF: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun initializeBlocking() {
        runBlocking {
            getAccountsWithPassPhrase().onFailure {
                Log.e("EFA-601", "EFA-601 FAILURE: ${it.stackTraceToString()}")
            }.onSuccess { list ->
                if (list.isEmpty()) {
                    Log.e("EFA-601", "EFA-601 NO PASSPHRASE NEEDED, UNLOCKING....")
                    unlockPassphrase()
                } else {
                    Log.e("EFA-601", "EFA-601 PASSPHRASE REQUIRED, LOCKED")
                    //timeToStartOrRetryMF.value = PassphraseUnlockNeeded(start = true)
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

    companion object {
        @Volatile
        @JvmStatic
        var passphraseUnlocked = false
            private set
    }
}

data class PassphraseUnlockNeeded(
    val failedAttempts: Int = 0,
    val start: Boolean = false,
) {
    companion object {
        val noError = PassphraseUnlockNeeded(0,)
    }
}