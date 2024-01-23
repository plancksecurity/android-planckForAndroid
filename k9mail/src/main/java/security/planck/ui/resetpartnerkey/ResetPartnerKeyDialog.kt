package security.planck.ui.resetpartnerkey

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.fsck.k9.R
import dagger.hilt.android.AndroidEntryPoint
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.dialog.SimpleBackgroundTaskDialog

private const val DIALOG_TAG = "security.planck.ui.resetpartnerkey.ResetPartnerKeyDialog"
private const val ARG_PARTNER = "security.planck.ui.resetpartnerkey.ResetPartnerKeyDialog.partner"

@AndroidEntryPoint
class ResetPartnerKeyDialog : SimpleBackgroundTaskDialog() {
    private val viewModel: ResetPartnerKeyViewModel by viewModels()

    override val title: String
        get() = getString(R.string.reset_partner_keys_title)
    override val progressMessage: String
        get() = getString(R.string.reset_partner_keys_progress)
    override val confirmationMessage: String
        get() = getString(R.string.reset_partner_keys_description)
    override val failureMessage: String
        get() = getString(R.string.reset_partner_keys_failure)
    override val successMessage: String
        get() = getString(R.string.reset_partner_keys_successful_result)
    override val actionText: String
        get() = getString(R.string.reset_partner_keys_confirmation_action)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            arguments?.let { arguments ->
                val partner = arguments.getString(ARG_PARTNER)
                viewModel.initialize(partner)
            }
        }
    }

    override fun dialogFinished() {
        viewModel.partnerKeyResetFinished()
    }

    override fun taskTriggered() {
        viewModel.resetPlanckData()
    }

    override fun dialogInitialized() {
        viewModel.resetPartnerKeyState.observe(viewLifecycleOwner) { state ->
            showState(state)
            if (state == BackgroundTaskDialogView.State.SUCCESS) {
                setFragmentResult(REQUEST_KEY, bundleOf(RESULT_KEY_SUCCESS to true))
            }
        }
    }

    companion object {
        const val REQUEST_KEY = DIALOG_TAG
        const val RESULT_KEY_SUCCESS =
            "security.planck.ui.resetpartnerkey.ResetPartnerKeyDialog.Success"
    }
}

private fun newInstance(partner: String) = ResetPartnerKeyDialog().apply {
    arguments = bundleOf(ARG_PARTNER to partner)
}

private fun createAndShowResetPartnerKeyDialog(
    fragmentManager: FragmentManager,
    partner: String
) {
    val fragment = newInstance(
        partner
    )
    fragmentManager
        .beginTransaction()
        .add(fragment, DIALOG_TAG)
        .commitAllowingStateLoss()
}

fun Fragment.showResetPartnerKeyDialog(partner: String) {
    createAndShowResetPartnerKeyDialog(parentFragmentManager, partner)
}

fun AppCompatActivity.showResetPartnerKeyDialog(partner: String) {
    createAndShowResetPartnerKeyDialog(supportFragmentManager, partner)
}