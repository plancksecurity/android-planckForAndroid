package security.planck.passphrase

import android.app.Application
import android.content.Context
import android.util.Log
import com.fsck.k9.Account
import com.fsck.k9.Globals
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import foundation.pEp.jniadapter.Pair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.pow

@Singleton
class PassphraseRepository @Inject constructor(
    private val planckProvider: Provider<PlanckProvider>,
    private val preferences: Preferences,
    private val k9: K9,
    private val dispatcherProvider: DispatcherProvider,
) {
    var unlockErrors: PassphraseUnlockRetryNotification = PassphraseUnlockRetryNotification.noError
    private set

    private val attemptWithDelay get() = unlockErrors.failedAttempts - RETRY_WITH_DELAY_AFTER

    private val timeToStartOrRetryMF: MutableStateFlow<PassphraseUnlockRetryNotification> =
        MutableStateFlow(
            PassphraseUnlockRetryNotification.noError
        )
    val timeToStartOrRetry: StateFlow<PassphraseUnlockRetryNotification> = timeToStartOrRetryMF.asStateFlow()

    fun initialize() {
        CoroutineScope(dispatcherProvider.planckDispatcher()).launch {
            getAccountsWithPassPhrase().onFailure {

            }.onSuccess { list ->
                if (list.isEmpty()) {
                    passphraseUnlocked = true
                    k9.startAllServices()
                } else {
                    timeToStartOrRetryMF.value = PassphraseUnlockRetryNotification(start = true)
                }
            }
        }
    }

    fun initializeBlocking() {
        runBlocking {
            getAccountsWithPassPhrase().onFailure {
                Log.e("EFA-601", "EFA-601 FAILURE: ${it.stackTraceToString()}")
            }.onSuccess { list ->
                if (list.isEmpty()) {
                    Log.e("EFA-601", "EFA-601 NO PASSPHRASE NEEDED, UNLOCKING....")
                    passphraseUnlocked = true
                    //k9.startAllServices()
                } else {
                    Log.e("EFA-601", "EFA-601 PASSPHRASE REQUIRED, LOCKED")
                    timeToStartOrRetryMF.value = PassphraseUnlockRetryNotification(start = true)
                }
            }
        }
    }

    private suspend fun getAccountsWithPassPhrase(): Result<List<Account>> {
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


    suspend fun unlockKeysWithPassphrase(
        emails: List<String>,
        passphrases: List<String>
    ): Result<List<String>?> = withContext(dispatcherProvider.planckDispatcher()) {
        val keysWithPassphrase =
            emails.mapIndexed { index, email -> Pair(email, passphrases[index]) }
        val result = planckProvider.get().unlockKeysWithPassphrase(ArrayList(keysWithPassphrase))
        Timber.e("EFA-601 UNLOCKING KEYS WITH PASSPHRASE RETURNED")
        result.map { it?.toList() }.onFailure {
            unlockErrors = unlockErrors.copy(failedAttempts = unlockErrors.failedAttempts + 1)
            processError()
            Timber.e("EFA-601 RESULT ERROR: ${it.stackTraceToString()}")
        }.onSuccess { list ->
            Timber.e("EFA-601 RESULT: $list")
            if (list.isNullOrEmpty()) {
                Timber.e("EFA-601 STARTING MAIL SERVICES...")
                passphraseUnlocked = true
                // initialize all mail services etc etc
                k9.startAllServices()
            } else {
                unlockErrors = unlockErrors.copy(
                    failedAttempts = unlockErrors.failedAttempts + 1,
                    accountUnlockErrors = list
                )
                processError()
            }
        }
    }

    private fun CoroutineScope.processError() {
        if (unlockErrors.failedAttempts >= RETRY_WITH_DELAY_AFTER) {
            // notify app to ask again after the delay
            launch {
                delay(RETRY_DELAY * 2.0.pow(attemptWithDelay).toLong())
                timeToStartOrRetryMF.value = unlockErrors
            }
        }
    }

    companion object {
        const val RETRY_DELAY = 10 // seconds
        const val RETRY_WITH_DELAY_AFTER = 3
        const val MAX_ATTEMPTS_STOP_APP = 10

        @Volatile
        @JvmStatic
        var passphraseUnlocked = false
            private set
    }
}

data class PassphraseUnlockRetryNotification(
    val failedAttempts: Int = 0,
    val accountUnlockErrors: List<String> = emptyList(),
    val start: Boolean = false,
) {
    companion object {
        val noError = PassphraseUnlockRetryNotification(0, emptyList())
    }
}