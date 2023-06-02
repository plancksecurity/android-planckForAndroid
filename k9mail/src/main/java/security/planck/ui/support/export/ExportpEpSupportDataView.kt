package security.planck.ui.support.export

interface ExportpEpSupportDataView {
    fun finish()
    fun showSuccess()
    fun showFailed()
    fun showNotEnoughSpaceInDevice()

    fun showLoading()
    fun hideLoading()
}