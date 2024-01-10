package com.fsck.k9.ui.messageview

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.extensions.isMessageIncomplete
import com.fsck.k9.extensions.isValidForHandshake
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.exceptions.KeyMissingException
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.ui.messageview.MessageViewState.*
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.dialog.BackgroundTaskDialogView
import security.planck.messaging.MessagingRepository

private const val ACCOUNT_UUID = "uuid"
private const val FOLDER_NAME = "folder"
private const val MESSAGE_UID = "uid"
private const val REFERENCE_STRING = "reference"
private const val MAIL1 = "test1@test.ch"
private const val MAIL2 = "test2@test.ch"

@ExperimentalCoroutinesApi
class MessageViewViewModelTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val preferences: Preferences = mockk()
    private val controller: MessagingController = mockk()
    private val planckProvider: PlanckProvider = mockk {
        coEvery { getRating(any<Address>()) }.returns(ResultCompat.success(Rating.pEpRatingReliable))
        every { keyResetIdentity(any(), any()) }.just(runs)
    }
    private val messageViewInfo: MessageViewInfo = mockk()
    private val app: Application = mockk()
    private val messageReference: MessageReference =
        spyk(MessageReference(ACCOUNT_UUID, FOLDER_NAME, MESSAGE_UID, null))
    private val account: Account = mockk {
        every { isPlanckPrivacyProtected }.returns(true)
        every { isOpenPgpProviderConfigured }.returns(false)
        every { email }.returns(MAIL2)
    }
    private val folder: LocalFolder = mockk {
        every { name }.returns(FOLDER_NAME)
    }
    private val localMessage: LocalMessage = mockk {
        every { planckRating }.returns(Rating.pEpRatingReliable)
        every { account }.returns(this@MessageViewViewModelTest.account)
        every { from }.returns(arrayOf(Address(MAIL1)))
        every { planckRating = any() }.just(runs)
        every { isSet(Flag.X_DOWNLOADED_FULL) }.returns(true)
        every { isSet(Flag.FLAGGED) }.returns(false)
        every { isSet(Flag.SEEN) }.returns(false)
        every { folder }.returns(this@MessageViewViewModelTest.folder)
        every { uid }.returns(MESSAGE_UID)
    }
    private val storedMessage = localMessage
    private val decryptedMessage: MimeMessage = mockk {
        every { uid = any() }.just(runs)
        every { setHeader(any(), any()) }.just(runs)
    }
    private val senderAddress = Address(MAIL1)
    private val senderIdentity: Identity = mockk()
    private val updateFlow: MutableStateFlow<MessageViewState> = MutableStateFlow(Idle)
    private val repository: MessagingRepository = mockk {
        coEvery { loadMessage(any(), any(), any()) }.answers {
            updateFlow.value = DecryptedMessageLoaded(localMessage)
        }
    }
    private val viewModel = MessageViewViewModel(
        preferences,
        controller,
        planckProvider,
        app,
        repository,
        updateFlow,
        coroutinesTestRule.testDispatcherProvider
    )
    private val receivedMessageStates = mutableListOf<MessageViewState>()
    private val receivedResetStates = mutableListOf<BackgroundTaskDialogView.State>()
    private val allowHandshakeSenderEvents = mutableListOf<Boolean>()
    private val flaggedToggledEvents = mutableListOf<Boolean>()
    private val readToggledEvents = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        every { preferences.getAccount(any()) }.returns(account)
        mockkStatic(MessageReference::class)
        every { controller.loadMessage(any(), any(), any()) }.returns(localMessage)
        every { controller.setFlag(any(), any(), any<List<Message>>(), any(), any()) }.just(runs)
        every { MessageReference.parse(any()) }.returns(messageReference)
        mockkStatic("com.fsck.k9.extensions.LocalMessageKt")
        every { localMessage.hasToBeDecrypted() }.returns(false)
        every { localMessage.isValidForHandshake() }.returns(true)
        every { localMessage.isMessageIncomplete() }.returns(false)
        mockkStatic(PlanckUtils::class)
        every { PlanckUtils.extractRating(any()) }.returns(Rating.pEpRatingReliable)
        every { PlanckUtils.createIdentity(any(), any()) }.returns(senderIdentity)
        mockkStatic(MessageViewInfo::class)
        coEvery { planckProvider.decryptMessage(any(), any<Account>()) }.answers {
            every { localMessage.hasToBeDecrypted() }.returns(false)
            Result.success(
                PlanckProvider.DecryptResult(decryptedMessage, Rating.pEpRatingReliable, -1, false)
            )
        }
        val runnableSlot = slot<Runnable>()
        every { folder.storeSmallMessage(any(), capture(runnableSlot)) }.answers {
            runnableSlot.captured.run()
            storedMessage
        }
        stubCanResetSenderKeysSuccess()
        receivedMessageStates.clear()
        receivedResetStates.clear()
        allowHandshakeSenderEvents.clear()
        flaggedToggledEvents.clear()
        readToggledEvents.clear()
        observeViewModel()
    }

    @After
    fun tearDown() {
        unmockkStatic(MessageReference::class)
        unmockkStatic("com.fsck.k9.extensions.LocalMessageKt")
        unmockkStatic(PlanckUtils::class)
        unmockkStatic(MessageViewInfo::class)
    }

    @Test
    fun `initial message state is Idle`() {
        assertMessageStates(Idle)
    }

    @Test
    fun `initialize() parses message reference and gets account`() {
        viewModel.initialize(REFERENCE_STRING)


        verify { MessageReference.parse(REFERENCE_STRING) }
        verify { preferences.getAccount(ACCOUNT_UUID) }
    }

    @Test
    fun `initialize() sets state to loading error if reference is wrong`() {
        every { MessageReference.parse(any()) }.returns(null)


        viewModel.initialize(REFERENCE_STRING)


        verify { preferences.wasNot(called) }
        assertMessageStates(
            Idle,
            ErrorLoadingMessage(IllegalStateException("null reference"), true),
            justClass = true
        )
        val last = receivedMessageStates.last() as ErrorLoadingMessage
        assertTrue(last.close)
        assertTrue(last.throwable is IllegalStateException)
    }

    @Test
    fun `initialize() sets state to ErrorLoadingMessage if account was removed`() {
        every { preferences.getAccount(any()) }.returns(null)


        viewModel.initialize(REFERENCE_STRING)


        assertMessageStates(
            Idle,
            ErrorLoadingMessage(null, true),
            justClass = true
        )
        val last = receivedMessageStates.last() as ErrorLoadingMessage
        assertTrue(last.close)
        assertTrue(last.throwable is IllegalStateException)
    }

    @Test
    fun `loadMessage() uses MessagingRepository to load message`() = runTest {
        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()


        coVerify { repository.loadMessage(account, messageReference, any()) }
    }

    @Test
    fun `loadMessage() allows to handshake sender if message is valid for handshake and sender rating is reliable`() =
        runTest {
            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            verify { localMessage.isValidForHandshake() }
            coVerify { planckProvider.getRating(any<Address>()) }
            verify { PlanckUtils.isRatingReliable(Rating.pEpRatingReliable) }
            assertAllowHandshakeEvents(false, true)
        }

    @Test
    fun `loadMessage() does not allow to handshake sender if message is not valid for handshake`() =
        runTest {
            every { localMessage.isValidForHandshake() }.returns(false)


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertAllowHandshakeEvents(false, false)
        }

    @Test
    fun `loadMessage() does not allow to handshake sender if sender rating is not reliable`() =
        runTest {
            every { PlanckUtils.isRatingReliable(any()) }.returns(false)


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertAllowHandshakeEvents(false, false)
        }

    @Test
    fun `ViewModel does not allow to handshake sender if getting loaded message sender rating fails`() =
        runTest {
            coEvery { planckProvider.getRating(any<Address>()) }.returns(
                ResultCompat.failure(
                    TestException("test")
                )
            )


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertAllowHandshakeEvents(false, false)
        }

    @Test
    fun `canResetSenderKeys() is false if ViewModel was not initialized`() {
        assertFalse(viewModel.canResetSenderKeys())
    }

    @Test
    fun `canResetSenderKeys() is false if message is not loaded`() {
        viewModel.initialize(REFERENCE_STRING)


        assertFalse(viewModel.canResetSenderKeys())
    }

    @Test
    fun `canResetSenderKeys() returns false if message has null from field`() = runTest {
        every { localMessage.from }.returns(null)
        every { localMessage.isValidForHandshake() }.returns(false)


        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()


        assertFalse(viewModel.canResetSenderKeys())
    }

    @Test
    fun `canResetSenderKeys() returns false if message sender is an account in the device`() =
        runTest {
            every { localMessage.from }.returns(arrayOf(Address(MAIL2)))


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertFalse(viewModel.canResetSenderKeys())
        }

    @Test
    fun `canResetSenderKeys() returns false if message has more than one sender`() = runTest {
        every { localMessage.from }.returns(arrayOf(Address(MAIL2), Address(MAIL2)))


        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()


        assertFalse(viewModel.canResetSenderKeys())
    }

    @Test
    fun `canResetSenderKeys() returns false if message does not have exactly one recipient in to field`() =
        runTest {
            every { localMessage.getRecipients(any()) }.returns(arrayOf(Address(MAIL1)))


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertFalse(viewModel.canResetSenderKeys())
        }

    @Test
    fun `canResetSenderKeys() returns true if all conditions are met`() = runTest {
        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()


        assertTrue(viewModel.canResetSenderKeys())
    }

    @Test
    fun `canResetSenderKeys() returns true if all conditions are met also when message rating is mistrusted`() =
        runTest {
            every { localMessage.planckRating }.returns(Rating.pEpRatingMistrust)
            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()


            assertTrue(viewModel.canResetSenderKeys())
        }

    @Test
    fun `initial value for resetPartnerKeyState is Confirmation`() {
        assertResetStates(BackgroundTaskDialogView.State.CONFIRMATION)
    }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Loading`() = runTest {
        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()
        viewModel.resetPlanckData()
        advanceUntilIdle()


        assertResetStates(
            BackgroundTaskDialogView.State.CONFIRMATION,
            BackgroundTaskDialogView.State.LOADING,
            full = false
        )
    }

    @Test
    fun `resetPlanckData uses PlanckProvider to reset sender identity`() = runTest {
        viewModel.initialize(REFERENCE_STRING)
        viewModel.loadMessage()
        advanceUntilIdle()
        viewModel.resetPlanckData()
        advanceUntilIdle()


        verify { PlanckUtils.createIdentity(senderAddress, any()) }
        verify { planckProvider.keyResetIdentity(senderIdentity, null) }
    }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Success if operation was successful`() =
        runTest {
            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.resetPlanckData()
            advanceUntilIdle()


            assertResetStates(
                BackgroundTaskDialogView.State.CONFIRMATION,
                BackgroundTaskDialogView.State.LOADING,
                BackgroundTaskDialogView.State.SUCCESS
            )
        }

    @Test
    fun `resetPlanckData sets resetPartnerKeyState to Error if operation was not successful`() =
        runTest {
            coEvery {
                planckProvider.keyResetIdentity(
                    any(),
                    null
                )
            }.throws(RuntimeException("test"))


            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.resetPlanckData()
            advanceUntilIdle()


            assertResetStates(
                BackgroundTaskDialogView.State.CONFIRMATION,
                BackgroundTaskDialogView.State.LOADING,
                BackgroundTaskDialogView.State.ERROR
            )
        }

    @Test
    fun `toggleFlagged() sets Flagged flag to message using MessagingController if message is not flagged`() =
        runTest {
            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.toggleFlagged()
            advanceUntilIdle()


            verify {
                controller.setFlag(
                    account,
                    FOLDER_NAME,
                    listOf(localMessage),
                    Flag.FLAGGED,
                    true
                )
            }
            assertFlaggedToggledEvents(false, true)
        }

    @Test
    fun `toggleFlagged() unsets Flagged flag to message using MessagingController if message is Flagged`() =
        runTest {
            every { localMessage.isSet(Flag.FLAGGED) }.returns(true)

            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.toggleFlagged()
            advanceUntilIdle()


            verify {
                controller.setFlag(
                    account,
                    FOLDER_NAME,
                    listOf(localMessage),
                    Flag.FLAGGED,
                    false
                )
            }
            assertFlaggedToggledEvents(false, true)
        }

    @Test
    fun `toggleRead() sets Seen flag to message using MessagingController if message is not Seen`() =
        runTest {
            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.toggleRead()
            advanceUntilIdle()


            verify {
                controller.setFlag(
                    account,
                    FOLDER_NAME,
                    listOf(localMessage),
                    Flag.SEEN,
                    true
                )
            }
            assertReadToggledEvents(false, true)
        }

    @Test
    fun `toggleRead() unsets Seen flag to message using MessagingController if message is Seen`() =
        runTest {
            every { localMessage.isSet(Flag.SEEN) }.returns(true)

            viewModel.initialize(REFERENCE_STRING)
            viewModel.loadMessage()
            advanceUntilIdle()
            viewModel.toggleRead()
            advanceUntilIdle()


            verify {
                controller.setFlag(
                    account,
                    FOLDER_NAME,
                    listOf(localMessage),
                    Flag.SEEN,
                    false
                )
            }
            assertReadToggledEvents(false, true)
        }

    @Test
    fun `viewModel does not set any flags if message is not loaded`() = runTest {
        viewModel.initialize(REFERENCE_STRING)
        viewModel.toggleFlagged()
        advanceUntilIdle()


        verify(exactly = 0) {
            controller.setFlag(
                account,
                FOLDER_NAME,
                listOf(localMessage),
                Flag.FLAGGED,
                true
            )
        }
        assertFlaggedToggledEvents(false)
    }

    private fun assertMessageStates(vararg states: MessageViewState, justClass: Boolean = false) {
        states.forEachIndexed { index, messageViewState ->
            if (justClass) {
                assertEquals(messageViewState::class, receivedMessageStates[index]::class)
            } else {
                assertEquals(messageViewState, receivedMessageStates[index])
            }
        }
    }

    private fun assertResetStates(
        vararg states: BackgroundTaskDialogView.State,
        full: Boolean = true
    ) {
        if (full) assertEquals(states.size, receivedResetStates.size)
        states.forEachIndexed { index, resetState ->
            assertEquals(resetState, receivedResetStates[index])
        }
    }

    private fun assertAllowHandshakeEvents(vararg events: Boolean) {
        assertEquals(events.toList(), allowHandshakeSenderEvents)
    }

    private fun assertFlaggedToggledEvents(vararg events: Boolean) {
        assertEquals(events.toList(), flaggedToggledEvents)
    }

    private fun assertReadToggledEvents(vararg events: Boolean) {
        assertEquals(events.toList(), readToggledEvents)
    }

    private fun observeViewModel() {
        viewModel.messageViewState.observeForever { value ->
            receivedMessageStates.add(value)
            println("received: $value")
        }
        viewModel.resetPartnerKeyState.observeForever { value ->
            println("received: $value")
            receivedResetStates.add(value)
        }
        viewModel.allowHandshakeSender.observeForever { event ->
            event.getContentIfNotHandled()?.let {
                allowHandshakeSenderEvents.add(it)
            }
        }
        viewModel.flaggedToggled.observeForever { event ->
            event.getContentIfNotHandled()?.let {
                flaggedToggledEvents.add(it)
            }
        }
        viewModel.readToggled.observeForever { event ->
            event.getContentIfNotHandled()?.let {
                readToggledEvents.add(it)
            }
        }
    }

    private fun stubCanResetSenderKeysSuccess() {
        every { preferences.availableAccounts }.answers { listOf(account) }
        every { localMessage.getRecipients(Message.RecipientType.TO) }.returns(arrayOf(senderAddress))
        every { localMessage.getRecipients(Message.RecipientType.CC) }.returns(null)
        every { localMessage.getRecipients(Message.RecipientType.BCC) }.returns(null)
    }

    data class TestException(override val message: String = "") : Throwable(message)
}