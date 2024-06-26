package security.planck.ui.passphrase.old

interface PassphraseInputView {
    fun init()
    fun initAffirmativeListeners()
    fun enableSyncDismiss()
    fun enableNonSyncDismiss()
    fun finish(passphraseAdded: Boolean = false)
    fun enableActionConfirmation(enabled: Boolean)
    fun showPassphraseError()
    fun hidePassphraseError()
    fun showRetryPasswordRequest()
    fun showPasswordRequest()
    fun showSyncPasswordRequest()
    fun showNewKeysPassphrase()
    fun showNewKeysPassphraseForAcountCreation(email: String)
}