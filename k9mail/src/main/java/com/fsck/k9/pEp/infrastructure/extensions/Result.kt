package com.fsck.k9.pEp.infrastructure.extensions

suspend fun <Type, NewType> Result<Type>.flatMapSuspend(block: suspend (Type) -> Result<NewType>): Result<NewType> =
    fold(
        onSuccess = { block(it) },
        onFailure = { Result.failure(it) }
    )

fun <Type> Result<Type>.mapError(block: (Throwable) -> Throwable): Result<Type> =
    fold(
        onSuccess = { this },
        onFailure = { Result.failure(block(it)) }
    )

inline fun <T, R> Iterable<T>.mapSuccess(transform: (T) -> Result<R>): List<R> {
    return mapNotNull { transform(it).getOrNull() }
}
