package com.fsck.k9.ui.messageview.duplicateattachment

import android.os.Bundle
import com.fsck.k9.pEp.DispatcherProvider
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

private const val STATE_CURRENT_SCREEN_MODE = "currentScreenMode"

class DuplicateAttachmentConfirmationPresenter @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val fileWrapper: FileWrapper
) {
    private lateinit var view: DuplicateAttachmentConfirmationView
    private lateinit var listener: DuplicationAttachmentConfirmationListener
    lateinit var initialScreenMode: ScreenMode
    lateinit var currentScreenMode: ScreenMode
    lateinit var defaultName: String
    lateinit var savePath: String

    fun initialize(
        view: DuplicateAttachmentConfirmationView,
        listener: DuplicationAttachmentConfirmationListener,
        initialScreenMode: ScreenMode,
        defaultName: String,
        savePath: String
    ) {
        this.view = view
        this.listener = listener
        this.initialScreenMode = initialScreenMode
        this.defaultName = defaultName
        this.savePath = savePath
    }

    fun displayInitialStage(savedInstanceState: Bundle?) {
        displayStage(
            ScreenMode.valueOf(
                savedInstanceState?.getString(STATE_CURRENT_SCREEN_MODE) ?: initialScreenMode.name
            )
        )
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN_MODE, currentScreenMode.name)
    }

    fun displayStage(screenMode: ScreenMode) {
        currentScreenMode = screenMode
        when(screenMode) {
            ScreenMode.OVERWRITE -> view.displayOverwriteScreen()
            ScreenMode.RENAME -> displayRenameScreenWithSuggestedFileName()
        }
    }

    private fun displayRenameScreenWithSuggestedFileName() {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            view.displayRenameScreen(
                initialScreenMode == ScreenMode.OVERWRITE,
                findNewNameForDuplicateAttachment()
            )
        }
    }

    fun renameActionClicked() {
        displayStage(ScreenMode.RENAME)
    }

    fun positiveActionClicked(newName: String) {
        view.finish()
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {
                listener.overwriteAttachmentName()
            }
            ScreenMode.RENAME -> {
                listener.attachmentNameConfirmed(newName)
            }
        }
    }

    fun negativeActionClicked() {
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {
                view.finish()
            }
            ScreenMode.RENAME -> {
                if(initialScreenMode == ScreenMode.OVERWRITE) displayStage(ScreenMode.OVERWRITE)
                else view.finish()
            }
        }
    }

    private suspend fun findNewNameForDuplicateAttachment(): String = withContext(dispatcherProvider.io()) {
        var attachmentFile: File
        var oldNameCount = 1
        var displayName: String
        do {
            displayName = defaultName.substringBeforeLast('.') +
                    "(" + oldNameCount + ")." +
                    defaultName.substringAfterLast('.')
            attachmentFile = fileWrapper.createFile(savePath, displayName)
            oldNameCount++
        } while (fileWrapper.fileExists(attachmentFile))
        return@withContext displayName
    }
}

class FileWrapper @Inject constructor() {
    fun createFile(parent: String, name: String ) = File(parent, name)
    fun fileExists(file: File) = file.exists()
}