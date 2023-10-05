package security.planck.ui.audit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import security.planck.audit.AuditLogger
import javax.inject.Inject

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogger: AuditLogger,
) : ViewModel() {
    private val tamperAlertLD: MutableLiveData<Event<Boolean>> = MutableLiveData(Event(false))
    val tamperAlert: LiveData<Event<Boolean>> = tamperAlertLD

    init {
        auditLogger.tamperAlertFlow
            .onEach {
                tamperAlertLD.value = Event(it)
            }.launchIn(viewModelScope)
    }

    fun auditTamperingAlertDismissed() {
        auditLogger.resetTamperAlert()
        auditLogger.disablePersistentWarningOnStartup()
    }

    fun auditTamperingCloseApp() {
        auditLogger.resetTamperAlert()
        auditLogger.enablePersistentWarningOnStartup()
    }
}