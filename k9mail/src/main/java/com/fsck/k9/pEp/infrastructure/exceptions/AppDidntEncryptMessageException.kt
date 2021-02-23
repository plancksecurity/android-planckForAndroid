package com.fsck.k9.pEp.infrastructure.exceptions

import com.fsck.k9.activity.MessageReference

class AppDidntEncryptMessageException constructor(val messageReference: MessageReference) :
        RuntimeException("Could not sent last message Secure")