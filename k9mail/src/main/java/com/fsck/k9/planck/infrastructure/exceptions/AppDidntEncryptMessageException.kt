package com.fsck.k9.planck.infrastructure.exceptions

import com.fsck.k9.mail.internet.MimeMessage

class AppDidntEncryptMessageException(val mimeMessage: MimeMessage) :
        RuntimeException("Could not sent last message Secure")