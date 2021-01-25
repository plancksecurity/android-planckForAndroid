package com.fsck.k9.pEp

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend inline fun<T, reified Ex: Throwable> withContextCatchException(
        coroutineContext: CoroutineContext,
        externalStackTrace: Array<StackTraceElement>,
        crossinline tryBlock: () -> T, crossinline catchBlock: (Ex) -> T): T =
        withContext(coroutineContext) {
            try {
                tryBlock()
            }
            catch (e: Throwable) {
                if(e is Ex) {
                    val stacktrace = e.stackTrace
                    val newStackTrace = stacktrace + externalStackTrace
                    e.stackTrace = newStackTrace
                    catchBlock(e)
                }
                else throw e
            }
        }