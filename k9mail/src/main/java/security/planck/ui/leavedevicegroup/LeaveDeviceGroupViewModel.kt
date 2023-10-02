package security.planck.ui.leavedevicegroup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import security.planck.dialog.BackgroundTaskDialogView
import timber.log.Timber
import javax.inject.Inject

private const val DEVICE_GROUPED_CHECK_ITERATIONS = 5
private const val DEVICE_GROUPED_CHECK_INTERVAL = 100L

@HiltViewModel
class LeaveDeviceGroupViewModel @Inject constructor(
    private val k9: K9,
    private val planckProvider: PlanckProvider,
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
                    delay(DEVICE_GROUPED_CHECK_INTERVAL)
                }
                k9.isGrouped = false
                stateLD.value = BackgroundTaskDialogView.State.SUCCESS
            }.onFailure {
                Timber.e(it)
                stateLD.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }
}