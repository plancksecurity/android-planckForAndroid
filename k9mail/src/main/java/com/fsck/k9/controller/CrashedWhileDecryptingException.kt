package com.fsck.k9.controller

import com.fsck.k9.mail.MessagingException

class CrashedWhileDecryptingException(messageId: String): MessagingException(
    "App crashed while decrypting message $messageId. " +
            "This message will be stored encrypted in the database."
)
