package com.fsck.k9.planck.ui.activities.provisioning

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.fsck.k9.BuildConfig
import com.fsck.k9.K9
import security.planck.file.PlanckSystemFileLocator
import security.planck.provisioning.ProvisionState
import security.planck.provisioning.ProvisioningFailedException
import security.planck.provisioning.ProvisioningManager
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class ProvisioningPresenter @Inject constructor(
    private val provisioningManager: ProvisioningManager,
    private val fileLocator: PlanckSystemFileLocator,
    private val k9: K9,
) : ProvisioningManager.ProvisioningStateListener {
    private var view: ProvisioningView? = null
    private val firstTime: Boolean
        get() = !fileLocator.keysDbFile.exists()
    private val shouldOfferRestore: Boolean
        get() = BuildConfig.IS_ENTERPRISE && !k9.isRunningOnWorkProfile && firstTime

    fun attach(view: ProvisioningView) {
        this.view = view
        provisioningManager.addListener(this)
    }

    fun detach() {
        this.view = null
        provisioningManager.removeListener(this)
    }

    private fun displayProvisionState(state: ProvisionState) {
        when(state) {
            is ProvisionState.WaitingForProvisioning ->
                view?.waitingForProvisioning()
            is ProvisionState.InProvisioning ->
                view?.provisioningProgress()
            is ProvisionState.WaitingToInitialize -> {
                if (shouldOfferRestore) {
                    view?.offerRestorePlanckData()
                } else {
                    view?.initializing()
                }
            }
            is ProvisionState.Initializing ->
                if (state.provisioned) {
                    view?.initializingAfterSuccessfulProvision()
                } else {
                    view?.initializing()
                }
            is ProvisionState.Initialized ->
                view?.initialized()
            is ProvisionState.Error -> {
                val throwableMessage = state.throwable.message
                val message =
                    if (throwableMessage.isNullOrBlank())
                        state.throwable.stackTraceToString()
                    else throwableMessage
                if (state.throwable is ProvisioningFailedException) {
                    view?.displayProvisioningError(message)
                } else {
                    view?.displayInitializationError(message)
                }
            }
        }
    }

    override fun provisionStateChanged(state: ProvisionState) {
        displayProvisionState(state)
    }

    fun initializeApp() {
        provisioningManager.startProvisioning()
    }

    fun restoreData(documentFile: DocumentFile?) {
        Log.e("EFA-625", "SELECTED DOCUMENT FILE: ${documentFile?.uri}")
        documentFile?.listFiles()?.forEach { file ->
            //Log.e("EFA-625", "COPYING FILE: ${file.uri}")
            copyFileToInternalStorage(file.uri, file.name ?: "unknown")
        }
    }

    private fun copyFileToInternalStorage(fileUri: Uri, fileName: String) {
        var outputFolder: File? = null
        if (fileName.startsWith(PlanckSystemFileLocator.KEYS_DB_FILE)
            || fileName.startsWith(PlanckSystemFileLocator.MANAGEMENT_DB_FILE)) {
            outputFolder = fileLocator.pEpFolder
        } else if (fileName.startsWith(PlanckSystemFileLocator.SYSTEM_DB_FILE)) {
            outputFolder = fileLocator.trustwordsFolder
        } else {
            Log.e("EFA-625", "IGNORED FILE: $fileName")
        }
        outputFolder?.let {
            val inputStream: InputStream? = k9.contentResolver.openInputStream(fileUri)
            outputFolder.mkdirs()
            val outputFile = File(outputFolder, fileName)
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    Log.e("EFA-625", "COPYING FILE $fileName TO ${outputFile.absolutePath}")
                    input.copyTo(output)
                }
            }
        }
    }
}
