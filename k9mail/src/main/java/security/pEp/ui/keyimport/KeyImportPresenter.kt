package security.pEp.ui.keyimport

import javax.inject.Inject

class KeyImportPresenter @Inject constructor() {

    private lateinit var view: KeyImportView
    private lateinit var account: String

    fun initialize(view: KeyImportView, account: String) {
        this.view = view
        this.account = account
        view.renderDialog()
    }

    fun onAccept() {
        view.showPositiveFeedback()
        view.finish()
    }

    fun onReject() {
        view.showNegativeFeedback()
        view.finish()
    }

}