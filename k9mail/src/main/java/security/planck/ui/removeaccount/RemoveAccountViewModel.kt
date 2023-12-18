package security.planck.ui.removeaccount

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

private const val MINIMUM_DELAY_FOR_ACCOUNT_DELETION = 500L

@HiltViewModel
class RemoveAccountViewModel @Inject constructor(
    private val context: Application,
    private val preferences: Preferences,
    private val controller: MessagingController,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private lateinit var account: Account

    private val stateLiveData: MutableLiveData<RemoveAccountState> =
        MutableLiveData(RemoveAccountState.Idle)
    val state: LiveData<RemoveAccountState> = stateLiveData

    fun initialize(uuid: String) {
        viewModelScope.launch {
            account = preferences.getAccount(uuid) ?: let {
                stateLiveData.value = RemoveAccountState.AccountNotAvailable(uuid)
                return@launch
            }
            stateLiveData.value =
                RemoveAccountState.RemoveAccountConfirmation(account.description ?: account.email)
        }
    }

    fun positiveAction() {
        when (stateLiveData.value) {
            is RemoveAccountState.RemoveAccountConfirmation ->
                viewModelScope.launch {
                    removeAccount()
                }

            is RemoveAccountState.Done ->
                stateLiveData.value = RemoveAccountState.Finish(true)

            is RemoveAccountState.AccountNotAvailable ->
                stateLiveData.value = RemoveAccountState.Finish(true)

            else -> error("unexpected state: ${stateLiveData.value}")
        }
    }

    fun negativeAction() {
        when (stateLiveData.value) {
            is RemoveAccountState.RemoveAccountConfirmation ->
                stateLiveData.value = RemoveAccountState.Finish(false)

            else -> error("unexpected state: ${stateLiveData.value}")
        }
    }

    private suspend fun removeAccount() {
        stateLiveData.value = RemoveAccountState.RemovingAccount
        deleteAccountWork()
        stateLiveData.value = RemoveAccountState.Done(account.description ?: account.email)
    }

    private suspend fun deleteAccountWork() = withContext(dispatcherProvider.io()) {
        launch {
            delay(MINIMUM_DELAY_FOR_ACCOUNT_DELETION)
        }
        try {
            account.localStore?.delete()
        } catch (e: Exception) {
            // Ignore, this may lead to localStores on sd-cards that
            // are currently not inserted to be left
        }
        kotlin.runCatching {
            controller.deleteAccount(account)
            preferences.deleteAccount(account)
            K9.setServicesEnabled(context)
        }.onFailure { Timber.e(it) }
    }
}