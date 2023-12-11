package com.fsck.k9.ui.messageview

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fsck.k9.R
import dagger.hilt.android.AndroidEntryPoint
import security.planck.dialog.SimpleBackgroundTaskDialog
import javax.inject.Inject

private const val DIALOG_TAG = "PARTNER_KEY_RESET"

@AndroidEntryPoint
class ResetPartnerKeyDialog : SimpleBackgroundTaskDialog() {
    private val viewModel: MessageViewViewModel by viewModels(ownerProducer = { requireParentFragment() })

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

    override fun dialogFinished() {
        viewModel.partnerKeyResetFinished()
    }

    override fun taskTriggered() {
        viewModel.resetPlanckData()
    }

    override fun dialogInitialized() {
        viewModel.resetPartnerKeyState.observe(viewLifecycleOwner) {
            showState(it)
        }
    }

    companion object {
        private fun newInstance() = ResetPartnerKeyDialog()

        @JvmStatic
        fun Fragment.showResetPartnerKeyDialog() {
            val fragment = newInstance()
            childFragmentManager
                .beginTransaction()
                .add(fragment, DIALOG_TAG)
                .commitAllowingStateLoss()
        }
    }
}