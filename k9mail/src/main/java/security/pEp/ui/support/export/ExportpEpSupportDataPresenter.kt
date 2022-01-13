package security.pEp.ui.support.export

import javax.inject.Inject

class ExportpEpSupportDataPresenter @Inject constructor() {
    private lateinit var view: ExportpEpSupportDataView

    fun initialize(view: ExportpEpSupportDataView) {
        this.view = view
    }

    fun export() {

    }

    fun cancel() {
        view.finish()
    }
}
