package com.fsck.k9.planck.infrastructure

/**
 * A result class that can also be used on Java.
 */
sealed class ResultCompat<T> {
    internal class Success<T>(val content: T) : ResultCompat<T>()
    internal class Failure<T>(val throwable: Throwable) : ResultCompat<T>()

    val isSuccess: Boolean
        get() = this is Success<*>

    val isFailure: Boolean
        get() = this is Failure<*>

    fun getOrDefault(defaultValue: T): T {
        return if (this is Success<T>) content else defaultValue
    }

    fun getOrThrow(): T {
        return when (this) {
            is Success -> content
            is Failure -> throw throwable
        }
    }

    fun exceptionOrNull(): Throwable? {
        return when (this) {
            is Success -> null
            is Failure -> throwable
        }
    }

    fun exceptionOrThrow(): Throwable {
        return when (this) {
            is Failure -> throwable
            is Success -> error("Cannot get an exception from a Success!")
        }
    }

    fun <OUT> map(transform: (T) -> OUT): ResultCompat<OUT> {
        return when (this) {
            is Success -> Success(transform(content))
            is Failure -> Failure(throwable)
        }
    }

    suspend fun alsoDoFlatSuspend(action: suspend (T) -> ResultCompat<Unit>): ResultCompat<T> = flatMapSuspend { content ->
        action(content)
            .map { content }
    }

    fun alsoDoCatching(action: (T) -> Unit): ResultCompat<T> = mapCatching { content ->
        action(content)
        content
    }

    fun <OUT> mapCatching(transform: (T) -> OUT): ResultCompat<OUT> {
        return when (this) {
            is Success -> of { transform(content) }
            is Failure -> Failure(throwable)
        }
    }

    fun <OUT> flatMap(transform: (T) -> ResultCompat<OUT>): ResultCompat<OUT> {
        return when (this) {
            is Success -> transform(content)
            is Failure -> Failure(throwable)
        }
    }

    suspend fun <OUT> flatMapSuspend (transform: suspend (T) -> ResultCompat<OUT>): ResultCompat<OUT> {
        return when (this) {
            is Success -> transform(content)
            is Failure -> Failure(throwable)
        }
    }

    fun onSuccess(block: (T) -> Unit): ResultCompat<T> {
        if (this is Success) {
            block(content)
        }
        return this
    }

    suspend fun onSuccessSuspend(block: suspend (T) -> Unit): ResultCompat<T> {
        if (this is Success) {
            block(content)
        }
        return this
    }

    fun onFailure(block: (Throwable) -> Unit): ResultCompat<T> {
        if (this is Failure) {
            block(throwable)
        }
        return this
    }

    companion object {
        @JvmStatic
        fun <T> success(value: T): ResultCompat<T> = Success(value)

        @JvmStatic
        fun <T> failure(throwable: Throwable): ResultCompat<T> = Failure(throwable)

        @JvmStatic
        fun <T> of(block: () -> T): ResultCompat<T> {
            return try {
                Success(block())
            } catch (ex: Throwable) {
                Failure(ex)
            }
        }

        suspend fun <T> ofSuspend(block: suspend () -> T): ResultCompat<T> {
            return try {
                Success(block())
            } catch (ex: Throwable) {
                Failure(ex)
            }
        }
    }
}

fun <T> Result<T>.toResultCompat(): ResultCompat<T> {
    return this.fold(
        onSuccess = { ResultCompat.Success(it) },
        onFailure = { ResultCompat.Failure(it) }
    )
}
