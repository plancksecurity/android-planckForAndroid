package com.fsck.k9.ui.messageview.duplicateattachment

import android.os.Bundle
import javax.inject.Inject

private const val STATE_CURRENT_SCREEN_MODE = "currentScreenMode"

class DuplicateAttachmentConfirmationPresenter @Inject constructor() {
    private lateinit var view: DuplicateAttachmentConfirmationView
    private lateinit var listener: DuplicationAttachmentConfirmationListener
    lateinit var initialScreenMode: ScreenMode
    lateinit var currentScreenMode: ScreenMode
    lateinit var defaultName: String

    fun initialize(
        view: DuplicateAttachmentConfirmationView,
        listener: DuplicationAttachmentConfirmationListener,
        initialScreenMode: ScreenMode,
        defaultName: String
    ) {
        this.view = view
        this.listener = listener
        this.initialScreenMode = initialScreenMode
        this.defaultName = defaultName
    }

    fun displayInitialScreen(savedInstanceState: Bundle?) {
        displayScreen(
            ScreenMode.valueOf(
                savedInstanceState?.getString(STATE_CURRENT_SCREEN_MODE) ?: initialScreenMode.name
            )
        )
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString(STATE_CURRENT_SCREEN_MODE, currentScreenMode.name)
    }

    fun displayScreen(screenMode: ScreenMode) {
        currentScreenMode = screenMode
        when(screenMode) {
            ScreenMode.OVERWRITE -> {
                view.displayOverwriteScreen()
            }
            ScreenMode.RENAME -> {
                view.displayRenameScreen(
                    initialScreenMode == ScreenMode.OVERWRITE,
                    defaultName
                )
            }
        }
    }

    fun renameButtonClicked() {
        displayScreen(ScreenMode.RENAME)
    }

    fun positiveButtonClicked(newName: String) {
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

    fun negativeButtonClicked() {
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {
                view.finish()
            }
            ScreenMode.RENAME -> {
                if(initialScreenMode == ScreenMode.OVERWRITE) displayScreen(ScreenMode.OVERWRITE)
                else view.finish()
            }
        }
    }
}