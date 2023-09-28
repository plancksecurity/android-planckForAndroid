package com.fsck.k9.ui.settings.general

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.K9
import com.fsck.k9.planck.infrastructure.livedata.Event
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GeneralSettingsViewModel @Inject constructor(
    private val k9: K9,
) : ViewModel() {

    private val deviceGroupLeftLD: MutableLiveData<Event<Boolean>> = MutableLiveData(Event(false))
    val deviceGroupLeft: LiveData<Event<Boolean>> = deviceGroupLeftLD

    fun leaveDeviceGroup() {
        viewModelScope.launch {
            withContext(PlanckDispatcher) {
                k9.leaveDeviceGroup()
            }
            if (!k9.isGrouped) {
                deviceGroupLeftLD.value = Event(true)
            }
        }
    }
}