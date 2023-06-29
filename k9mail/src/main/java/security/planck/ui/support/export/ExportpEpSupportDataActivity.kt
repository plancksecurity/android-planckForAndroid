package security.planck.ui.support.export

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import com.fsck.k9.R
import com.fsck.k9.planck.manualsync.WizardActivity
import javax.inject.Inject

class ExportpEpSupportDataActivity : WizardActivity(), ExportpEpSupportDataView {
    @Inject
    lateinit var presenter: ExportPlanckSupportDataPresenter
    private lateinit var exportButton: Button
    private lateinit var cancelButton: Button
    private lateinit var messageText: TextView
    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var successFailureImage: ImageView

    override fun inject() {
        getPlanckComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        restoreNonConfigurationInstance()
        presenter.initialize(this, lifecycle)
    }

    private fun restoreNonConfigurationInstance() {
        val retainedPresenter = lastCustomNonConfigurationInstance
        if (retainedPresenter is ExportPlanckSupportDataPresenter) {
            presenter = retainedPresenter
        }
    }

    override fun onRetainCustomNonConfigurationInstance(): Any {
        return presenter
    }

    private fun setupViews() {
        setContentView(R.layout.activity_export_pep_support_data)
        setUpFloatingWindow()

        exportButton = findViewById(R.id.affirmativeActionButton)
        cancelButton = findViewById(R.id.dismissActionButton)
        messageText = findViewById(R.id.message)
        progressBar = findViewById(R.id.loadingProgressBar)
        successFailureImage = findViewById(R.id.successFailureImage)

        exportButton.setOnClickListener { presenter.export() }
        cancelButton.setOnClickListener { presenter.cancel() }
    }

    override fun showSuccess() {
        showOkAction()
        messageText.setText(R.string.export_pep_support_data_dialog_success_msg)
        successFailureImage.setImageResource(R.drawable.ic_success_planck_24dp)
        successFailureImage.contentDescription =
            getString(R.string.export_pep_support_data_dialog_image_success_content_desc)
    }

    override fun showFailed() {
        showOkAction()
        messageText.setText(R.string.export_pep_support_data_dialog_failure_msg)
        successFailureImage.setImageResource(R.drawable.ic_failure_red_24dp)
        successFailureImage.contentDescription =
            getString(R.string.export_pep_support_data_dialog_image_failure_content_desc)
    }

    override fun showNotEnoughSpaceInDevice() {
        showOkAction()
        messageText.setText(R.string.export_pep_support_data_dialog_not_enough_space_msg)
        successFailureImage.setImageResource(R.drawable.ic_failure_red_24dp)
        successFailureImage.contentDescription =
            getString(R.string.export_pep_support_data_dialog_image_failure_content_desc)
    }

    override fun showLoading() {
        progressBar.show()
        hideDialogContent()
    }

    override fun hideLoading() {
        showDialogContent()
        progressBar.hide()
    }

    private fun showDialogContent() {
        exportButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
        messageText.visibility = View.VISIBLE
        successFailureImage.visibility = View.VISIBLE
    }

    private fun hideDialogContent() {
        exportButton.visibility = View.INVISIBLE
        cancelButton.visibility = View.INVISIBLE
        messageText.visibility = View.INVISIBLE
        successFailureImage.visibility = View.INVISIBLE
    }

    private fun showOkAction() {
        exportButton.setText(R.string.okay_action)
        exportButton.setOnClickListener { presenter.cancel() }
        cancelButton.visibility = View.GONE
    }

    companion object {
        fun showExportPEpSupportDataDialog(activity: Activity) {
            val intent = Intent(activity, ExportpEpSupportDataActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
