package security.planck.ui.passphrase.models

import foundation.pEp.jniadapter.PassphraseEntry

data class PlanckPassphraseEntry(val email: String = "", val passphrase: String = "") {
    fun toPassphraseEntry(): PassphraseEntry = PassphraseEntry(email, passphrase)
    fun isBlank() = email.isBlank() || passphrase.isBlank()
    fun isNotBlank() = !isBlank()
}
