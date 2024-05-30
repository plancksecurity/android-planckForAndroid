package security.planck.passphrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import com.fsck.k9.planck.infrastructure.livedata.Event
import kotlinx.coroutines.flow.launchIn

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor(
    private val passphraseRepository: PassphraseRepository,
): ViewModel() {
    private val passphraseUnlockRetryLiveData: MutableLiveData<Event<PassphraseUnlockRetryState>> = MutableLiveData(Event(PassphraseUnlockRetryState.Idle))
    val passphraseUnlockRetry: LiveData<Event<PassphraseUnlockRetryState>> = passphraseUnlockRetryLiveData

    init {
        passphraseRepository.timeToStartOrRetry.onEach { notification ->
            if (notification.start) {
                passphraseUnlockRetryLiveData.value = Event(PassphraseUnlockRetryState.TimeToStart)
            } else if (notification.failedAttempts >= PassphraseRepository.MAX_ATTEMPTS_STOP_APP) {
                passphraseUnlockRetryLiveData.value = Event(PassphraseUnlockRetryState.FinishApp)
            } else if (notification != PassphraseUnlockRetryNotification.noError) {
                passphraseUnlockRetryLiveData.value = Event(PassphraseUnlockRetryState.TimeToRetry(notification.accountUnlockErrors))
            }
        }.launchIn(viewModelScope)
    }
}