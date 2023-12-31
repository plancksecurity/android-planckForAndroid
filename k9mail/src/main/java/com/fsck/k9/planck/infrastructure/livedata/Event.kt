package com.fsck.k9.planck.infrastructure.livedata

/**
 * Used as a wrapper for data that is exposed via a LiveData/StateFlow that represents an event.
 */
data class Event<out T>(private val content: T, val isReady: Boolean = true) {

    var hasBeenHandled = false
        private set

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun getContent(): T {
        hasBeenHandled = true
        return content
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}