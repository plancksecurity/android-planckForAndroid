package security.planck.ui.leavedevicegroup

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.fsck.k9.R
import dagger.hilt.android.AndroidEntryPoint
import security.planck.dialog.SimpleBackgroundTaskDialog

@AndroidEntryPoint
class LeaveDeviceGroupDialog : SimpleBackgroundTaskDialog() {
    private val viewModel: LeaveDeviceGroupViewModel by viewModels()
    override val title: String
        get() = getString(R.string.pep_sync_leave_device_group)
    override val progressMessage: String
        get() = getString(R.string.leave_device_group_dialog_progress_text)
    override val confirmationMessage: String
        get() = getString(R.string.leave_device_group_dialog_confirmation_text)
    override val failureMessage: String
        get() = getString(R.string.leave_device_group_dialog_error_text)
    override val successMessage: String
        get() = getString(R.string.leave_device_group_dialog_success_text)
    override val actionText: String
        get() = getString(R.string.keysync_wizard_action_leave)

    override fun taskTriggered() {
        viewModel.leaveDeviceGroup()
    }

    override fun dialogInitialized() {
        viewModel.state.observe(viewLifecycleOwner) {
            showState(it)
        }
    }

    override fun dialogCancelled() {
        setFragmentResult(DIALOG_TAG, bundleOf(RESULT_KEY to CANCELLED))
    }

    override fun dialogFinished() {
        setFragmentResult(DIALOG_TAG, bundleOf(RESULT_KEY to EXECUTED))
    }

    companion object {
        const val RESULT_KEY = "LeaveDeviceGroupDialog"
        const val DIALOG_TAG = "LEAVE_DEVICE_GROUP"
        const val CANCELLED = 0
        const val EXECUTED = 1

        fun newInstance(): LeaveDeviceGroupDialog = LeaveDeviceGroupDialog()
    }
}

fun Fragment.showLeaveDeviceGroupDialog() {
    val fragment = LeaveDeviceGroupDialog.newInstance()
    parentFragmentManager
        .beginTransaction()
        .add(fragment, LeaveDeviceGroupDialog.DIALOG_TAG)
        .commitAllowingStateLoss()
}