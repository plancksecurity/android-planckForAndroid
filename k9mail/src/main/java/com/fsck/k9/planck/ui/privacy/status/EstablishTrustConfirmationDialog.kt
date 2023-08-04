package com.fsck.k9.planck.ui.privacy.status

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import security.planck.dialog.SimpleBackgroundTaskDialog
import javax.inject.Inject

private const val DIALOG_TAG = "EstablishTrustConfirmationDialog"

abstract class EstablishTrustConfirmationDialog : SimpleBackgroundTaskDialog() {
    @Inject
    lateinit var presenter: PlanckStatusPresenter
    protected val email: String by lazy {
        requireArguments().getString(EMAIL)!!
    }

    override fun dialogFinished() {
        presenter.handshakeFinished()
    }

    override fun taskTriggered() {
        presenter.performHandshake()
    }

    override fun dialogInitialized() {
        presenter.initializeTrustConfirmationView(this)
    }

    override fun dialogCancelled() {
        presenter.handshakeCancelled()
    }

    companion object {
        private const val EMAIL = "EstablishTrustConfirmationDialog.email"

        @JvmStatic
        fun showTrustConfirmationDialog(
            activity: AppCompatActivity,
            email: String,
        ) {
            val fragment = TrustConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(EMAIL, email)
                }
            }
            showDialog(activity, fragment)
        }

        @JvmStatic
        fun showMistrustConfirmationDialog(
            activity: AppCompatActivity,
            email: String,
        ) {
            val fragment = MistrustConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(EMAIL, email)
                }
            }
            showDialog(activity, fragment)
        }

        private fun showDialog(
            activity: AppCompatActivity,
            fragment: EstablishTrustConfirmationDialog
        ) {
            activity.supportFragmentManager
                .beginTransaction()
                .add(fragment, DIALOG_TAG)
                .commitAllowingStateLoss()
        }
    }
}