package com.fsck.k9.planck.infrastructure.exceptions

class CannotCreateMessageException(cause: Throwable) :
    RuntimeException("Cannot create message", cause)