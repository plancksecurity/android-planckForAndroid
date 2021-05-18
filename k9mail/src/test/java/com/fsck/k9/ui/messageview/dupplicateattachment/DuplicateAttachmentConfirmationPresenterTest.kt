package com.fsck.k9.ui.messageview.dupplicateattachment

import android.os.Bundle
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.fsck.k9.ui.messageview.duplicateattachment.*
import com.nhaarman.mockito_kotlin.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import java.io.File

@ExperimentalCoroutinesApi
class DuplicateAttachmentConfirmationPresenterTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val view: DuplicateAttachmentConfirmationView = mock()
    private val listener: DuplicationAttachmentConfirmationListener = mock()
    private val savedInstanceState: Bundle = mock()
    private val fileWrapper: FileWrapper = mock()
    private lateinit var presenter: DuplicateAttachmentConfirmationPresenter
    private var wrapperCalls = 0

    @Before
    fun setUp() {
        doReturn(File("hi there")).`when`(fileWrapper).createFile(anyString(), anyString())
        presenter = DuplicateAttachmentConfirmationPresenter(
            coroutinesTestRule.testDispatcherProvider,
            fileWrapper
        )
    }

    @Test
    fun `when presenter displays mode OVERWRITE, view displays overwrite screen`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )

        presenter.displayStage(ScreenMode.OVERWRITE)

        verify(view).displayOverwriteStage()
    }

    @Test
    fun `when presenter displays mode RENAME, view displays rename screen`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.initialize(
                view,
                listener,
                ScreenMode.RENAME,
                DEFAULT_NAME,
                SAVE_PATH
            )

            presenter.displayStage(ScreenMode.RENAME)

            verify(view).displayRenameStage(anyBoolean(), anyString())
        }

    @Test
    fun `when presenter has NO saved state, view displays screen in same mode presenter was initialized with`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )

        presenter.displayInitialStage(null)

        verify(view).displayOverwriteStage()
    }

    @Test
    fun `when presenter has saved state, view displays screen in mode saved in the state`() {
        doReturn(ScreenMode.OVERWRITE.name).`when`(savedInstanceState).getString(STATE_CURRENT_SCREEN_MODE)

        presenter.initialize(
            view,
            listener,
            ScreenMode.RENAME,
            DEFAULT_NAME,
            SAVE_PATH
        )

        presenter.displayInitialStage(savedInstanceState)

        verify(view).displayOverwriteStage()
    }

    @Test
    fun `when rename button is clicked, view displays screen in mode RENAME`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.initialize(
                view,
                listener,
                ScreenMode.RENAME,
                DEFAULT_NAME,
                SAVE_PATH
            )

            presenter.renameActionClicked()

            verify(view).displayRenameStage(eq(false), anyString())
        }

    @Test
    fun `when negative button is clicked and current screen mode is OVERWRITE, view just finishes`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.OVERWRITE)

        presenter.negativeActionClicked()

        verify(view).finish()
    }

    @Test
    fun `when negative button is clicked, current screen mode is RENAME and initial mode was RENAME, view just finishes`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.RENAME,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.RENAME)

        presenter.negativeActionClicked()

        verify(view).finish()
    }

    @Test
    fun `when negative button is clicked, current screen mode is RENAME and initial mode was OVERWRITE, view displays OVERWRITE mode`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.RENAME)

        presenter.negativeActionClicked()

        verify(view).displayOverwriteStage()
    }

    @Test
    fun `when positive button is clicked, view finishes`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.OVERWRITE)

        presenter.positiveActionClicked(DEFAULT_NAME)

        verify(view).finish()
    }

    @Test
    fun `when positive button is clicked and screen mode is OVERWRITE, listener overwrites attachment name`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.OVERWRITE)

        presenter.positiveActionClicked(DEFAULT_NAME)

        verify(listener).overwriteAttachmentName()
    }

    @Test
    fun `when positive button is clicked and screen mode is RENAME, listener confirms new attachment name`() {
        presenter.initialize(
            view,
            listener,
            ScreenMode.OVERWRITE,
            DEFAULT_NAME,
            SAVE_PATH
        )
        presenter.displayStage(ScreenMode.RENAME)

        presenter.positiveActionClicked(DEFAULT_NAME)

        verify(listener).attachmentNameConfirmed(anyString())
    }

    @Test
    fun `when presenter displays mode RENAME, view displays rename screen with modified name`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.initialize(
                view,
                listener,
                ScreenMode.RENAME,
                DEFAULT_NAME,
                SAVE_PATH
            )

            presenter.displayStage(ScreenMode.RENAME)

            verify(view).displayRenameStage(false, MODIFIED_NAME_1)
        }

    @Test
    fun `when presenter displays mode RENAME with name(1)ext, view displays rename screen with name(2)ext`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            stubFileWrapper()

            presenter.initialize(
                view,
                listener,
                ScreenMode.RENAME,
                DEFAULT_NAME,
                SAVE_PATH
            )

            presenter.displayStage(ScreenMode.RENAME)

            verify(view).displayRenameStage(false, MODIFIED_NAME_4)
        }

    private fun stubFileWrapper() {
        doAnswer {
            wrapperCalls++ < NUMBER_OF_EXISTING_FILES
        }.`when`(fileWrapper).fileExists(any())
    }

    companion object {
        private const val STATE_CURRENT_SCREEN_MODE = "currentScreenMode"
        private const val DEFAULT_NAME = "defaultName.extension"
        private const val MODIFIED_NAME_1 = "defaultName(1).extension"
        private const val MODIFIED_NAME_4 = "defaultName(4).extension"
        private const val SAVE_PATH = "save path"
        private const val NUMBER_OF_EXISTING_FILES = 3
    }
}