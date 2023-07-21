package com.fsck.k9.planck.manualsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val DEFAULT_TRUSTWORDS_LANGUAGE = "en"

@HiltViewModel
class PlanckSyncWizardViewModel @Inject constructor(
    private val k9: K9,
    private val planckProvider: PlanckProvider
) : ViewModel(), SyncStateChangeListener {
    private val syncState = MutableLiveData<SyncScreenState>(SyncScreenState.Idle)
    fun getSyncState(): LiveData<SyncScreenState> = syncState

    var formingGroup = false
    var shortTrustWords = true
    private var trustwordsLanguage = getInitialTrustwordsLanguage()
    private val uiScope = CoroutineScope(Dispatchers.Main)

    private lateinit var myself: Identity
    private lateinit var partner: Identity

    init {
        syncState.value = k9.syncState
        k9.setSyncStateChangeListener(this)
        k9.syncState = SyncScreenState.AwaitingOtherDevice
    }

    fun next() {
        when (syncState.value) {
            SyncScreenState.HandshakeReadyAwaitingUser -> {
                getOrRefreshTrustWords()
            }

            is SyncScreenState.UserHandshaking -> {
                setState(

                    SyncScreenState.AwaitingHandshakeCompletion(
                        PlanckUtils.formatFpr(myself.fpr),
                        PlanckUtils.formatFpr(partner.fpr),
                    )
                )
            }

            else -> error("No next")
        }
    }

    private fun finish() {
        k9.syncState = k9.syncState.finish()
    }

    private fun setState(state: SyncScreenState) {
        syncState.value = state.also { k9.syncState = it }
    }

    fun rejectHandshake() {
        planckProvider.rejectSync()
    }

    fun acceptHandshake() {
        planckProvider.acceptSync()
        next()
    }

    fun cancelHandshake() {
        planckProvider.cancelSync()
        k9.cancelSync()
    }

    fun changeTrustwordsLanguage(languagePosition: Int) {
        val planckLanguages = PlanckUtils.getPlanckLocales()
        changeTrustwords(planckLanguages[languagePosition])
    }

    private fun getInitialTrustwordsLanguage(): String {
        var language = K9.getK9CurrentLanguage()
        if (!PlanckUtils.trustWordsAvailableForLang(language)) {
            language = DEFAULT_TRUSTWORDS_LANGUAGE
        }
        return language
    }

    private fun changeTrustwords(language: String) {
        trustwordsLanguage = language
        getOrRefreshTrustWords()
    }

    private fun getOrRefreshTrustWords() {
        uiScope.launch {
            val trustwords = withContext(PlanckDispatcher) {
                planckProvider.trustwords(
                    myself,
                    partner,
                    trustwordsLanguage,
                    shortTrustWords,
                )
            }
            setState(
                SyncScreenState.UserHandshaking(
                    PlanckUtils.formatFpr(myself.fpr),
                    PlanckUtils.formatFpr(partner.fpr),
                    trustwords
                )
            )
        }
    }

    override fun syncStateChanged(state: SyncScreenState) {
        this.syncState.postValue(state)
    }

    override fun syncStateChanged(
        state: SyncScreenState,
        myself: Identity,
        partner: Identity,
        formingGroup: Boolean
    ) {
        this.formingGroup = formingGroup
        this.myself = myself
        this.partner = partner
        this.syncState.postValue(state)
    }

    override fun onCleared() {
        super.onCleared()
        finish()
        k9.setSyncStateChangeListener(null)
    }

    fun switchTrustwordsLength() {
        shortTrustWords = !shortTrustWords
        getOrRefreshTrustWords()
    }

    fun isHandshaking(): Boolean = syncState.value is SyncScreenState.UserHandshaking

}