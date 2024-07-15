package com.fsck.k9.planck.ui.activities.provisioning


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.documentfile.provider.DocumentFile
import com.fsck.k9.R
import com.fsck.k9.activity.K9Activity.NO_ANIMATION
import com.fsck.k9.activity.SettingsActivity
import com.fsck.k9.databinding.ActivityProvisioningBinding
import com.fsck.k9.planck.ui.activities.SplashScreen
import dagger.hilt.android.AndroidEntryPoint
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningFailedException

@AndroidEntryPoint
class ProvisioningActivity : AppCompatActivity(), ProvisioningView, SplashScreen {
    private lateinit var binding: ActivityProvisioningBinding
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>
    private val viewModel: ProvisioningViewModel by viewModels()
    private lateinit var waitingForProvisioningText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun observeViewModel() {
        this.viewModel.state.observe(this) { state ->
            state?.let { renderState(state) }
        }
    }

    private fun renderState(state: ProvisionState) {
        when (state) {
            is ProvisionState.InProvisioning ->
                provisioningProgress()

            is ProvisionState.WaitingToInitialize -> {
                if (state.offerRestore) {
                    offerRestorePlanckData()
                } else {
                    initializing()
                }
            }

            is ProvisionState.Initializing ->
                if (state.provisioned) {
                    initializingAfterSuccessfulProvision()
                } else {
                    initializing()
                }

            is ProvisionState.Initialized ->
                initialized()

            is ProvisionState.Error -> {
                val throwableMessage = state.throwable.message
                val message =
                    if (throwableMessage.isNullOrBlank())
                        state.throwable.stackTraceToString()
                    else throwableMessage
                if (state.throwable is ProvisioningFailedException) {
                    displayProvisioningError(message)
                } else {
                    displayInitializationError(message)
                }
            }

            is ProvisionState.DbImportFailed ->
                displayDbImportFailed(state.throwable.message.orEmpty())
        }
    }

    private fun setupViews() {
        binding = ActivityProvisioningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        waitingForProvisioningText = binding.waitingForProvisioningText
        progressBar = binding.provisioningProgress
        binding.provisioningSkipButton.setOnClickListener { this.viewModel.initializeApp() }
        binding.provisioningRestoreDataButton.setOnClickListener { pickFolder() }

        folderPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        restoreDataFromSelectedFolder(uri)
                    }
                }
            }
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(NO_ANIMATION, NO_ANIMATION)
    }

    override fun provisioningProgress() {
        waitingForProvisioningText.setText(R.string.provisioning_in_progress)
    }

    override fun initializing() {
        hideRestoreButtons()
        waitingForProvisioningText.isVisible = true
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

    override fun displayDbImportFailed(message: String) {
        binding.provisioningRestoreDataButton.isEnabled = false
        displayError(R.string.provisioning_db_import_error_msg, message)
    }

    private fun displayError(@StringRes stringResource: Int, message: String) {
        val errorColor = ContextCompat.getColor(
            this,
            R.color.compose_unsecure_delivery_warning
        )
        waitingForProvisioningText.isVisible = true
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

    override fun offerRestorePlanckData() {
        waitingForProvisioningText.isVisible = false
        showRestoreButtons()
    }

    private fun showRestoreButtons() {
        binding.provisioningSkipButton.isVisible = true
        binding.provisioningRestoreDataButton.isVisible = true
    }

    private fun hideRestoreButtons() {
        binding.provisioningSkipButton.isVisible = false
        binding.provisioningRestoreDataButton.isVisible = false
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        folderPickerLauncher.launch(intent)
    }

    private fun restoreDataFromSelectedFolder(folderUri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(this, folderUri)
        this.viewModel.restoreData(documentFile)
    }
}
