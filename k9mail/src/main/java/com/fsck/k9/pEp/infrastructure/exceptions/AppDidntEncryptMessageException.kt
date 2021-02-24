package com.fsck.k9.pEp.infrastructure.exceptions

import com.fsck.k9.activity.MessageReference

class AppDidntEncryptMessageException constructor(messageReference: MessageReference) :
    MessageRelatedException("Could not sent last message Secure", messageReference)