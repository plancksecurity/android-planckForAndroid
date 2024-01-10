package security.planck.messaging

import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.extensions.isMessageIncomplete
import com.fsck.k9.extensions.isValidForHandshake
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Store
import com.fsck.k9.mail.internet.MimeHeader
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mailstore.LocalFolder
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.mailstore.MessageViewInfoExtractor
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.exceptions.KeyMissingException
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.ui.messageview.MessageViewState
import com.fsck.k9.ui.messageview.MessageViewViewModelTest
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
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
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val ACCOUNT_UUID = "uuid"
private const val FOLDER_NAME = "folder"
private const val MESSAGE_UID = "uid"

@ExperimentalCoroutinesApi
class MessagingRepositoryTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val preferences: Preferences = mockk()
    private val controller: MessagingController = mockk()
    private val planckProvider: PlanckProvider = mockk()
    private val messageViewInfo: MessageViewInfo = mockk()
    private val errorMessageViewInfo: MessageViewInfo = mockk()
    private val infoExtractor: MessageViewInfoExtractor = mockk {
        every { extractMessageForView(any(), null, any()) }.returns(messageViewInfo)
    }
    private val messageReference: MessageReference =
        spyk(MessageReference(ACCOUNT_UUID, FOLDER_NAME, MESSAGE_UID, null))
    private val account: Account = mockk {
        every { isPlanckPrivacyProtected }.returns(true)
        every { isOpenPgpProviderConfigured }.returns(false)
        every { planckSuspiciousFolderName }.returns(Store.PLANCK_SUSPICIOUS_FOLDER)
    }
    private val folder: LocalFolder = mockk {
        every { name }.returns(FOLDER_NAME)
    }
    private val localMessage: LocalMessage = mockk {
        every { planckRating }.returns(Rating.pEpRatingReliable)
        every { account }.returns(this@MessagingRepositoryTest.account)
        every { planckRating = any() }.just(runs)
        every { isSet(Flag.X_DOWNLOADED_FULL) }.returns(true)
        every { isSet(Flag.FLAGGED) }.returns(false)
        every { isSet(Flag.SEEN) }.returns(false)
        every { folder }.returns(this@MessagingRepositoryTest.folder)
        every { uid }.returns(MESSAGE_UID)
        every { makeMessageReference() }.returns(messageReference)
    }
    private val storedMessage = localMessage
    private val decryptedMessage: MimeMessage = mockk {
        every { uid = any() }.just(runs)
        every { setHeader(any(), any()) }.just(runs)
    }
    private val senderIdentity: Identity = mockk()
    private val appIoScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())
    private val repository = MessagingRepository(
        controller,
        planckProvider,
        infoExtractor,
        appIoScope,
        coroutinesTestRule.testDispatcherProvider
    )
    private val receivedMessageStates = mutableListOf<MessageViewState>()
    private val updateFlow: MutableStateFlow<MessageViewState> =
        MutableStateFlow(MessageViewState.Idle)

    @Before
    fun setUp() {
        every { preferences.getAccount(any()) }.returns(account)
        mockkStatic(MessageReference::class)
        every { controller.loadMessage(any(), any(), any()) }.returns(localMessage)
        every { controller.setFlag(any(), any(), any<List<Message>>(), any(), any()) }.just(runs)
        every { controller.moveMessage(any(), any(), any(), any()) }.just(runs)
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
        receivedMessageStates.clear()
        observeUpdateFlow()
    }

    @After
    fun tearDown() {
        unmockkStatic(MessageReference::class)
        unmockkStatic("com.fsck.k9.extensions.LocalMessageKt")
        unmockkStatic(PlanckUtils::class)
        unmockkStatic(MessageViewInfo::class)
    }

    @Test
    fun `loadMessage() uses MessagingController to load message`() = runTest {
        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { controller.loadMessage(account, FOLDER_NAME, MESSAGE_UID) }
    }

    @Test
    fun `loadMessage() sets state to DecryptedMessageLoaded if message does not need to be decrypted`() =
        runTest {
            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            assertMessageStates(
                MessageViewState.Idle,
                MessageViewState.Loading,
                MessageViewState.DecryptedMessageLoaded(localMessage)
            )
        }

    @Test
    fun `loadMessage() sets state to ErrorLoadingMessage if message load fails`() = runTest {
        every { controller.loadMessage(any(), any(), any()) }.throws(TestException("test"))


        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        assertMessageStates(
            MessageViewState.Idle,
            MessageViewState.Loading,
            MessageViewState.ErrorLoadingMessage(TestException("test"))
        )
    }

    @Test
    fun `loadMessage() sets state to ErrorLoadingMessage if loaded message is null`() = runTest {
        every { controller.loadMessage(any(), any(), any()) }.returns(null)


        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        assertMessageStates(
            MessageViewState.Idle,
            MessageViewState.Loading,
            MessageViewState.ErrorLoadingMessage()
        )
    }

    @Test
    fun `loadMessage() sets rating to message from header if message has rating null`() = runTest {
        every { localMessage.planckRating }.returns(null)



        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { localMessage.planckRating }
        verify { PlanckUtils.extractRating(localMessage) }
        verify { localMessage.planckRating = Rating.pEpRatingReliable }
    }

    @Test
    fun `loadMessage() does not set rating to message from header if message rating is not null`() =
        runTest {

            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            verify(exactly = 0) { PlanckUtils.extractRating(localMessage) }
            verify(exactly = 0) { localMessage.planckRating = Rating.pEpRatingReliable }
        }

    @Test
    fun `loadMessage() uses MessageViewInfoExtractor to extract message info if message does not need to be decrypted`() =
        runTest {

            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            verify { infoExtractor.extractMessageForView(localMessage, null, false) }
        }

    @Test
    fun `loadMessage() sets state to MessageDecoded if message is successfully decoded for view`() =
        runTest {

            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            assertMessageStates(
                MessageViewState.Idle,
                MessageViewState.Loading,
                MessageViewState.DecryptedMessageLoaded(localMessage),
                MessageViewState.MessageDecoded(messageViewInfo)
            )
        }

    @Test
    fun `loadMessage() sets state to ErrorDecodingMessage if message decoding fails`() = runTest {
        every {
            infoExtractor.extractMessageForView(
                any(),
                null,
                any()
            )
        }.throws(MessageViewViewModelTest.TestException("test"))
        every { MessageViewInfo.createWithErrorState(any(), any()) }.returns(errorMessageViewInfo)



        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { MessageViewInfo.createWithErrorState(localMessage, false) }
        assertMessageStates(
            MessageViewState.Idle,
            MessageViewState.Loading,
            MessageViewState.DecryptedMessageLoaded(localMessage),
            MessageViewState.ErrorDecodingMessage(
                errorMessageViewInfo,
                MessageViewViewModelTest.TestException("test")
            )
        )
    }

    @Test
    fun `loadMessage() sets state to EncryptedMessageLoaded`() = runTest {
        every { localMessage.hasToBeDecrypted() }.returns(true)



        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        assertMessageStates(
            MessageViewState.Idle,
            MessageViewState.Loading,
            MessageViewState.EncryptedMessageLoaded(localMessage)
        )
    }

    @Test
    fun `loadMessage() decrypts message using PlanckProvider if message is fully downloaded and it needs to be decrypted`() =
        runTest {
            every { localMessage.hasToBeDecrypted() }.returns(true)



            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            coVerify { planckProvider.decryptMessage(localMessage, account) }
        }

    @Test
    fun `loadMessage() sets state to ErrorDecryptingMessageKeyMissing if decryption fails with KeyMissingException`() =
        runTest {
            every { localMessage.hasToBeDecrypted() }.returns(true)
            coEvery { planckProvider.decryptMessage(any(), any<Account>()) }.returns(
                Result.failure(
                    KeyMissingException()
                )
            )



            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            assertMessageStates(
                MessageViewState.Idle,
                MessageViewState.Loading,
                MessageViewState.EncryptedMessageLoaded(localMessage),
                MessageViewState.ErrorDecryptingMessageKeyMissing
            )
        }

    @Test
    fun `loadMessage() sets state to ErrorDecryptingMessage if decryption fails with any other exception`() =
        runTest {
            every { localMessage.hasToBeDecrypted() }.returns(true)
            coEvery { planckProvider.decryptMessage(any(), any<Account>()) }.returns(
                Result.failure(
                    MessageViewViewModelTest.TestException("test")
                )
            )



            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            assertMessageStates(
                MessageViewState.Idle,
                MessageViewState.Loading,
                MessageViewState.EncryptedMessageLoaded(localMessage),
                MessageViewState.ErrorDecryptingMessage(MessageViewViewModelTest.TestException("test"))
            )
        }

    @Test
    fun `loadMessage() stores the decrypted message in folder after decryption`() = runTest {
        every { localMessage.hasToBeDecrypted() }.returns(true)



        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { folder.storeSmallMessage(decryptedMessage, any()) }
    }

    @Test
    fun `loadMessage() sets state to ErrorDecryptingMessage if saving message to database fails`() =
        runTest {
            every { localMessage.hasToBeDecrypted() }.returns(true)
            every {
                folder.storeSmallMessage(
                    any(),
                    any()
                )
            }.throws(MessageViewViewModelTest.TestException("test"))



            repository.loadMessage(account, messageReference, updateFlow)
            advanceUntilIdle()


            verify { folder.storeSmallMessage(decryptedMessage, any()) }
            assertMessageStates(
                MessageViewState.Idle,
                MessageViewState.Loading,
                MessageViewState.EncryptedMessageLoaded(localMessage),
                MessageViewState.ErrorDecryptingMessage(MessageViewViewModelTest.TestException("test")),
            )
        }

    @Test
    fun `loadMessage() sets rating in decrypted message header`() = runTest {
        every { localMessage.hasToBeDecrypted() }.returns(true)


        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { decryptedMessage.setHeader(MimeHeader.HEADER_PEP_RATING, "reliable") }
    }

    @Test
    fun `loadMessage() moves Dangerous message to Suspicious folder`() = runTest {
        every { localMessage.hasToBeDecrypted() }.returns(true)
        coEvery { planckProvider.decryptMessage(any(), any<Account>()) }.answers {
            every { localMessage.hasToBeDecrypted() }.returns(false)
            Result.success(
                PlanckProvider.DecryptResult(
                    decryptedMessage, Rating.pEpRatingMistrust, -1, false
                )
            )
        }


        repository.loadMessage(account, messageReference, updateFlow)
        advanceUntilIdle()


        verify { decryptedMessage.setHeader(MimeHeader.HEADER_PEP_RATING, "mistrust") }
        verify { folder.storeSmallMessage(decryptedMessage, any()) }
        verify {
            controller.moveMessage(
                account,
                FOLDER_NAME,
                messageReference,
                Store.PLANCK_SUSPICIOUS_FOLDER
            )
        }
        assertMessageStates(
            MessageViewState.Idle,
            MessageViewState.Loading,
            MessageViewState.EncryptedMessageLoaded(localMessage),
            MessageViewState.MessageMovedToSuspiciousFolder,
        )
    }

    private fun observeUpdateFlow() {
        CoroutineScope(UnconfinedTestDispatcher()).launch {
            updateFlow.collect {
                receivedMessageStates.add(it)
            }
        }
    }

    private fun assertMessageStates(vararg states: MessageViewState) {
        states.forEachIndexed { index, messageViewState ->
            TestCase.assertEquals(messageViewState, receivedMessageStates[index])
        }
    }

    data class TestException(override val message: String = "") : Throwable(message)
}