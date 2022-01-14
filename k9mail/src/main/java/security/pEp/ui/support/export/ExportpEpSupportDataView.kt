package security.pEp.ui.support.export

interface ExportpEpSupportDataView {
    fun finish()
    fun showSuccess()
    fun showFailed()
    fun showNotEnoughSpaceInDevice(
        neededSpace: Long,
        availableSpace: Long,
    )

    fun showLoading()
    fun hideLoading()
}