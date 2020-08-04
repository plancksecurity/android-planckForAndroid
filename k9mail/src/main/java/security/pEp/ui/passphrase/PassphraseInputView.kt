package security.pEp.ui.passphrase

interface PassphraseInputView {
    fun init()
    fun initAffirmativeListeners()
    fun enableSyncDismiss()
    fun enableNonSyncDismiss()
    fun finish()
    fun enableActionConfirmation(enabled: Boolean)
    fun showRetryPasswordRequest()
    fun showPasswordRequest()
    fun showSyncPasswordRequest()
}