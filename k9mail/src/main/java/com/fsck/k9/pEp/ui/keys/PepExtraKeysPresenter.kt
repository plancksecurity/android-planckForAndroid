package com.fsck.k9.pEp.ui.keys

import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.ui.blacklist.KeyListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class PepExtraKeysPresenter @Inject constructor(
        private val dispatcherProvider: DispatcherProvider
) {

    private lateinit var view: PepExtraKeysView
    private lateinit var pEp: PEpProvider

    fun initialize(view: PepExtraKeysView, pEp: PEpProvider, keys: Set<String>) {
        this.view = view
        this.pEp = pEp
        setupMasterKeys(keys)
    }

    private fun setupMasterKeys(keys: Set<String>) {
        val uiScope = CoroutineScope(dispatcherProvider.main() + SupervisorJob())
        uiScope.launch {
            val availableKeys = getMasterKeyInfo()
            val masterKeys: MutableList<KeyListItem> = ArrayList(availableKeys?.size ?: 0)
            availableKeys?.forEach { availableKey ->
                availableKey.isSelected = keys.contains(availableKey.fpr)
                masterKeys.add(availableKey)
            }
            view.showKeys(masterKeys)

        }
    }

    private suspend fun getMasterKeyInfo(): List<KeyListItem>? = withContext(dispatcherProvider.io()) {
        pEp.masterKeysInfo
    }
}