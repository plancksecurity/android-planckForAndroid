package com.fsck.k9.ui.messageview

import android.app.Application
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.RobolectricTest
import com.fsck.k9.extensions.hasToBeDecrypted
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
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
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.dialog.BackgroundTaskDialogView

private const val MAIL1 = "test1@test.ch"
private const val MAIL2 = "test2@test.ch"

@ExperimentalCoroutinesApi
class SenderPlanckHelperTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val context: Application = mockk()
    private val planckProvider: PlanckProvider = mockk()
    private val preferences: Preferences = mockk()
    private val message: LocalMessage = mockk()
    private val view: SenderPlanckHelperView = mockk(relaxed = true)
    private val resetPartnerKeyView: BackgroundTaskDialogView = mockk(relaxed = true)
    private val senderAddress = Address(MAIL1)

    private val helper = SenderPlanckHelper(
        context, planckProvider, preferences, coroutinesTestRule.testDispatcherProvider
    )

    @Before
    fun setUp() {
        mockkStatic("com.fsck.k9.extensions.LocalMessageKt")
        mockkStatic(PlanckUtils::class)
        stubCanResetSenderKeysSuccess()
        every { message.planckRating }.returns(Rating.pEpRatingReliable)
        coEvery { planckProvider.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))
    }

    @After
    fun tearDown() {
        unmockkStatic("com.fsck.k9.extensions.LocalMessageKt")
        unmockkStatic(PlanckUtils::class)
    }

    @Test
    fun `canResetSenderKeys() with a null message is false`() {
        assertFalse(helper.canResetSenderKeys(null))
    }

    @Test
    fun `canResetSenderKeys() returns false if SenderPlanckHelper was not initialized`() {
        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns false if message has to be decrypted`() {
        every { message.hasToBeDecrypted() }.returns(true)
        helper.initialize(message, view)


        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns false if message has null from field`() {
        every { message.from }.returns(null)
        helper.initialize(message, view)


        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns false if message sender is an account in the device`() {
        every { message.from }.returns(arrayOf(Address(MAIL2)))
        helper.initialize(message, view)


        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns false if message has more than one sender`() {
        every { message.from }.returns(arrayOf(Address(MAIL2), Address(MAIL2)))
        helper.initialize(message, view)


        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns false if message does not have exactly one recipient in to field`() {
        every { message.getRecipients(any()) }.returns(arrayOf(Address(MAIL1)))
        helper.initialize(message, view)


        assertFalse(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns true if all conditions are met`() {
        helper.initialize(message, view)


        assertTrue(helper.canResetSenderKeys(message))
    }

    @Test
    fun `canResetSenderKeys() returns true if all conditions are met also when message rating is mistrusted`() {
        every { message.planckRating }.returns(Rating.pEpRatingMistrust)
        helper.initialize(message, view)


        assertTrue(helper.canResetSenderKeys(message))
    }

    @Test
    fun `checkCanHandshakeSender() does not allow handshake if message has null from field`() =
        runTest {
            coEvery { message.from }.returns(null)
            helper.initialize(message, view)


            helper.checkCanHandshakeSender()
            advanceUntilIdle()


            coVerify { view.wasNot(called) }
        }

    @Test
    fun `checkCanHandshakeSender() does not allow handshake if message has more than one sender`() =
        runTest {
            coEvery { message.from }.returns(arrayOf(Address(MAIL2), Address(MAIL2)))
            helper.initialize(message, view)


            helper.checkCanHandshakeSender()
            advanceUntilIdle()


            coVerify { view.wasNot(called) }
        }

    @Test
    fun `checkCanHandshakeSender() does not allow handshake if message rating is not within handshake values`() =
        runTest {
            coEvery { message.planckRating }.returns(Rating.pEpRatingUndefined)
            helper.initialize(message, view)


            helper.checkCanHandshakeSender()
            advanceUntilIdle()


            coVerify { view.wasNot(called) }
        }

    @Test
    fun `checkCanHandshakeSender() does not allow handshake if message does not have exactly one recipient in to field`() =
        runTest {
            coEvery { message.getRecipients(any()) }.returns(arrayOf(Address(MAIL1)))
            helper.initialize(message, view)


            helper.checkCanHandshakeSender()
            advanceUntilIdle()


            coVerify { view.wasNot(called) }
        }

    @Test
    fun `checkCanHandshakeSender() does not allow handshake if message sender rating is not within handshake values`() =
        runTest {
            helper.initialize(message, view)
            coEvery { planckProvider.getRating(any<Address>()) }
                .returns(ResultCompat.success(Rating.pEpRatingUndefined))


            helper.checkCanHandshakeSender()
            advanceUntilIdle()


            coVerify { view.wasNot(called) }
        }

    @Test
    fun `checkCanHandshakeSender() allows handshake if all conditions are met`() = runTest {
        helper.initialize(message, view)


        helper.checkCanHandshakeSender()
        advanceUntilIdle()


        coVerify { view.allowKeyResetWithSender() }
    }

    @Test
    fun `resetPartnerKeyView shows Confirmation state on initialization`() {
        helper.initializeResetPartnerKeyView(resetPartnerKeyView)


        verify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.CONFIRMATION) }
    }

    @Test
    fun `resetPlanckData sets Loading state in resetPartnerKeyView`() = runTest {
        helper.initialize(message, view)
        helper.initializeResetPartnerKeyView(resetPartnerKeyView)
        helper.resetPlanckData()
        advanceUntilIdle()


        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.CONFIRMATION) }
        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.LOADING) }
    }

    @Test
    fun `resetPlanckData uses PlanckProvider to reset sender identity`() = runTest {
        val identity = Identity()
        coEvery { PlanckUtils.createIdentity(any(), any()) }.returns(identity)
        coEvery { planckProvider.keyResetIdentity(any(), null) }.just(runs)
        helper.initialize(message, view)
        helper.initializeResetPartnerKeyView(resetPartnerKeyView)
        helper.resetPlanckData()
        advanceUntilIdle()


        coVerify { PlanckUtils.createIdentity(senderAddress, context) }
        coVerify { planckProvider.keyResetIdentity(identity, null) }
    }

    @Test
    fun `if resetPlanckData is successful, resetPartnerKeyView shows Success state`() = runTest {
        coEvery { PlanckUtils.createIdentity(any(), any()) }.returns(mockk())
        coEvery { planckProvider.keyResetIdentity(any(), null) }.just(runs)
        helper.initialize(message, view)
        helper.initializeResetPartnerKeyView(resetPartnerKeyView)
        helper.resetPlanckData()
        advanceUntilIdle()


        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.CONFIRMATION) }
        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.LOADING) }
        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.SUCCESS) }
    }

    @Test
    fun `if resetPlanckData is not successful, resetPartnerKeyView shows Error state`() = runTest {
        coEvery { PlanckUtils.createIdentity(any(), any()) }.returns(mockk())
        coEvery { planckProvider.keyResetIdentity(any(), null) }.throws(RuntimeException("test"))
        helper.initialize(message, view)
        helper.initializeResetPartnerKeyView(resetPartnerKeyView)
        helper.resetPlanckData()
        advanceUntilIdle()


        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.CONFIRMATION) }
        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.LOADING) }
        coVerify { resetPartnerKeyView.showState(BackgroundTaskDialogView.State.ERROR) }
    }

    private fun stubCanResetSenderKeysSuccess() {
        every { message.hasToBeDecrypted() }.returns(false)
        every { message.from }.returns(arrayOf(Address(MAIL1)))
        val account: Account = mockk {
            every { email }.returns(MAIL2)
        }
        every { preferences.availableAccounts }.returns(listOf(account))
        every { message.getRecipients(Message.RecipientType.TO) }.returns(arrayOf(senderAddress))
        every { message.getRecipients(Message.RecipientType.CC) }.returns(null)
        every { message.getRecipients(Message.RecipientType.BCC) }.returns(null)
    }

}