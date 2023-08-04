package com.fsck.k9.planck.ui.privacy.status

import com.fsck.k9.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrustConfirmationDialog : EstablishTrustConfirmationDialog() {
    override val title: String
        get() = getString(R.string.confirm_trust_dialog_title)
    override val progressMessage: String
        get() = getString(R.string.confirm_trust_dialog_progress, email)
    override val confirmationMessage: String
        get() = getString(R.string.confirm_trust_dialog_confirmation, email)
    override val failureMessage: String
        get() = getString(R.string.confirm_trust_dialog_failure, email)
    override val successMessage: String
        get() = getString(R.string.confirm_trust_dialog_success, email)
    override val actionText: String
        get() = getString(R.string.confirm_trust_dialog_positive_action)
}