package com.fsck.k9.pEp.testutils

sealed class ReturnBehavior<T> {
    class Throw<T>(val e: Throwable) : ReturnBehavior<T>()
    class Return<T>(val value: T?) : ReturnBehavior<T>()
}