package security.pEp.ui.keyimport

import android.content.Context
import foundation.pEp.jniadapter.Identity

interface KeyImportView {

    fun openFileChooser()

    fun showCorrectKeyImport()

    fun showFailedKeyImport()

    fun finish()

    fun getApplicationContext(): Context

    fun showLoading()

    fun hideLoading()

    fun showKeyImportConfirmationDialog(firstIdentity: Identity, filename: String)

    fun showLayout()
}