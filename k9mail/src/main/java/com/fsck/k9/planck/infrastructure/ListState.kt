package com.fsck.k9.planck.infrastructure

sealed interface ListState<out T> {
    object Loading: ListState<Nothing>

    object EmptyList: ListState<Nothing>
    data class Ready<T>(val list: List<T>): ListState<T>
    data class Error(val throwable: Throwable): ListState<Nothing>
}