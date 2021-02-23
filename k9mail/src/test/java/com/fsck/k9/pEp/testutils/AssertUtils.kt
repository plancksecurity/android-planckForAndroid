package com.fsck.k9.pEp.testutils

import foundation.pEp.jniadapter.Identity
import org.mockito.ArgumentMatcher
import org.mockito.internal.progress.ThreadSafeMockingProgress

object AssertUtils {
    fun identityThat(condition: (Identity) -> Boolean): Identity {
        reportMatcher(ArgumentMatcher<Identity> { argument -> condition(argument) })
        return Identity()
    }

    private fun <T> reportMatcher(matcher: ArgumentMatcher<T>) {
        ThreadSafeMockingProgress.mockingProgress().argumentMatcherStorage.reportMatcher(matcher)
    }
}