package com.fsck.k9.pEp.ui.keys

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.pEp.DispatcherProvider
import com.fsck.k9.pEp.PEpProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class PepExtraKeysPresenter @Inject constructor(
    @Named("MainUI") private val pEp: PEpProvider,
    private val preferences: Preferences,
    private val dispatcherProvider: DispatcherProvider,
) {

    private lateinit var keys: MutableSet<String>
    private lateinit var view: PepExtraKeysView

    fun initialize(view: PepExtraKeysView) {
        this.view = view
        keys = HashSet(K9.getMasterKeys())
        setupMasterKeys(keys)
    }

    fun addMasterKey(key: String) {
        keys.add(key)
        K9.setMasterKeys(keys)
    }

    fun removeMasterKey(key: String) {
        keys.remove(key)
        K9.setMasterKeys(keys)
    }

    fun onPause() {
        CoroutineScope(dispatcherProvider.main() + SupervisorJob()).launch {
            save()
        }
    }

    private suspend fun save() = withContext(dispatcherProvider.io()) {
        val editor = preferences.storage.edit()
        K9.save(editor)
        editor.commit()
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

    private suspend fun getMasterKeyInfo(): List<KeyListItem>? =
        withContext(dispatcherProvider.io()) {
            pEp.masterKeysInfo
        }
}