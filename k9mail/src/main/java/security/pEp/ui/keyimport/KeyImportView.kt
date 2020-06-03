package security.pEp.ui.keyimport

interface KeyImportView {

    fun openFileChooser()

    fun showNegativeFeedback(message: String)

    fun finish()

}