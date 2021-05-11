package com.fsck.k9.ui.messageview.duplicateattachment

import android.os.Bundle
import javax.inject.Inject

private const val STATE_CURRENT_SCREEN_MODE = "currentScreenMode"

class DuplicateAttachmentConfirmationPresenter @Inject constructor() {
    private lateinit var view: DuplicateAttachmentConfirmationView
    lateinit var initialScreenMode: ScreenMode
    lateinit var currentScreenMode: ScreenMode
    lateinit var defaultName: String

    fun initialize(
        view: DuplicateAttachmentConfirmationView,
        initialScreenMode: ScreenMode,
        defaultName: String
    ) {
        this.view = view
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
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {

            }
            ScreenMode.RENAME -> {

            }
        }
    }

    fun positiveButtonClicked() {
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {

            }
            ScreenMode.RENAME -> {

            }
        }
    }

    fun negativeButtonClicked() {
        when(currentScreenMode) {
            ScreenMode.OVERWRITE -> {

            }
            ScreenMode.RENAME -> {

            }
        }
    }
}