package com.fsck.k9.ui.settings.account.remove

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
    fun getScope(): CoroutineScope
}