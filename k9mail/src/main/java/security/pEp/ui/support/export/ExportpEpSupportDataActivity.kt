package security.pEp.ui.support.export

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.pEp.manualsync.WizardActivity
import javax.inject.Inject

class ExportpEpSupportDataActivity : WizardActivity(), ExportpEpSupportDataView {
    @Inject
    lateinit var presenter: ExportpEpSupportDataPresenter
    private lateinit var exportButton: Button
    private lateinit var cancelButton: Button
    private lateinit var messageText: TextView

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupViews()
        presenter.initialize(this)
    }

    private fun setupViews() {
        setContentView(R.layout.activity_export_pep_support_data)
        setUpFloatingWindow()

        exportButton = findViewById(R.id.affirmativeActionButton)
        cancelButton = findViewById(R.id.dismissActionButton)
        messageText = findViewById(R.id.message)

        exportButton.setOnClickListener { presenter.export() }
        cancelButton.setOnClickListener { presenter.cancel() }
    }

    override fun showSuccess() {
        showOkAction()
        messageText.setText(R.string.export_pep_support_data_dialog_success_msg)
    }

    override fun showFailed() {
        showOkAction()
        messageText.setText(R.string.export_pep_support_data_dialog_failure_msg)
    }

    private fun showOkAction() {
        exportButton.setText(R.string.okay_action)
        exportButton.setOnClickListener { presenter.cancel() }
        cancelButton.visibility = View.GONE
    }
}
