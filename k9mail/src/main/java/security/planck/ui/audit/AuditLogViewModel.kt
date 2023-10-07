package security.planck.ui.audit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.infrastructure.livedata.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import security.planck.audit.AuditLogger
import javax.inject.Inject

/**
* AuditLogViewModel
*
* Audit Logs ViewModel is responsible to create a LiveData for sharing the update events
* about tampering audit log with UI subscribers inherited K9Activity.
*
* @property auditLogger [AuditLogger] PlanckAuditLogger implementation for AuditLogger interface
* @constructor Creates an AuditLogViewModel
*/
@HiltViewModel
class AuditLogViewModel @Inject constructor(
    private val auditLogger: AuditLogger,
) : ViewModel() {

    /**
     * @property tamperAlertLD
     *
     * MutableLiveData with default log state of undetected tampering
     */
    private val tamperAlertLD: MutableLiveData<Event<Boolean>> = MutableLiveData(Event(false))

    /**
     * @property tamperAlert
     *
     * LiveData available for subscribers responsible for interacting with the end user
     * about the classified potentially audit log tampering situations
     */
    val tamperAlert: LiveData<Event<Boolean>> = tamperAlertLD

    init {
        auditLogger.tamperAlertFlow
            .onEach {
                tamperAlertLD.value = Event(it)
            }.launchIn(viewModelScope)
    }

    /**
     * auditTamperingAlertDismissed
     *
     * processing the end user choice to keep working with the application
     * after audit log tampering was detected.
     */
    fun auditTamperingAlertDismissed() {
        viewModelScope.launch {
            auditLogger.resetTamperAlert()
            auditLogger.disablePersistentWarningOnStartup()
        }
    }

    /**
     * auditTamperingCloseApp
     *
     * processing the end user choice to terminate the application
     * after audit log tampering was detected.
     *
     * @param lambda function as a parameter available for outside implementations execution
     * [closeApp] right after the main processing finished
     */
    fun auditTamperingCloseApp(closeApp: () -> Unit) {
        viewModelScope.launch {
            auditLogger.resetTamperAlert()
            auditLogger.enablePersistentWarningOnStartup()
            closeApp()
        }
    }
}