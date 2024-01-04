package security.planck.ui.leavedevicegroup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.BuildConfig
import com.fsck.k9.planck.PlanckProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.sync.SyncRepository
import timber.log.Timber
import javax.inject.Inject

private const val DEVICE_GROUPED_CHECK_ITERATIONS = 5
private const val DEVICE_GROUPED_CHECK_INTERVAL = 200L

/**
 * LeaveDeviceGroupViewModel
 *
 * ViewModel used to leave device group and deliver confirmation via LiveData.
 */
@HiltViewModel
class LeaveDeviceGroupViewModel @Inject constructor(
    private val planckProvider: PlanckProvider,
    private val syncRepository: SyncRepository,
) : ViewModel() {

    private val stateLD: MutableLiveData<BackgroundTaskDialogView.State> =
        MutableLiveData(BackgroundTaskDialogView.State.CONFIRMATION)

    /**
     * state
     *
     * Current state of the ViewModel represented as LiveData of [BackgroundTaskDialogView.State].
     */
    val state: LiveData<BackgroundTaskDialogView.State> = stateLD

    /**
     * leaveDeviceGroup
     *
     * Leaves device group and updates [state].
     */
    fun leaveDeviceGroup() {
        viewModelScope.launch {
            stateLD.value = BackgroundTaskDialogView.State.LOADING
            planckProvider.leaveDeviceGroup().onSuccessSuspend {
                repeat(DEVICE_GROUPED_CHECK_ITERATIONS) {
                    if (!planckProvider.isDeviceGrouped()) {
                        return@repeat
                    }
                    Timber.e("planckProvider.isDeviceGrouped returned true, trying again...")
                    delay(DEVICE_GROUPED_CHECK_INTERVAL)
                }
                syncRepository.isGrouped = false
                stateLD.value =
                    if (BuildConfig.DEBUG && planckProvider.isDeviceGrouped()) {
                        Timber.e("CANNOT LEAVE DEVICE GROUP!!!")
                        BackgroundTaskDialogView.State.ERROR
                    } else
                        BackgroundTaskDialogView.State.SUCCESS
            }.onFailure {
                Timber.e(it)
                stateLD.value = BackgroundTaskDialogView.State.ERROR
            }
        }
    }
}