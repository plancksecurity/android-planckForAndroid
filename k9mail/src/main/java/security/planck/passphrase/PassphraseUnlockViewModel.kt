package security.planck.passphrase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PassphraseUnlockViewModel @Inject constructor() : ViewModel() {
    private val needsPassphraseUnlockLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(!PassphraseRepository.passphraseUnlocked))
    val needsPassphraseUnlock: LiveData<Event<Boolean>> = needsPassphraseUnlockLiveData
}