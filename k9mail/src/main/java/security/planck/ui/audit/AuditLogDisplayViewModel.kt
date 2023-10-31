package security.planck.ui.audit

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.planck.DispatcherProvider
import com.fsck.k9.planck.infrastructure.ListState
import com.fsck.k9.planck.infrastructure.NEW_LINE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.audit.PlanckAuditLogger
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AuditLogDisplayViewModel(
    application: Application,
    private val dispatcherProvider: DispatcherProvider,
    private val auditFile: File = File(application.filesDir, PlanckAuditLogger.AUDIT_LOGGER_ROUTE),
) : ViewModel() {
    private val auditTextLD = MutableLiveData<ListState<String>>(ListState.Loading)
    val auditText: LiveData<ListState<String>> = auditTextLD

    var longestItem: String = ""
        private set

    @Inject
    constructor(
        application: Application,
        dispatcherProvider: DispatcherProvider
    ) : this(
        application,
        dispatcherProvider,
        File(application.filesDir, PlanckAuditLogger.AUDIT_LOGGER_ROUTE)
    )

    init {
        getAuditLogFileContent()
    }

    private fun getAuditLogFileContent() {
        viewModelScope.launch {
            auditTextLD.value = ListState.Loading
            withContext(dispatcherProvider.io()) {
                kotlin.runCatching {
                    if (auditFile.exists()) auditFile.readText().split(NEW_LINE)
                    else emptyList()
                }.onSuccess { list ->
                    if (list.any { it.isNotBlank() }) {
                        auditTextLD.postValue(ListState.Ready(list))
                        longestItem = list.maxByOrNull { it.length }.orEmpty()
                    } else {
                        auditTextLD.postValue(ListState.EmptyList)
                    }
                }.onFailure {
                    auditTextLD.postValue(ListState.Error(it))
                }
            }
        }
    }
}