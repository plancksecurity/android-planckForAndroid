package com.fsck.k9.planck.manualsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.K9
import com.fsck.k9.planck.DefaultDispatcherProvider
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import foundation.pEp.jniadapter.Identity
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.sync.SyncRepository
import javax.inject.Inject

private const val DEFAULT_TRUSTWORDS_LANGUAGE = "en"

@HiltViewModel
class PlanckSyncWizardViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val planckProvider: PlanckProvider,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
) : ViewModel() {
    private val syncState = MutableLiveData<SyncScreenState>(SyncState.Idle)
    fun getSyncState(): LiveData<SyncScreenState> = syncState

    var formingGroup = false
    var shortTrustWords = true
    private var trustwordsLanguage = getInitialTrustwordsLanguage()

    private lateinit var myself: Identity
    private lateinit var partner: Identity
    private var wasDone = false

    init {
        viewModelScope.launch {
            syncRepository.userConnected()
            observeSyncRepository()
        }
    }

    private suspend fun observeSyncRepository() {
        syncRepository.syncStateFlow.onEach { appState ->
            if (appState is SyncScreenState) {
                when (appState) {
                    is SyncState.HandshakeReadyAwaitingUser ->
                        populateDataFromHandshakeReadyState(appState)

                    SyncState.Done ->
                        wasDone = true

                    else -> {}
                }
                syncState.value = appState
            }
        }.collect()
    }

    private fun populateDataFromHandshakeReadyState(appState: SyncState.HandshakeReadyAwaitingUser) {
        myself = appState.myself
        partner = appState.partner
        formingGroup = appState.formingGroup
    }

    fun next() {
        when (syncState.value) {
            is SyncState.HandshakeReadyAwaitingUser -> {
                viewModelScope.launch {
                    syncRepository.lockHandshake()
                    getOrRefreshTrustWords()
                }
            }

            is SyncState.UserHandshaking -> {
                setAwaitingHandshakeCompletion()
            }

            else -> error("No next for ${syncState.value}")
        }
    }

    private fun setAwaitingHandshakeCompletion() {
        syncState.value = SyncState.AwaitingHandshakeCompletion(
            PlanckUtils.formatFpr(myself.fpr),
            PlanckUtils.formatFpr(partner.fpr),
        )
        syncRepository.setCurrentState(SyncState.PerformingHandshake)
    }

    private fun finish() {
        syncRepository.userDisconnected()
    }

    fun rejectHandshake() {
        viewModelScope.launch {
            planckProvider.rejectSync()
            syncRepository.cancelSync()
        }
    }

    fun acceptHandshake() {
        viewModelScope.launch {
            planckProvider.acceptSync()
            next()
        }
    }

    fun cancelHandshake() {
        viewModelScope.launch {
            planckProvider.cancelSync()
            syncRepository.cancelSync()
        }
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
        viewModelScope.launch {
            trustwordsLanguage = language
            getOrRefreshTrustWords()
        }
    }

    private suspend fun getOrRefreshTrustWords() {
        withContext(dispatcherProvider.planckDispatcher()) {
            planckProvider.trustwords(
                myself,
                partner,
                trustwordsLanguage,
                shortTrustWords,
            )
        }.onSuccess { trustwords ->
            if (trustwords.isNullOrBlank()) {
                syncState.value = SyncState.Error(IllegalStateException("could not get trustwords"))
            } else {
                syncState.value = SyncState.UserHandshaking(
                    PlanckUtils.formatFpr(myself.fpr),
                    PlanckUtils.formatFpr(partner.fpr),
                    trustwords
                )
            }
        }.onFailure {
            syncState.value = SyncState.Error(it)
        }
    }

    override fun onCleared() {
        super.onCleared()
        finish()
    }

    fun switchTrustwordsLength() {
        viewModelScope.launch {
            shortTrustWords = !shortTrustWords
            getOrRefreshTrustWords()
        }
    }

    fun isHandshaking(): Boolean = syncState.value is SyncState.UserHandshaking

    fun cancelIfNotDone() {
        if (!wasDone) cancelHandshake()
    }

}