package security.pEp.ui.keyimport

import android.content.Context
import foundation.pEp.jniadapter.Identity

interface KeyImportView {

    fun openFileChooser()

    fun showCorrectKeyImport()

    fun showFailedKeyImport(filename: String?)

    fun finish()

    fun getApplicationContext(): Context

    fun showLoading(showMessage: Boolean = true)

    fun hideLoading()

    fun showKeyImportConfirmationDialog(firstIdentity: Identity, filename: String)

    fun show()
}