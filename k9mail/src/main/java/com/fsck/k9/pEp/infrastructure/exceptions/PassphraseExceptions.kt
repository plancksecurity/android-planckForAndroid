package com.fsck.k9.pEp.infrastructure.exceptions

class AuthFailureWrongPassphrase : java.lang.RuntimeException("Wrong Passphrase")

class AuthFailurePassphraseNeeded : java.lang.RuntimeException("Passphrase required")