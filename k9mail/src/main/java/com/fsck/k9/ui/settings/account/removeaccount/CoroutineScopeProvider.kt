package com.fsck.k9.ui.settings.account.removeaccount

import kotlinx.coroutines.CoroutineScope

interface CoroutineScopeProvider {
    fun getScope(): CoroutineScope
}