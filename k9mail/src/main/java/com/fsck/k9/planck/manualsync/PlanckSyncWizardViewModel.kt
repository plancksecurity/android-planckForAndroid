package com.fsck.k9.planck.manualsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlanckSyncWizardViewModel @Inject constructor(
    private val planckProvider: PlanckProvider
) : ViewModel() {
    private val internalSyncState = MutableLiveData<SyncScreenState>(SyncScreenState.Idle)
    val syncState: LiveData<SyncScreenState> = internalSyncState

    var formingGroup = false

    fun next() {
        TODO("Not yet implemented")
    }

    fun rejectHandshake() {
        TODO("Not yet implemented")
    }

    fun acceptHandshake() {
        TODO("Not yet implemented")
    }

    fun cancelHandshake() {
        TODO("Not yet implemented")
    }
}