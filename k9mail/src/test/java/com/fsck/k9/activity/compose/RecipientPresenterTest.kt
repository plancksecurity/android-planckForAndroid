package com.fsck.k9.activity.compose

import android.content.Context
import android.os.Looper
import androidx.loader.app.LoaderManager
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.message.ComposePgpInlineDecider
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import io.mockk.MockKAdditionalAnswerScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openintents.openpgp.OpenPgpApiManager
import org.robolectric.Shadows
import java.util.Vector

private const val FROM_MAIL = "from@sample.ch"
private const val TO_MAIL = "to@sample.ch"
private val TO_ADDRESSES = ReplyToAddresses(Address.parse("to@example.org"))
private val ALL_TO_ADDRESSES = listOf(*Address.parse("allTo@example.org"))
private val ALL_CC_ADDRESSES = listOf(*Address.parse("allCc@example.org"))

@ExperimentalCoroutinesApi
class RecipientPresenterTest : RobolectricTest() {
    private val context: Context = mockk<Context>()
    private val loaderManager: LoaderManager = mockk()
    private val openApiManager: OpenPgpApiManager = mockk(relaxed = true)
    private val fromAddress: Address = mockk {
        every { address }.returns(FROM_MAIL)
    }
    private val toAddress: Address = mockk {
        every { address }.returns(TO_MAIL)
    }
    private val view: RecipientMvpView = mockk(relaxed = true) {
        every { fromAddress }.returns(this@RecipientPresenterTest.fromAddress)
        val presenterSlot = slot<RecipientPresenter>()
        every { setPresenter(capture(presenterSlot)) }.answers { setPresenters(presenterSlot.captured) }
    }
    private val account: Account = mockk {
        every { isAlwaysShowCcBcc }.returns(false)
        every { openPgpProvider }.returns(null)
        every { isPlanckPrivacyProtected }.returns(true)
        every { email }.returns(FROM_MAIL)
        every { openPgpKey }.returns(0)
    }
    private val decider: ComposePgpInlineDecider = mockk {
        every { shouldReplyInline(any()) }.returns(false)
    }
    private val planckProvider: PlanckProvider = mockk {
        coEvery { isGroupAddress(any()) }.returns(Result.success(false))
    }
    private val replyToParser: ReplyToParser = mockk()
    private val listener: RecipientPresenter.RecipientsChangedListener = mockk(relaxed = true)
    private val uiCache: PlanckUIArtefactCache = mockk(relaxed = true)
    private val toPresenter: RecipientSelectPresenter = mockk(relaxed = true) {
        every { addresses }.returns(listOf(toAddress))
        every { recipients }.returns(addresses.map { Recipient(it) })
        every { reportedUncompletedRecipients() }.returns(false)
    }
    private val ccPresenter: RecipientSelectPresenter = mockk(relaxed = true) {
        every { addresses }.returns(emptyList())
        every { recipients }.returns(addresses.map { Recipient(it) })
        every { reportedUncompletedRecipients() }.returns(false)
    }
    private val bccPresenter: RecipientSelectPresenter = mockk(relaxed = true) {
        every { addresses }.returns(emptyList())
        every { recipients }.returns(addresses.map { Recipient(it) })
        every { reportedUncompletedRecipients() }.returns(false)
    }
    private val preferences: Preferences = mockk {
        every { containsAccountByEmail(any()) }.returns(false)
    }

    private lateinit var presenter: RecipientPresenter

    @Before
    fun setUp() = runTest {
        every { context.applicationContext }.returns(context)
        stubRating()
        mockkStatic(PlanckUtils::class)
        every { PlanckUtils.createIdentities(any(), any()) }
            .returns(Vector<Identity?>().apply {
                add(
                    mockk()
                )
            })
        Dispatchers.setMain(UnconfinedTestDispatcher())
        createPresenter()
        advanceUntilIdle()
    }

    private fun stubRating(rating: Rating = Rating.pEpRatingReliable) {
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planckProvider.getRating(any(), any(), any(), any(), capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(rating) }
    }

