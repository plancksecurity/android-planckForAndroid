package security.planck.ui.passphrase

interface PassphraseInputView {
    fun init()
    fun initAffirmativeListeners()
    fun enableSyncDismiss()
    fun enableNonSyncDismiss()
    fun finish(passphraseAdded: Boolean = false)
    fun enableActionConfirmation(enabled: Boolean)
    fun showRetryPasswordRequest()
    fun showPasswordRequest()
    fun showSyncPasswordRequest()
    fun showNewKeysPassphrase()
}