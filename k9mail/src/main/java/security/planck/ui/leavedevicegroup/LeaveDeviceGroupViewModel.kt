package security.planck.ui.leavedevicegroup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.sync.SyncRepository
import timber.log.Timber
import javax.inject.Inject

private const val DEVICE_GROUPED_CHECK_ITERATIONS = 5
private const val DEVICE_GROUPED_CHECK_INTERVAL = 100L

@HiltViewModel
class LeaveDeviceGroupViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val stateLD: MutableLiveData<BackgroundTaskDialogView.State> =
        MutableLiveData(BackgroundTaskDialogView.State.CONFIRMATION)
    val state: LiveData<BackgroundTaskDialogView.State> = stateLD

    fun leaveDeviceGroup() {
        viewModelScope.launch {
            stateLD.value = BackgroundTaskDialogView.State.LOADING
            planckProvider.leaveDeviceGroup().onSuccessSuspend {
                repeat(DEVICE_GROUPED_CHECK_ITERATIONS) {
                    if (!planckProvider.isDeviceGrouped) {
                        return@repeat
                    }
                    Timber.e("planckProvider.isDeviceGrouped returned true, trying again...")
                    delay(DEVICE_GROUPED_CHECK_INTERVAL)
                }
                syncRepository.isGrouped = false
                stateLD.value = BackgroundTaskDialogView.State.SUCCESS
            }.onFailure {
                Timber.e(it)
                stateLD.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }
}