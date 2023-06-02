package com.fsck.k9.planck.infrastructure.exceptions

class AuthFailureWrongPassphrase : java.lang.RuntimeException("Wrong Passphrase")

class AuthFailurePassphraseNeeded : java.lang.RuntimeException("Passphrase required")