package com.fsck.k9.pEp.infrastructure.extensions

private const val STACKTRACE_AT = "\nat\n"

fun Throwable.getStackTrace(depth: Int): String {
    return if (depth < 0) stackTraceToString() else {
        "${javaClass.name}: $message" +
                this.stackTrace.take(depth).joinToString(STACKTRACE_AT, prefix = STACKTRACE_AT)
    }
}