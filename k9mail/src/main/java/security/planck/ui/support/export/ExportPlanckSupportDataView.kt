package security.planck.ui.support.export

interface ExportPlanckSupportDataView {
    fun finish()
    fun showSuccess()
    fun showFailed()
    fun showNotEnoughSpaceInDevice()

    fun showLoading()
    fun hideLoading()
}