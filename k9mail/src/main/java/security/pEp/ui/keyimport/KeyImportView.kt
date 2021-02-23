package security.pEp.ui.keyimport

import foundation.pEp.jniadapter.Identity

interface KeyImportView {

    fun openFileChooser()

    fun showCorrectKeyImport(fingerprint: String, filename: String?)

    fun showFailedKeyImport(filename: String?)

    fun finish()

    fun showLoading()

    fun hideLoading()

    fun showKeyImportConfirmationDialog(importedIdentities: List<Identity>, filename: String)

    fun showKeyImportResult(result: Map<Identity, Boolean>, filename: String)
}