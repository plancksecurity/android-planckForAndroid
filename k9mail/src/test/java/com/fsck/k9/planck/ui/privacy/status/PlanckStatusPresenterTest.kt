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
import security.planck.dialog.BackgroundTaskDialogView

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
    private val handshakeDialogView: BackgroundTaskDialogView = mockk(relaxed = true)
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
    fun `view calls setupRecipients() with 0 recipients when the recipient does not have the right rating`() =
        runTest {
            mappedRecipients.first().rating = Rating.pEpRatingUnreliable
            initializePresenter()


            presenter.loadRecipients()
            advanceUntilIdle()


            coVerify { planckStatusView.setupRecipients(emptyList()) }
        }

    @Test
    fun `startHandshake() calls view to show mistrust confirmation`() {
        stubIncomingMessageRating()
        initializePresenter(incoming = true)


        val identity = Identity().apply { username = USER_NAME }
        presenter.startHandshake(identity, false)


        verify { planckStatusView.showMistrustConfirmationView(USER_NAME) }
    }

    @Test
    fun `startHandshake() calls view to show trust confirmation`() {
        stubIncomingMessageRating()
        initializePresenter(incoming = true)


        val identity = Identity().apply { username = USER_NAME }
        presenter.startHandshake(identity, true)


        verify { planckStatusView.showTrustConfirmationView(USER_NAME) }
    }

    @Test
    fun `performHandshake() for an incoming message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)


            val identity = Identity()
            presenter.startHandshake(identity, false)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { provider.incomingMessageRating(any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `negative handshake calls PlanckProvider_keyMistrusted()`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)


            val identity = Identity()
            presenter.startHandshake(identity, false)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { provider.keyMistrusted(identity) }
        }

    @Test
    fun `positive handshake calls PlanckProvider_trustPersonalKey()`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)


            val identity = Identity()
            presenter.startHandshake(identity, true)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { provider.trustPersonaKey(identity) }
        }

    @Test
    fun `performHandshake() for an incoming message updates message rating in database`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter(incoming = true)
            presenter.loadMessage(mockk())


            val identity = Identity()
            presenter.startHandshake(identity, false)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { localMessage.planckRating = Rating.pEpRatingReliable }
        }

    @Test
    fun `performHandshake() for an outgoing message does not update message rating in database`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)
            presenter.loadMessage(mockk())


            val identity = Identity()
            presenter.startHandshake(identity, false)
            presenter.performHandshake()
            advanceUntilIdle()


            verify(exactly = 0) { localMessage.planckRating = any() }
        }

    @Test
    fun `performHandshake() for an outgoing message updates recipients with PlanckProvider_incomingMessageRating`() =
        runTest {
            stubOutgoingMessageRating()
            initializePresenter(incoming = false)


            val identity = Identity()
            presenter.startHandshake(identity, false)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { provider.getRatingResult(senderAddress, any(), any(), any()) }
            verify { uiCache.recipients }
            verify { identityMapper.mapRecipients(recipients) }
        }

    @Test
    fun `handshakeFinished() finishes the view`() = runTest {
        initializePresenter()


        presenter.initializeTrustConfirmationView(handshakeDialogView)
        presenter.handshakeFinished()


        verify { planckStatusView.finish() }
    }

    @Test
    fun `handshakeCancelled() does not finish the view`() = runTest {
        initializePresenter()


        presenter.initializeTrustConfirmationView(handshakeDialogView)
        presenter.handshakeCancelled()


        verify(exactly = 0) { planckStatusView.finish() }
    }

    @Test
    fun `initialized presenter sets current state in dialog`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity().apply { username = USER_NAME }
        presenter.startHandshake(identity, false)
        presenter.initializeTrustConfirmationView(handshakeDialogView)


        verify { handshakeDialogView.showState(BackgroundTaskDialogView.State.CONFIRMATION) }
    }

    @Test
    fun `performHandshake sets loading state in dialog`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity().apply { username = USER_NAME }
        presenter.startHandshake(identity, false)
        presenter.initializeTrustConfirmationView(handshakeDialogView)
        presenter.performHandshake()
        advanceUntilIdle()


        verify { handshakeDialogView.showState(BackgroundTaskDialogView.State.LOADING) }
    }

    @Test
    fun `on negative handshake click, dialog displays mistrusted recipient feedback`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity().apply { username = USER_NAME }
        presenter.startHandshake(identity, false)
        presenter.initializeTrustConfirmationView(handshakeDialogView)
        presenter.performHandshake()
        advanceUntilIdle()


        verify { handshakeDialogView.showState(BackgroundTaskDialogView.State.SUCCESS) }
    }

    @Test
    fun `on positive handshake click, dialog displays trusted recipient feedback`() =
        runTest {
            stubIncomingMessageRating()
            initializePresenter()


            val identity = Identity().apply { username = USER_NAME }
            presenter.startHandshake(identity, true)
            presenter.initializeTrustConfirmationView(handshakeDialogView)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { handshakeDialogView.showState(BackgroundTaskDialogView.State.SUCCESS) }
        }

    @Test
    fun `if performHandshake() fails, dialog displays error state`() =
        runTest {
            stubIncomingMessageRating()
            every { provider.trustPersonaKey(any()) }.throws(RuntimeException("test"))
            initializePresenter()


            val identity = Identity().apply { username = USER_NAME }
            presenter.startHandshake(identity, true)
            presenter.initializeTrustConfirmationView(handshakeDialogView)
            presenter.performHandshake()
            advanceUntilIdle()


            verify { handshakeDialogView.showState(BackgroundTaskDialogView.State.ERROR) }
        }

    @Test
    fun `performHandshake() uses view to update recipients and set back intent`() = runTest {
        stubIncomingMessageRating()
        initializePresenter()


        val identity = Identity()
        presenter.startHandshake(identity, false)
        presenter.performHandshake()
        advanceUntilIdle()


        verify { planckStatusView.updateIdentities(mappedRecipients) }
        verify { planckStatusView.setupBackIntent(Rating.pEpRatingReliable, false, false) }
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

    private fun stubOutgoingMessageRating(rating: Rating = Rating.pEpRatingReliable) {
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
    fun `undoTrust() uses view to update recipients and set back intent`() = runTest {
        stubIncomingMessageRating()
        stubIncomingMessageRatingAfterResetTrust()
        initializePresenter()
        val identity = Identity()
        presenter.startHandshake(identity, true)
        presenter.performHandshake()
        advanceUntilIdle()


        presenter.undoTrust()
        advanceUntilIdle()


        verify { planckStatusView.updateIdentities(mappedRecipients) }
        verify { planckStatusView.setupBackIntent(Rating.pEpRatingReliable, false, false) }
    }

    private fun stubIncomingMessageRatingAfterResetTrust(
        rating: Rating = Rating.pEpRatingReliable
    ) {
        every { provider.loadMessageRatingAfterResetTrust(any(), true, any()) }
            .returns(ResultCompat.success(rating))
    }

    private fun stubIncomingMessageRating(rating: Rating = Rating.pEpRatingReliable) {
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