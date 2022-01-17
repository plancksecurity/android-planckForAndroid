package security.pEp.ui.support.export

import android.content.Context

interface ExportpEpSupportDataView {
    fun finish()
    fun showSuccess()
    fun showFailed()
    fun showNotEnoughSpaceInDevice()

    fun showLoading()
    fun hideLoading()
    fun getContext(): Context
}