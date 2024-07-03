package com.fsck.k9.planck.ui.activities.provisioning

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.BuildConfig
import com.fsck.k9.Globals
import com.fsck.k9.planck.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import security.planck.file.PlanckSystemFileLocator
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningManager
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val provisioningManager: ProvisioningManager,
    private val fileLocator: PlanckSystemFileLocator,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val stateLiveData: MutableLiveData<ProvisionState> = MutableLiveData(
        ProvisionState.WaitingToInitialize(false)
    )
    val state: LiveData<ProvisionState> = stateLiveData

    init {
        provisioningManager.state
            .onEach {
                stateLiveData.value =
                    it // we can map state here if we need more control on presentation layer
            }.launchIn(viewModelScope)
    }

    fun initializeApp() {
        provisioningManager.startProvisioning()
    }

    fun restoreData(documentFile: DocumentFile?) {
        viewModelScope.launch {
            withContext(dispatcherProvider.io()) {
                logInDebug("EFA-625", "SELECTED DOCUMENT FILE: ${documentFile?.uri}")
                documentFile?.listFiles()?.forEach { file ->
                    copyFileToInternalStorage(file.uri, file.name ?: "unknown")
                }
            }
        }
    }

    private fun copyFileToInternalStorage(fileUri: Uri, fileName: String) {
        var outputFolder: File? = null
        if (fileName.startsWith(PlanckSystemFileLocator.KEYS_DB_FILE)
            || fileName.startsWith(PlanckSystemFileLocator.MANAGEMENT_DB_FILE)
                    || fileName.startsWith(PlanckSystemFileLocator.LOG_DB_FILE)
        ) {
            outputFolder = fileLocator.pEpFolder
        } else if (fileName.startsWith(PlanckSystemFileLocator.SYSTEM_DB_FILE)) {
            outputFolder = fileLocator.trustwordsFolder
        } else {
            logInDebug("EFA-625", "IGNORED FILE: $fileName")
        }
        outputFolder?.let {
            val inputStream: InputStream? =
                Globals.getContext().contentResolver.openInputStream(fileUri)
            outputFolder.mkdirs()
            val outputFile = File(outputFolder, fileName)
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    logInDebug("EFA-625", "COPYING FILE $fileName TO ${outputFile.absolutePath}")
                    input.copyTo(output)
                }
            }
        }
    }

    private fun logInDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }
}
