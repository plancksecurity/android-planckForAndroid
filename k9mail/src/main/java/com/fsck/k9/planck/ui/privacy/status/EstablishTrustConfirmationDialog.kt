package com.fsck.k9.planck.ui.privacy.status

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fsck.k9.R
import dagger.hilt.android.AndroidEntryPoint
import security.planck.dialog.SimpleBackgroundTaskDialog
import javax.inject.Inject

private const val DIALOG_TAG = "EstablishTrustConfirmationDialog"

@AndroidEntryPoint
class EstablishTrustConfirmationDialog : SimpleBackgroundTaskDialog() {
    @Inject
    lateinit var presenter: PlanckStatusPresenter
    private val trust: Boolean by lazy {
        requireArguments().getBoolean(TRUST)
    }
    private val email: String by lazy {
        requireArguments().getString(EMAIL)!!
    }
    override val title: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_title)
        else getString(R.string.reject_trust_dialog_title)
    override val progressMessage: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_progress, email)
        else getString(R.string.reject_trust_dialog_progress, email)
    override val confirmationMessage: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_confirmation, email)
        else getString(R.string.reject_trust_dialog_confirmation, email)
    override val failureMessage: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_failure, email)
        else getString(R.string.reject_trust_dialog_failure, email)
    override val successMessage: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_success, email)
        else getString(R.string.reject_trust_dialog_success, email)
    override val actionText: String
        get() = if (trust) getString(R.string.confirm_trust_dialog_positive_action)
        else getString(R.string.reject_trust_dialog_positive_action)


    override fun dialogFinished() {
        presenter.handshakeFinished()
    }

    override fun taskTriggered() {
        presenter.performHandshake()
    }

    override fun dialogInitialized() {
        presenter.initializeTrustConfirmationView(this)
    }

    companion object {
        private const val TRUST = "EstablishTrustConfirmationDialog.trust"
        private const val EMAIL = "EstablishTrustConfirmationDialog.email"

        @JvmStatic
        fun showEstablishTrustConfirmationDialog(
            activity: AppCompatActivity,
            trust: Boolean,
            email: String,
        ) {
            val fragment = EstablishTrustConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putBoolean(TRUST, trust)
                    putString(EMAIL, email)
                }
            }

            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, DIALOG_TAG)
                .commitAllowingStateLoss()
        }
    }
}