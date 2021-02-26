package com.fsck.k9.pEp.ui.feedback

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.pEp.ui.SimpleMessageLoaderHelper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import security.pEp.ui.feedback.FeedbackMessageInfo
import security.pEp.ui.feedback.SendErrorFeedbackActivityData
import security.pEp.ui.feedback.SendErrorFeedbackPresenter
import security.pEp.ui.feedback.SendErrorFeedbackView

class SendErrorFeedbackPresenterTest {
    private lateinit var presenter: SendErrorFeedbackPresenter
    private val view: SendErrorFeedbackView = mock()
    private val controller: MessagingController = mock()
    private val loaderHelper: SimpleMessageLoaderHelper = mock()
    private val preferences: Preferences = mock()
    private val account: Account = mock()

    private val testMessageReference =
        MessageReference(ACCOUNT_UUID, FOLDER, UID, null)

    private val testData: SendErrorFeedbackActivityData
        get() {
            val recipients = FeedbackMessageInfo.FeedbackMessageRecipients("pep@pep.test", "", "")
            val messageInfo = FeedbackMessageInfo("subject", "from", recipients, "Date")
            return SendErrorFeedbackActivityData(ACCOUNT_UUID, TITLE, TEXT, messageInfo)
        }

    @Before
    fun setUp() {
        doReturn(account).`when`(preferences).getAccount(ACCOUNT_UUID)
        presenter = SendErrorFeedbackPresenter(preferences, loaderHelper)
    }

    @Test
    fun `when intializing presenter with empty accountUuid, view finishes`() {
        presenter.initialize(
            view,
            controller,
            "",
            TITLE,
            TEXT,
            testMessageReference.toIdentityString()
        )

        verify(view).finish()
    }

    @Test
    fun `when intializing presenter with empty title, view finishes`() {
        presenter.initialize(
            view,
            controller,
            ACCOUNT_UUID,
            "",
            TEXT,
            testMessageReference.toIdentityString()
        )

        verify(view).finish()
    }

    @Test
    fun `when intializing presenter with empty text, view finishes`() {
        presenter.initialize(
            view,
            controller,
            ACCOUNT_UUID,
            TITLE,
            "",
            testMessageReference.toIdentityString()
        )

        verify(view).finish()
    }

    @Test
    fun `when intializing presenter with empty message reference, view finishes`() {
        presenter.initialize(view, controller, ACCOUNT_UUID, TITLE, TEXT, "")

        verify(view).finish()
    }

    @Test
    fun `when intializing presenter with correct data, SimpleLoaderHelper starts loading`() {
        initializePresenterProperly()

        verify(loaderHelper).asyncStartOrResumeLoadingMessage(any(), any())
    }

    @Test
    fun `when close button is clicked, view finishes`() {
        initializePresenterProperly()

        presenter.onCloseButtonClicked()

        verify(view).finish()
    }

    @Test
    fun `when pendingMessages is called, controller calls sendPendingMessages and view finishes`() {
        initializePresenterProperly()

        presenter.sendPendingMessages()

        verify(controller).sendPendingMessages(account, null)
        verify(view).finish()
    }

    private fun initializePresenterProperly() {
        presenter.initialize(
            view,
            controller,
            ACCOUNT_UUID,
            TITLE,
            TEXT,
            testMessageReference.toIdentityString()
        )
    }

    companion object {
        private const val ACCOUNT_UUID = "accountUuid"
        private const val TITLE = "title"
        private const val TEXT = "text"
        private const val FOLDER = "folder"
        private const val UID = "uid"
    }
}