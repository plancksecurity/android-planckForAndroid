package security.planck.ui.passphrase.unlock

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import security.planck.passphrase.PassphraseRepository
import javax.inject.Inject

@HiltViewModel
class PassphraseUnlockNotificationViewModel @Inject constructor(
    repository: PassphraseRepository,
) : ViewModel() {
    private val needsPassphraseUnlockLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(repository.lockedState.value.needsUnlock))
    val needsPassphraseUnlock: LiveData<Event<Boolean>> = needsPassphraseUnlockLiveData

    init {
        repository.lockedState.onEach {
            needsPassphraseUnlockLiveData.value = Event(it.needsUnlock)
        }.launchIn(viewModelScope)
    }
}