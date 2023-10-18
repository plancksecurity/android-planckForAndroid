package security.planck.mdm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * [ViewModel] dedicated for restrictions affairs.
 */
@HiltViewModel
class RestrictionsViewModel @Inject constructor(
    configurationManager: ConfigurationManager,
) : ViewModel() {
    private val restrictionsUpdatedLiveData: MutableLiveData<Event<Boolean>> =
        MutableLiveData(Event(false))

    /**
     * @property restrictionsUpdated
     *
     * [LiveData] that delivers restrictions updates.
     */
    val restrictionsUpdated: LiveData<Event<Boolean>> = restrictionsUpdatedLiveData

    init {
        configurationManager.restrictionsUpdatedFlow
            .onEach {
                if (it > 0) {
                    restrictionsUpdatedLiveData.value = Event(true)
                }
            }.launchIn(viewModelScope)
    }
}