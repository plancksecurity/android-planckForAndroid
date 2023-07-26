package com.fsck.k9.planck.ui.activities.provisioning


import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity.NO_ANIMATION
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.planck.ui.activities.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProvisioningActivity : AppCompatActivity(), ProvisioningView, SplashScreen {
    @Inject
    lateinit var presenter: ProvisioningPresenter

    private lateinit var waitingForProvisioningText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        setContentView(R.layout.activity_provisioning)
        waitingForProvisioningText = findViewById(R.id.waitingForProvisioningText)
        progressBar = findViewById(R.id.provisioning_progress)
    }

    override fun onStart() {
        super.onStart()
        presenter.attach(this)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(NO_ANIMATION, NO_ANIMATION)
    }

    override fun onStop() {
        super.onStop()
        presenter.detach()
    }

    override fun waitingForProvisioning() {
        waitingForProvisioningText.setText(R.string.waiting_for_provisioning)
    }

    override fun provisioningProgress() {
        waitingForProvisioningText.setText(R.string.provisioning_in_progress)
    }

    override fun initializing() {
        waitingForProvisioningText.setText(R.string.initializing_application)
    }

    override fun initializingAfterSuccessfulProvision() {
        waitingForProvisioningText.setText(
            R.string.provisioning_successful_initializing_application
        )
    }

    override fun initialized() {
        waitingForProvisioningText.setText(R.string.initialization_complete)
        SettingsActivity.actionBasicStart(this)
        finish()
    }

    override fun displayInitializationError(message: String) {
        displayError(R.string.initialization_error_template, message)
    }

    override fun displayProvisioningError(message: String) {
        displayError(R.string.provisioning_error_template, message)
    }

    private fun displayError(@StringRes stringResource: Int, message: String) {
        val errorColor = ContextCompat.getColor(
            this,
            R.color.compose_unsecure_delivery_warning
        )
        waitingForProvisioningText.setTextColor(errorColor)
        waitingForProvisioningText.text = getString(stringResource, message)
        progressBar.indeterminateDrawable.setColorFilter(
            errorColor,
            android.graphics.PorterDuff.Mode.MULTIPLY
        )
    }

    override fun displayUnknownError(trace: String) {
        waitingForProvisioningText.text = trace
    }
}