    private fun stubRatingError(throwable: Throwable) {
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planckProvider.getRating(any(), any(), any(), any(), capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(throwable) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(PlanckUtils::class)
    }

    private fun createPresenter() {
        presenter = RecipientPresenter(
            context,
            loaderManager,
            openApiManager,
            view,
            account,
            decider,
            planckProvider,
            replyToParser,
            listener,
            uiCache,
            preferences
        )
    }

    private fun setPresenters(presenter: RecipientPresenter) {
        presenter.setPresenter(toPresenter, Message.RecipientType.TO)
        presenter.setPresenter(ccPresenter, Message.RecipientType.CC)
        presenter.setPresenter(bccPresenter, Message.RecipientType.BCC)
    }

    @Test
    fun `verify presenter initialization`() {
        verify { view.setPresenter(presenter) }
        verify { view.setLoaderManager(loaderManager) }

        verify { view.fromAddress }
        verify { toPresenter.addresses }
        verify { ccPresenter.addresses }
        verify { bccPresenter.addresses }
        verify { view.messageRatingIsBeingLoaded() }
        verify {
            planckProvider.getRating(
                fromAddress,
                listOf(toAddress),
                emptyList(),
                emptyList(),
                any()
            )
        }
    }

    @Test
    fun `presenter gets addresses from RecipientSelectPresenter`() = runTest {
        advanceUntilIdle()
        presenter.toAddresses
        presenter.ccAddresses
        presenter.bccAddresses


        verify(exactly = 3) { toPresenter.addresses }
        verify(exactly = 3) { ccPresenter.addresses }
        verify(exactly = 3) { bccPresenter.addresses }
    }

    @Test
    fun `presenter uses RecipientSelectPresenter to clear unsecure recipients`() {
        presenter.clearUnsecureRecipients()


        verify { toPresenter.clearUnsecureAddresses() }
        verify { ccPresenter.clearUnsecureAddresses() }
        verify { bccPresenter.clearUnsecureAddresses() }
    }

    @Test
    fun `checkRecipientsOkForSending() uses RecipientSelectPresenter to check if recipients are ok for sending`() {
        assertFalse(presenter.checkRecipientsOkForSending())


        verifyOnRecipientSelectPresenters { it.reportedUncompletedRecipients() }
    }

    @Test
    fun `checkRecipientsOkForSending() returns true if any RecipientSelectPresenter reported incomplete recipipients`() {
        every { toPresenter.reportedUncompletedRecipients() }.returns(true)


        assertTrue(presenter.checkRecipientsOkForSending())


        verify { toPresenter.reportedUncompletedRecipients() }
    }

    @Test
    fun `checkRecipientsOkForSending() returns true if recipients are empty`() {
        every { toPresenter.addresses }.returns(emptyList())


        assertTrue(presenter.checkRecipientsOkForSending())


        verifyOnRecipientSelectPresenters { it.reportedUncompletedRecipients() }
        verify { toPresenter.showNoRecipientsError() }
    }

    @Test
    fun `startHandshakeWithSingleRecipient() refreshes recipients`() = runTest {
        val messageReference: MessageReference = mockk()


        presenter.startHandshakeWithSingleRecipient(messageReference)
        advanceUntilIdle()


        verify { toPresenter.addresses }
        verify { ccPresenter.addresses }
        verify { bccPresenter.addresses }
        verify { PlanckUtils.createIdentities(listOf(toAddress), context) }
        verify { PlanckUtils.createIdentities(emptyList(), context) }
        verify { PlanckUtils.createIdentities(emptyList(), context) }
        verify { uiCache.setRecipients(account, any()) }
    }

    @Test
    fun `startHandshakeWithSingleRecipient() starts handshake if all conditions are met`() =
        runTest {
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify { account.isPlanckPrivacyProtected }
            verify { preferences.containsAccountByEmail(TO_MAIL) }
            coVerify { planckProvider.isGroupAddress(toAddress) }
            verify { view.setMessageReference(messageReference) }
            verify { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if there is not just one TO recipient`() =
        runTest {
            every { toPresenter.addresses }.returns(listOf(mockk(), mockk()))
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if there are any cc or bcc recipients`() =
        runTest {
            every { ccPresenter.addresses }.returns(listOf(fromAddress))
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if rating is not reliable`() =
        runTest {
            stubRating(Rating.pEpRatingUnencrypted)
            val messageReference: MessageReference = mockk()


            presenter.updateCryptoStatus()
            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if account is not planck privacy protected`() =
        runTest {
            every { account.isPlanckPrivacyProtected }.returns(false)
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if TO address is myself`() =
        runTest {
            every { preferences.containsAccountByEmail(any()) }.returns(true)
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if TO address is a group address`() =
        runTest {
            coEvery { planckProvider.isGroupAddress(any()) }.returns(Result.success(true))
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
        }

    @Test
    fun `startHandshakeWithSingleRecipient() does not start handshake if PlanckProvider_isGroupAddress() fails`() =
        runTest {
            coEvery { planckProvider.isGroupAddress(any()) }.returns(Result.failure(TestException("test")))
            val messageReference: MessageReference = mockk()


            presenter.startHandshakeWithSingleRecipient(messageReference)
            advanceUntilIdle()


            verify(exactly = 0) { view.setMessageReference(messageReference) }
            verify(exactly = 0) { view.onPlanckPrivacyStatus() }
            verify { view.showError(TestException("test")) }
        }

    @Test
    fun `resetPartnerKeys() uses view to reset partner keys if all conditions are met with secure rating`() =
        runTest {
            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify { account.isPlanckPrivacyProtected }
            verify { preferences.containsAccountByEmail(TO_MAIL) }
            coVerify { planckProvider.isGroupAddress(toAddress) }
            verify { view.resetPartnerKeys(TO_MAIL) }
        }

    @Test
    fun `resetPartnerKeys() uses view to reset partner keys if all conditions are met with rating mistrusted`() =
        runTest {
            stubRating(Rating.pEpRatingMistrust)


            presenter.updateCryptoStatus()
            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify { account.isPlanckPrivacyProtected }
            verify { preferences.containsAccountByEmail(TO_MAIL) }
            coVerify { planckProvider.isGroupAddress(toAddress) }
            verify { view.resetPartnerKeys(TO_MAIL) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if there is not just one TO recipient`() =
        runTest {
            every { toPresenter.addresses }.returns(listOf(mockk(), mockk()))


            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if there are any cc or bcc recipients`() =
        runTest {
            every { ccPresenter.addresses }.returns(listOf(fromAddress))


            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if rating is unsecure but not mistrusted`() =
        runTest {
            stubRating(Rating.pEpRatingUnencrypted)


            presenter.updateCryptoStatus()
            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if account is not planck privacy protected`() =
        runTest {
            every { account.isPlanckPrivacyProtected }.returns(false)


            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if TO address is myself`() = runTest {
        every { preferences.containsAccountByEmail(any()) }.returns(true)


        presenter.resetPartnerKeys()
        advanceUntilIdle()


        verify(exactly = 0) { view.resetPartnerKeys(any()) }
    }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if TO address is a group address`() =
        runTest {
            coEvery { planckProvider.isGroupAddress(any()) }.returns(Result.success(true))


            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
        }

    @Test
    fun `resetPartnerKeys() does not reset partner keys if PlanckProvider_isGroupAddress() fails`() =
        runTest {
            coEvery { planckProvider.isGroupAddress(any()) }.returns(Result.failure(TestException("test")))


            presenter.resetPartnerKeys()
            advanceUntilIdle()


            verify(exactly = 0) { view.resetPartnerKeys(any()) }
            verify { view.showError(TestException("test")) }
        }

    @Test
    @Throws(Exception::class)
    fun testInitFromReplyToMessage() {
        val message = mockk<Message>()
        every { replyToParser.getRecipientsToReplyTo(message, account) }.returns(TO_ADDRESSES)
        val shadowLooper = Shadows.shadowOf(Looper.getMainLooper())


        presenter.initFromReplyToMessage(message, false)
        Thread.sleep(1000)
        shadowLooper.runOneTask()


        verify { toPresenter.addRecipients(any()) }
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun testInitFromReplyToAllMessage() {
        val message = mockk<Message>()
        every { replyToParser.getRecipientsToReplyTo(message, account) }.returns(TO_ADDRESSES)
        val replyToAddresses = ReplyToAddresses(
            ALL_TO_ADDRESSES,
            ALL_CC_ADDRESSES
        )
        every {
            replyToParser.getRecipientsToReplyAllTo(
                message,
                account
            )
        }.returns(replyToAddresses)
        val shadowLooper = Shadows.shadowOf(Looper.getMainLooper())


        presenter.initFromReplyToMessage(message, true)
        Thread.sleep(1000)
        shadowLooper.runToEndOfTasks()


        verify { toPresenter.addRecipients(any()) }
        verify { ccPresenter.addRecipients(any()) }
    }

    @Test
    fun `onClickXLabel calls view_requestFocusOnXField`() {
        presenter.onClickToLabel()
        presenter.onClickCcLabel()
        presenter.onClickBccLabel()


        verify { view.requestFocusOnToField() }
        verify { view.requestFocusOnCcField() }
        verify { view.requestFocusOnBccField() }
    }

    @Test
    fun `onClickRecipientExpander sets fields visibility on view`() {
        presenter.onClickRecipientExpander()


        verify { view.setCcVisibility(true) }
        verify { view.setBccVisibility(true) }
        verify { view.setRecipientExpanderVisibility(true) }
    }

    @Test
    fun `onRecipientsChanged() updates rating`() {
        presenter.onRecipientsChanged()


        verify {
            planckProvider.getRating(
                fromAddress,
                listOf(toAddress),
                emptyList(),
                emptyList(),
                any()
            )
        }
        verify { listener.onRecipientsChanged() }
    }

    @Test
    fun `handleVerifyPartnerIdentityResult() updates rating`() {
        presenter.onRecipientsChanged()


        verify {
            planckProvider.getRating(
                fromAddress,
                listOf(toAddress),
                emptyList(),
                emptyList(),
                any()
            )
        }
    }

    @Test
    fun `showError() uses view to show error`() {
        presenter.showError(TestException("test"))


        verify { view.showError(TestException("test")) }
    }

    @Test
    fun `onNonRecipientFieldFocused() uses view to hide empty CC, BCC fields`() {
        presenter.onNonRecipientFieldFocused()


        verify { account.isAlwaysShowCcBcc }
        verify { ccPresenter.addresses }
        verify { bccPresenter.addresses }
        verify { view.setCcVisibility(false) }
        verify { view.setBccVisibility(false) }
        verify { view.setRecipientExpanderVisibility(true) }
    }

    @Test
    fun `onMenuAddFromContacts() uses view to show contact picker`() {
        presenter.onMenuAddFromContacts()


        verify { view.showContactPicker(any()) }
    }

    @Test
    fun `handlePlanckState() uses view to handle planck state`() {
        presenter.handlePlanckState()


        verify { view.handlePlanckState(false) }
    }

    @Test
    fun `loadPlanckStatus() uses PlanckProvider to get outgoing message rating`() {
        presenter.updateCryptoStatus()


        verify { view.fromAddress }
        verifyOnRecipientSelectPresenters { it.addresses }
        verify { view.messageRatingIsBeingLoaded() }
        verify(exactly = 2) { planckProvider.getRating(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `loadPlanckStatus() updates view with loaded rating`() {
        presenter.updateCryptoStatus()


        verify { view.planckRating = Rating.pEpRatingReliable }
        verify { view.handlePlanckState(false) }
        verify { view.messageRatingLoaded() }
    }

    @Test
    fun `loadPlanckStatus() allows handshake on rating loaded if conditions for handshake are met`() {
        presenter.updateCryptoStatus()


        verify { view.showSingleRecipientHandshakeBanner() }
    }

    @Test
    fun `loadPlanckStatus() allows partner key reset on rating loaded if conditions for key reset are met`() {
        presenter.updateCryptoStatus()


        verify { view.showResetPartnerKeyOption() }
    }

    @Test
    fun `loadPlanckStatus() does not allow handshake on rating loaded if conditions for handshake are not met`() { // same logic as startHandshakeWithSingleRecipient()
        every { account.isPlanckPrivacyProtected }.returns(false)


        presenter.updateCryptoStatus()


        verify { view.hideSingleRecipientHandshakeBanner() }
    }

    @Test
    fun `loadPlanckStatus() does not allow partner key reset on rating loaded if conditions for key reset are not met`() { // same logic as resetPartnerKeys()
        every { account.isPlanckPrivacyProtected }.returns(false)


        presenter.updateCryptoStatus()


        verify { view.hideResetPartnerKeyOption() }
    }

    @Test
    fun `loadPlanckStatus() does not get message rating if there are no recipients`() {
        every { toPresenter.addresses }.returns(emptyList())
        every { toPresenter.recipients }.returns(emptyList())


        presenter.updateCryptoStatus()


        verify { view.planckRating = Rating.pEpRatingUndefined }
        verify { view.handlePlanckState(true) }
        verify { view.hideUnsecureDeliveryWarning() }
        verify { view.hideSingleRecipientHandshakeBanner() }
        verify { view.messageRatingLoaded() }
        verify(exactly = 1) { planckProvider.getRating(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `loadPlanckStatus() uses view to display error status`() {
        stubRatingError(TestException("test"))


        presenter.updateCryptoStatus()


        verify { view.showError(TestException("test")) } // no need to hide handshake banner as it is replaced by error banner
        verify { view.hideResetPartnerKeyOption() }
        verify { view.planckRating = Rating.pEpRatingUndefined }
        verify { view.handlePlanckState(false) }
        verify { view.messageRatingLoaded() }
    }

    private fun verifyOnRecipientSelectPresenters(block: (RecipientSelectPresenter) -> Unit) {
        val presenters = listOf(toPresenter, ccPresenter, bccPresenter)
        presenters.forEach {
            verify { block(it) }
        }
    }

    private fun <B> everyRecipientSelectPresenter(block: (RecipientSelectPresenter) -> MockKAdditionalAnswerScope<RecipientSelectPresenter, B>) {
        val presenters = listOf(toPresenter, ccPresenter, bccPresenter)
        presenters.forEach {
            block(it)
        }
    }

    data class TestException(override val message: String = "") : Throwable(message)
}