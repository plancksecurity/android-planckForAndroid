package security.pEp.ui.keyimport

import android.content.Context
import foundation.pEp.jniadapter.Identity

interface KeyImportView {

    fun openFileChooser()

    fun showCorrectKeyImport(fingerprint: String, filename: String?)

    fun showFailedKeyImport(filename: String?)

    fun finish()

    fun getApplicationContext(): Context

    fun showLoading()

    fun hideLoading()

    fun showKeyImportConfirmationDialog(firstIdentity: Identity, filename: String)

    fun showLayout(show: Boolean)
}