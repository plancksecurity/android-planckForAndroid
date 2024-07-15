package com.fsck.k9.planck.ui.activities.provisioning

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningManager
import javax.inject.Inject

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val provisioningManager: ProvisioningManager,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<ProvisionState> = MutableLiveData(
        provisioningManager.state.value
    )
    val state: LiveData<ProvisionState> = stateLiveData

    init {
        provisioningManager.state
            .onEach {
                stateLiveData.value =
                    it // we can map state here if we need more control on presentation layer
            }.launchIn(viewModelScope)
    }

    fun initializeApp() {
        provisioningManager.initializeApp()
    }

    fun restoreData(documentFile: DocumentFile?) {
        viewModelScope.launch {
            provisioningManager.restoreData(documentFile)
        }
    }
}
