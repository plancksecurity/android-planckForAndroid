package security.pEp.ui.support.export

import android.os.Bundle
import com.fsck.k9.R
import com.fsck.k9.pEp.manualsync.WizardActivity
import javax.inject.Inject

class ExportpEpSupportDataActivity : WizardActivity(), ExportpEpSupportDataView {
    @Inject
    lateinit var presenter: ExportpEpSupportDataPresenter

    override fun inject() {
        getpEpComponent().inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_pep_support_data)
        setUpFloatingWindow()
        presenter.initialize(this)
    }
}
