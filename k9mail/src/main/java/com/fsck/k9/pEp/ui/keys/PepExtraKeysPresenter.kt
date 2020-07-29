package com.fsck.k9.pEp.ui.keys

import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.infrastructure.Presenter
import com.fsck.k9.pEp.ui.blacklist.KeyListItem
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class PepExtraKeysPresenter @Inject constructor() : Presenter {

    private lateinit var view: PepExtraKeysView
    private lateinit var pEp: PEpProvider

    fun initialize(view: PepExtraKeysView, pEp: PEpProvider, keys: Set<String>) {
        this.view = view
        this.pEp = pEp
        setupMasterKeys(keys)
    }

    private fun setupMasterKeys(keys: Set<String>) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
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

    private suspend fun getMasterKeyInfo(): List<KeyListItem>? = withContext(Dispatchers.IO) {
        pEp.masterKeysInfo
    }

    override fun resume() {}
    override fun pause() {}
    override fun destroy() {}
}