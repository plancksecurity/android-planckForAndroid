package com.fsck.k9.ui.settings.account.remove

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.nhaarman.mockito_kotlin.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemoveAccountPresenterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private lateinit var presenter: RemoveAccountPresenter
    private val view: RemoveAccountView = mock()
    private val controller: MessagingController = mock()
    private val preferences: Preferences = mock()
    private val account: Account = mock()
    private val k9Wrapper: K9Wrapper = mock()
    private val model: RemoveAccountModel = mock()
    private val scopeProvider: CoroutineScopeProvider = mock()
    private val localStore: LocalStore = mock()
    private val lifecycle: Lifecycle = mock()
    private val viewDelegate: RemoveAccountViewDelegate = mock()

    @Before
    fun setUp() {
        stubAccount()
        stubModel()
        stubScopeProvider()
        stubLifecycle()
        presenter = RemoveAccountPresenter(
            k9Wrapper,
            preferences,
            controller,
            lifecycle,
            coroutinesTestRule.testDispatcherProvider
        )
    }

    private fun stubAccount() {
        doReturn(account).`when`(preferences).getAccount(ACCOUNT_UUID)
        doReturn(ACCOUNT_DESCRIPTION).`when`(account).description
        doReturn(localStore).`when`(account).localStore
    }

    private fun stubScopeProvider() {
        doReturn(CoroutineScope(Dispatchers.Main)).`when`(scopeProvider).getScope()
    }

    private fun stubModel() {
        doReturn(account).`when`(model).account
        doReturn(RemoveAccountStep.INITIAL).`when`(model).step
        doReturn(false).`when`(model).isStarted()
    }

    private fun stubLifecycle() {
        doReturn(Lifecycle.State.STARTED).`when`(lifecycle).currentState
    }

    @Test
    fun `when cancel button is clicked, view finishes`() {
        initializePresenterProperly()

        presenter.onCancelButtonClicked()

        verify(viewDelegate).finish()
    }

    @Test
    fun `when intializing presenter with empty accountUuid, view just finishes`() {
        presenter.initialize(
            view,
            model,
            scopeProvider,
            viewDelegate,
            "",
            true
        )

        verify(viewDelegate).finish()
    }

    @Test
    fun `when intializing presenter with an unexistent account, view just finishes`() {
        doReturn(null).`when`(preferences).getAccount(ACCOUNT_UUID)

        initializePresenterProperly()

        verify(viewDelegate).finish()
    }

    @Test
    fun `when initializing presenter and we do not need to initialize model, model is not initialized`() {
        presenter.initialize(
            view,
            model,
            scopeProvider,
            viewDelegate,
            ACCOUNT_UUID,
            false
        )

        verify(model, never()).initialize(any())
    }

    @Test
    fun `when initializing presenter and we need to initialize model, model is initialized`() {
        initializePresenterProperly()

        verify(model).initialize(account)
    }

    @Test
    fun `when lifecycle event Start happens and current lifecycle state is at least STARTED, view shows current step`() {
        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)

            verify(view).showLoading(RemoveAccountStep.INITIAL)
        }
    }

    @Test
    fun `when lifecycle event Start happens and current lifecycle state is NOT at least STARTED, view DOES NOT show current step`() {
        doReturn(Lifecycle.State.CREATED).`when`(lifecycle).currentState

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)

            verify(view, never()).showLoading(RemoveAccountStep.INITIAL)
        }
    }

    @Test
    fun `when lifecycle event Start happens and viewmodel is not started, we check for messages in Outbox folder`() {
        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)

            verify(model).step = RemoveAccountStep.CHECKING_FOR_MESSAGES
            verify(view).showLoading(RemoveAccountStep.CHECKING_FOR_MESSAGES)
            verify(controller).hasMessagesPendingToSend(account)
        }
    }

    @Test
    fun `when lifecycle starts, viewmodel is not started, and have NO messages in Outbox, we show state NORMAL`() {
        doReturn(false).`when`(controller).hasMessagesPendingToSend(account)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()

            verify(model).step = RemoveAccountStep.CHECKING_FOR_MESSAGES
            verify(model).step = RemoveAccountStep.NORMAL
            verify(view).showDialogAtStep(RemoveAccountStep.NORMAL, ACCOUNT_DESCRIPTION)
        }
    }

    @Test
    fun `when lifecycle starts, viewmodel is not started, and have messages in Outbox, we show state MESSAGES_IN_OUTBOX`() {
        doReturn(true).`when`(controller).hasMessagesPendingToSend(account)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()

            verify(model).step = RemoveAccountStep.CHECKING_FOR_MESSAGES
            verify(model).step = RemoveAccountStep.MESSAGES_IN_OUTBOX
            verify(view).showDialogAtStep(RemoveAccountStep.MESSAGES_IN_OUTBOX, ACCOUNT_DESCRIPTION)
        }
    }

    @Test
    fun `when lifecycle event Start happens and viewmodel is ALREADY started, we only show current step`() {
        doReturn(true).`when`(model).isStarted()
        doReturn(RemoveAccountStep.REMOVING_ACCOUNT).`when`(model).step

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.testLifeCycleEvent(Lifecycle.Event.ON_START)

            verify(model, never()).step = any()
            verify(view).showLoading(RemoveAccountStep.REMOVING_ACCOUNT)
            verify(controller, never()).hasMessagesPendingToSend(account)
        }
    }

    @Test
    fun `when presenter renders step INITIAL, view shows loading screen for step INITIAL`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.INITIAL)

        verify(view).showLoading(RemoveAccountStep.INITIAL)
    }

    @Test
    fun `when presenter renders step CHECKING_FOR_MESSAGES, view shows loading screen for step CHECKING_FOR_MESSAGES`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.CHECKING_FOR_MESSAGES)

        verify(view).showLoading(RemoveAccountStep.CHECKING_FOR_MESSAGES)
    }

    @Test
    fun `when presenter renders step SENDING_MESSAGES, view shows loading screen for step SENDING_MESSAGES`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.SENDING_MESSAGES)

        verify(view).showLoading(RemoveAccountStep.SENDING_MESSAGES)
    }

    @Test
    fun `when presenter renders step REMOVING_ACCOUNT, view shows loading screen for step REMOVING_ACCOUNT`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.REMOVING_ACCOUNT)

        verify(view).showLoading(RemoveAccountStep.REMOVING_ACCOUNT)
    }

    @Test
    fun `when presenter renders step FINISHED, viewdelegate just calls accountRemoved`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.FINISHED)

        verify(viewDelegate).accountRemoved()
    }

    @Test
    fun `when presenter renders step NORMAL, view shows screen at step and hides loading screen`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.NORMAL)

        verify(view).showDialogAtStep(RemoveAccountStep.NORMAL, ACCOUNT_DESCRIPTION)
        verify(view).hideLoading()
    }

    @Test
    fun `when presenter renders step MESSAGES_IN_OUTBOX, view shows screen at step and hides loading screen`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.MESSAGES_IN_OUTBOX)

        verify(view).showDialogAtStep(RemoveAccountStep.MESSAGES_IN_OUTBOX, ACCOUNT_DESCRIPTION)
        verify(view).hideLoading()
    }

    @Test
    fun `when presenter renders step SEND_FAILED, view shows screen at step and hides loading screen`() {
        initializePresenterProperly()

        presenter.renderStep(RemoveAccountStep.SEND_FAILED)

        verify(view).showDialogAtStep(RemoveAccountStep.SEND_FAILED, ACCOUNT_DESCRIPTION)
        verify(view).hideLoading()
    }

    @Test
    fun `when user clicks accept button and we are in step SEND_FAILED, then we just remove account`() {
        doReturn(RemoveAccountStep.SEND_FAILED).`when`(model).step

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()

            verifyAccountRemoval()
        }
    }

    @Test
    fun `when user clicks accept button and we are in step NORMAL, then if there are no messages to send we just remove account`() {
        doReturn(RemoveAccountStep.NORMAL).`when`(model).step
        stubControllerHasMessagesToSend(false)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()
            coroutinesTestRule.testDispatcher.advanceUntilIdle()

            verify(controller).hasMessagesPendingToSend(account)
            verifyAccountRemoval()
        }
    }

    @Test
    fun `when user clicks accept button and we are in step MESSAGES_IN_OUTBOX, then if there are no messages to send we just remove account`() {
        doReturn(RemoveAccountStep.MESSAGES_IN_OUTBOX).`when`(model).step
        stubControllerHasMessagesToSend(false)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()
            coroutinesTestRule.testDispatcher.advanceUntilIdle()

            verify(controller).hasMessagesPendingToSend(account)
            verifyAccountRemoval()
        }
    }

    @Test
    fun `when user clicks accept button and we are in step NORMAL, then if there are messages to send we show loading screen and send messages`() {
        doReturn(RemoveAccountStep.NORMAL).`when`(model).step
        stubControllerHasMessagesToSend(true)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()

            verify(model).step = RemoveAccountStep.CHECKING_FOR_MESSAGES
            verify(view).showLoading(RemoveAccountStep.CHECKING_FOR_MESSAGES)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()
            verify(controller, times(2)).hasMessagesPendingToSend(account)

            verify(model).step = RemoveAccountStep.SENDING_MESSAGES
            verify(view).showLoading(RemoveAccountStep.SENDING_MESSAGES)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()


            verify(controller).sendPendingMessagesAndHandleSendingNotificationSynchronous(account)
        }
    }

    @Test
    fun `when user clicks accept button and we are in step MESSAGES_IN_OUTBOX, then if there are messages to send we show loading screen and send messages`() {
        doReturn(RemoveAccountStep.MESSAGES_IN_OUTBOX).`when`(model).step
        stubControllerHasMessagesToSend(true)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()

            verify(model).step = RemoveAccountStep.CHECKING_FOR_MESSAGES
            verify(view).showLoading(RemoveAccountStep.CHECKING_FOR_MESSAGES)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()
            verify(controller, times(2)).hasMessagesPendingToSend(account)

            verify(model).step = RemoveAccountStep.SENDING_MESSAGES
            verify(view).showLoading(RemoveAccountStep.SENDING_MESSAGES)
            coroutinesTestRule.testDispatcher.advanceUntilIdle()


            verify(controller).sendPendingMessagesAndHandleSendingNotificationSynchronous(account)
        }
    }

    @Test
    fun `if sending messages succeeds, then we just remove account`() {
        doReturn(RemoveAccountStep.MESSAGES_IN_OUTBOX).`when`(model).step
        stubControllerSendSuccess(true)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()

            coroutinesTestRule.testDispatcher.advanceUntilIdle()
            verifyAccountRemoval()
        }
    }

    @Test
    fun `if sending messages fails, then we show step SEND_FAILED`() {
        doReturn(RemoveAccountStep.MESSAGES_IN_OUTBOX).`when`(model).step
        stubControllerSendSuccess(false)

        initializePresenterProperly()

        coroutinesTestRule.testDispatcher.runBlockingTest {
            presenter.onAcceptButtonClicked()

            coroutinesTestRule.testDispatcher.advanceUntilIdle()

            verify(model).step = RemoveAccountStep.SEND_FAILED
            verify(view).showDialogAtStep(RemoveAccountStep.SEND_FAILED, ACCOUNT_DESCRIPTION)
            verifyAccountNotRemoved()
        }
    }

    private fun LifecycleObserver.testLifeCycleEvent(lifecycleEvent: Lifecycle.Event) {
        val mockLifeCycleOwner: LifecycleOwner = mock()
        val lifecycleRegistry = LifecycleRegistry(mockLifeCycleOwner)
        lifecycleRegistry.addObserver(this)
        lifecycleRegistry.handleLifecycleEvent(lifecycleEvent)
    }

    private fun initializePresenterProperly() {
        presenter.initialize(
            view,
            model,
            scopeProvider,
            viewDelegate,
            ACCOUNT_UUID,
            true
        )
    }

    private fun stubControllerSendSuccess(success: Boolean) {
        stubControllerHasMessagesToSend(true)
        stubControllerSendPendingMessages(success)
    }

    private fun stubControllerHasMessagesToSend(messagesToSend: Boolean) {
        doReturn(messagesToSend).`when`(controller).hasMessagesPendingToSend(account)
    }

    private fun stubControllerSendPendingMessages(success: Boolean) {
        doAnswer {
            stubControllerHasMessagesToSend(!success)
        }.`when`(controller).sendPendingMessagesAndHandleSendingNotificationSynchronous(account)
    }

    private fun verifyAccountRemoval() {
        verify(model).step = RemoveAccountStep.REMOVING_ACCOUNT
        verify(view).showLoading(RemoveAccountStep.REMOVING_ACCOUNT)
        coroutinesTestRule.testDispatcher.advanceUntilIdle()

        verify(localStore).delete()
        verify(controller).deleteAccount(account)
        verify(preferences).deleteAccount(account)
        verify(k9Wrapper).setServicesEnabled()

        verify(model).step = RemoveAccountStep.FINISHED
    }

    private fun verifyAccountNotRemoved() {
        verify(localStore, never()).delete()
        verify(controller, never()).deleteAccount(account)
        verify(preferences, never()).deleteAccount(account)
        verify(k9Wrapper, never()).setServicesEnabled()
        verify(model, never()).step = RemoveAccountStep.FINISHED
    }

    companion object {
        private const val ACCOUNT_UUID = "accountUuid"
        private const val ACCOUNT_DESCRIPTION = "accountDescription"
    }
}