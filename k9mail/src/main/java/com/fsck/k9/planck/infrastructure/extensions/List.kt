package com.fsck.k9.planck.infrastructure.extensions

fun <T> List<T>.modifyItems(findItem: (T) -> Boolean, newValue: (T) -> T): List<T> {
    return map { item ->
        if (findItem(item)) newValue(item) else item
    }
}