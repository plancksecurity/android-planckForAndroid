package com.fsck.k9.planck.ui.privacy.status

import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.planck.ui.SimpleMessageLoaderHelper
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val RECIPIENT_ADDRESS = "ignacioxplanck@hello.ch"
private const val USER_NAME = "user"

@ExperimentalCoroutinesApi
class PlanckStatusPresenterTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val recipients: ArrayList<Identity> =
        arrayListOf(Identity().apply { address = RECIPIENT_ADDRESS })
    private val mappedRecipients: ArrayList<PlanckIdentity> =
        arrayListOf(mappedIdentity(recipients.first()))

    private val simpleMessageLoaderHelper: SimpleMessageLoaderHelper = mockk()
    private val planckStatusView: PlanckStatusView = mockk(relaxed = true)
    private val uiCache: PlanckUIArtefactCache = mockk()
    private val provider: PlanckProvider = mockk(relaxed = true)
    private val senderAddress: Address = mockk()
    private val displayHtml: DisplayHtml = mockk()
    private val identityMapper: PlanckIdentityMapper = mockk()
    private val localMessage: LocalMessage = mockk(relaxed = true)
    private val presenter = PlanckStatusPresenter(
        provider,
        uiCache,
        displayHtml,
        simpleMessageLoaderHelper,
        identityMapper,
        coroutinesTestRule.testDispatcherProvider
    )
    private val testMessageReference =
        MessageReference("", "", "", null)

    @Before
    fun setUp() {
        stubMapper()
        stubUiCache()
        stubSimpleMessageLoader()
        stubLocalMessage()
    }

    private fun stubLocalMessage() {
        every { localMessage.planckRating }.returns(Rating.pEpRatingReliable)
    }

    private fun stubSimpleMessageLoader(
        callback: () -> Unit = {
            presenter.callback().onMessageDataLoadFinished(localMessage)
        }
    ) {
        every { simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(any(), any(), any()) }
            .answers { callback() }
    }

    private fun stubMapper() {
        coEvery { identityMapper.mapRecipients(emptyList()) }.returns(emptyList())
        coEvery { identityMapper.mapRecipients(recipients) }.returns(mappedRecipients)
    }

    private fun stubUiCache(output: ArrayList<Identity> = recipients) {
        every { uiCache.recipients }.returns(output)
    }

    @Test
    fun `loadMessage() starts message loader`() {
        initializePresenter()


        presenter.loadMessage(testMessageReference)


        verify {
            simpleMessageLoaderHelper.asyncStartOrResumeLoadingMessage(
                any(),
                any(),
                displayHtml
            )
        }
    }

    @Test
    fun `loadMessage() uses view to display feedback if message cannot be loaded`() {
        stubSimpleMessageLoader {
            presenter.callback().onMessageDataLoadFailed()
        }
        initializePresenter()

        presenter.loadMessage(testMessageReference)


        verify {
            planckStatusView.showDataLoadError()
        }
    }

    @Test
    fun `loadRecipients() maps cache identities and setups view recipients`() = runTest {
        initializePresenter()

        presenter.loadRecipients()
        advanceUntilIdle()


        verify { uiCache.recipients }
        verify { identityMapper.mapRecipients(recipients) }
        verify { planckStatusView.setupRecipients(any()) }
    }

    @Test
    fun `view calls showItsOnlyOwnMsg() when loading an empty recipients list`() = runTest {
        stubUiCache(arrayListOf())
        initializePresenter()


        presenter.loadRecipients()
        advanceUntilIdle()


        coVerify(exactly = 0) { planckStatusView.setupRecipients(any()) }
        coVerify { planckStatusView.showItsOnlyOwnMsg() }
    }

    @Test
    fun `onHandshakeResult() for an incoming message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)


            val identity = Identity()
            presenter.onHandshakeResult(identity, false)
            advanceUntilIdle()


            verify { provider.incomingMessageRating(any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `onHandshakeResult() for an incoming message updates message rating in database`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)
            presenter.loadMessage(mockk())


            val identity = Identity()
            presenter.onHandshakeResult(identity, false)
            advanceUntilIdle()


            verify { localMessage.planckRating = Rating.pEpRatingUndefined }
        }

    @Test
    fun `onHandshakeResult() for an outgoing message does not update message rating in database`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)
            presenter.loadMessage(mockk())


            val identity = Identity()
            presenter.onHandshakeResult(identity, false)
            advanceUntilIdle()


            verify(exactly = 0) { localMessage.planckRating = any() }
        }

    @Test
    fun `onHandshakeResult() for an outgoing message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)


            val identity = Identity()
            presenter.onHandshakeResult(identity, false)
            advanceUntilIdle()


            verify { provider.getRatingResult(senderAddress, any(), any(), any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `on negative handshake result, view displays mistrusted recipient feedback`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity().apply { username = USER_NAME }
        presenter.onHandshakeResult(identity, false)
        advanceUntilIdle()


        verify { planckStatusView.showMistrustFeedback(USER_NAME) }
    }

    @Test
    fun `on positive handshake result, view displays trusted recipient feedback and undo trust`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter()


            val identity = Identity().apply { username = USER_NAME }
            presenter.onHandshakeResult(identity, true)
            advanceUntilIdle()


            verify { planckStatusView.showUndoTrust(USER_NAME) }
        }

    @Test
    fun `onHandshakeResult() uses view to update recipients and set back intent`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity()
        presenter.onHandshakeResult(identity, false)
        advanceUntilIdle()


        verify { planckStatusView.updateIdentities(mappedRecipients) }
        verify { planckStatusView.setupBackIntent(Rating.pEpRatingUndefined, false, false) }
    }

    @Test
    fun `setForceUnencrypted calls view to change back intent`() {
        initializePresenter()


        presenter.setForceUnencrypted(true)

        verify {
            planckStatusView.setupBackIntent(any(), true, false)
        }
    }

    @Test
    fun `setAlwaysSecure calls view to change back intent`() {
        initializePresenter()


        presenter.setAlwaysSecure(true)

        verify {
            planckStatusView.setupBackIntent(any(), false, true)
        }
    }

    @Test
    fun `resetPlanckData() resets identity key`() = runTest {
        initializePresenter()


        val identity = Identity()
        presenter.resetPlanckData(identity)
        advanceUntilIdle()


        verify { provider.keyResetIdentity(identity, null) }
    }

    @Test
    fun `resetPlanckData() for an incoming message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)


            val identity = Identity()
            presenter.resetPlanckData(identity)
            advanceUntilIdle()


            verify { provider.incomingMessageRating(any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `resetPlanckData() for an incoming message updates message rating in database`() = runTest {
        stubIncomingMessageRating()
        initializePresenter(incoming = true)
        presenter.loadMessage(mockk())


        val identity = Identity()
        presenter.resetPlanckData(identity)
        advanceUntilIdle()


        verify { localMessage.planckRating = Rating.pEpRatingUndefined }
    }

    @Test
    fun `resetPlanckData() for an outgoing message does not update message rating in database`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)
            presenter.loadMessage(mockk())


            val identity = Identity()
            presenter.resetPlanckData(identity)
            advanceUntilIdle()


            verify(exactly = 0) { localMessage.planckRating = any() }
        }

    @Test
    fun `resetPlanckData() for an outgoing message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)


            val identity = Identity()
            presenter.resetPlanckData(identity)
            advanceUntilIdle()


            verify { provider.getRatingResult(senderAddress, any(), any(), any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    private fun stubOutgoingMessageRating(rating: Rating = Rating.pEpRatingUndefined) {
        every {
            provider.getRatingResult(
                any(),
                any(),
                any(),
                any()
            )
        }.returns(ResultCompat.success(rating))
    }

    @Test
    fun `resetPlanckData() uses view to update recipients and set back intent`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity()
        presenter.resetPlanckData(identity)
        advanceUntilIdle()


        verify { planckStatusView.updateIdentities(mappedRecipients) }
        verify { planckStatusView.setupBackIntent(Rating.pEpRatingUndefined, false, false) }
    }

    @Test
    fun `undoTrust() on incoming message resets identity trust`() = runTest {
        stubIncomingMessageRating()
        stubIncomingMessageRatingAfterResetTrust()
        initializePresenter()
        val identity = Identity()
        presenter.onHandshakeResult(identity, true)
        advanceUntilIdle()


        presenter.undoTrust()
        advanceUntilIdle()


        verify { provider.loadMessageRatingAfterResetTrust(any(), true, identity) }
    }

    @Test
    fun `undoTrust() on outgoing message resets identity trust`() = runTest {
        stubOutgoingMessageRating()
        stubOutgoingMessageRatingAfterResetTrust()
        initializePresenter(incoming = false)
        val identity = Identity()
        presenter.onHandshakeResult(identity, true)
        advanceUntilIdle()


        presenter.undoTrust()
        advanceUntilIdle()


        verify {
            provider.loadOutgoingMessageRatingAfterResetTrust(
                identity,
                senderAddress,
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `undoTrust() for an incoming message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubIncomingMessageRating()
            stubIncomingMessageRatingAfterResetTrust()
            initializePresenter(incoming = true)
            val identity = Identity()
            presenter.onHandshakeResult(identity, true)
            advanceUntilIdle()


            presenter.undoTrust()
            advanceUntilIdle()


            verify { provider.incomingMessageRating(any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `undoTrust() for an incoming message updates message rating in database`() = runTest {
        stubIncomingMessageRating()
        stubIncomingMessageRatingAfterResetTrust()
        initializePresenter(incoming = true)
        presenter.loadMessage(mockk())
        val identity = Identity()
        presenter.onHandshakeResult(identity, true)
        advanceUntilIdle()


        presenter.undoTrust()
        advanceUntilIdle()


        verify { localMessage.planckRating = Rating.pEpRatingUndefined }
    }

    @Test
    fun `undoTrust() for an outgoing message does not update message rating in database`() =
        runTest {
            stubOutgoingMessageRating()
            stubOutgoingMessageRatingAfterResetTrust()
            initializePresenter(incoming = false)
            presenter.loadMessage(mockk())
            val identity = Identity()
            presenter.onHandshakeResult(identity, true)
            advanceUntilIdle()


            presenter.undoTrust()
            advanceUntilIdle()


            verify(exactly = 0) { localMessage.planckRating = any() }
        }

    @Test
    fun `undoTrust() for an outgoing message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubOutgoingMessageRating()
            stubOutgoingMessageRatingAfterResetTrust()
            initializePresenter(incoming = false)
            val identity = Identity()
            presenter.onHandshakeResult(identity, true)
            advanceUntilIdle()


            presenter.undoTrust()
            advanceUntilIdle()


            verify { provider.getRatingResult(senderAddress, any(), any(), any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    private fun stubOutgoingMessageRatingAfterResetTrust(
        rating: Rating = Rating.pEpRatingUndefined
    ) {
        every {
            provider.loadOutgoingMessageRatingAfterResetTrust(
                any(),
                senderAddress,
                any(),
                any(),
                any()
            )
        }.returns(ResultCompat.success(rating))
    }

    @Test
    fun `undoTrust() uses view to update recipients and set back intent`() = runTest {
        stubIncomingMessageRating()
        stubIncomingMessageRatingAfterResetTrust()
        initializePresenter()
        val identity = Identity()
        presenter.onHandshakeResult(identity, true)
        advanceUntilIdle()


        presenter.undoTrust()
        advanceUntilIdle()


        verify { planckStatusView.updateIdentities(mappedRecipients) }
        verify { planckStatusView.setupBackIntent(Rating.pEpRatingUndefined, false, false) }
    }

    private fun stubIncomingMessageRatingAfterResetTrust(
        rating: Rating = Rating.pEpRatingUndefined
    ) {
        every { provider.loadMessageRatingAfterResetTrust(any(), true, any()) }
            .returns(ResultCompat.success(rating))
    }

    private fun stubIncomingMessageRating(rating: Rating = Rating.pEpRatingUndefined) {
        every { provider.incomingMessageRating(any()) }
            .returns(ResultCompat.success(rating))
    }

    private fun initializePresenter(
        incoming: Boolean = true,
        forceUnencrypted: Boolean = false,
        alwaysSecure: Boolean = false
    ) {
        presenter.initialize(
            planckStatusView,
            incoming,
            senderAddress,
            forceUnencrypted,
            alwaysSecure
        )
    }


    private fun mappedIdentity(recipient: Identity): PlanckIdentity {
        val out = PlanckIdentity()
        out.address = recipient.address
        out.comm_type = recipient.comm_type
        out.flags = recipient.flags
        out.fpr = recipient.fpr
        out.lang = recipient.lang
        out.user_id = recipient.user_id
        out.username = recipient.username
        out.me = recipient.me
        out.rating = Rating.pEpRatingReliable
        return out
    }
}