package com.fsck.k9.pEp.infrastructure.exceptions

import com.fsck.k9.activity.MessageReference

open class MessageRelatedException : RuntimeException {
    val messageReference: MessageReference

    constructor(message: String, messageReference: MessageReference) : super(message) {
        this.messageReference = messageReference
    }

    constructor(cause: Throwable, messageReference: MessageReference) : super(cause) {
        this.messageReference = messageReference
    }
}