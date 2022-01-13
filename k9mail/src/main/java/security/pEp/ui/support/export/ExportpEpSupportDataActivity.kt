package security.pEp.ui.support.export

import android.os.Bundle
import android.widget.Button
import com.fsck.k9.R
import com.fsck.k9.pEp.manualsync.WizardActivity
import javax.inject.Inject

class ExportpEpSupportDataActivity : WizardActivity(), ExportpEpSupportDataView {
    @Inject
    lateinit var presenter: ExportpEpSupportDataPresenter
    private lateinit var exportButton: Button
    private lateinit var cancelButton: Button

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

        exportButton.setOnClickListener { presenter.export() }
        cancelButton.setOnClickListener { presenter.cancel() }
    }
}
