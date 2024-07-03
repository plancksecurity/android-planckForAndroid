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
import javax.inject.Inject

@AndroidEntryPoint
class ProvisioningActivity : AppCompatActivity(), ProvisioningView, SplashScreen {
    private lateinit var binding: ActivityProvisioningBinding
    private lateinit var folderPickerLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var presenter: ProvisioningPresenter

    private lateinit var waitingForProvisioningText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding = ActivityProvisioningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        waitingForProvisioningText = binding.waitingForProvisioningText
        progressBar = binding.provisioningProgress
        binding.provisioningSkipButton.setOnClickListener { presenter.initializeApp() }
        binding.provisioningRestoreDataButton.setOnClickListener { pickFolder() }

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.e("EFA-625", "RESULT CODE: ${result.resultCode}, DATA: ${result.data}")
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    restoreDataFromSelectedFolder(uri)
                }
            }
            presenter.initializeApp()
        }
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
        runOnUiThread {
            hideRestoreButtons()
            waitingForProvisioningText.isVisible = true
            waitingForProvisioningText.setText(R.string.initializing_application)
        }
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
        val documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val initialFolderUri: Uri = Uri.fromFile(documentsFolder)
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialFolderUri)
        }
        folderPickerLauncher.launch(intent)
    }

    private fun restoreDataFromSelectedFolder(folderUri: Uri) {
        Log.e("EFA-625", "FOLDER URI: $folderUri")
        val contentResolver = applicationContext.contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(folderUri, takeFlags)

        val documentFile = DocumentFile.fromTreeUri(this, folderUri)
        presenter.restoreData(documentFile)
    }
}
