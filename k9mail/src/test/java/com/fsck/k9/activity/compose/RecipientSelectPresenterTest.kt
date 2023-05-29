package com.fsck.k9.activity.compose

import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Rating
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val SENDER_ADDRESS = "sender address"

@ExperimentalCoroutinesApi
class RecipientSelectPresenterTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val planck: PlanckProvider = mockk(relaxed = true)
    private val listener: RecipientsReadyListener = mockk(relaxed = true)
    private val ratedListener: RatedRecipientsReadyListener = mockk(relaxed = true)
    private val view: RecipientSelectContract = mockk(relaxed = true)

    private val helper = RecipientSelectPresenter(planck)

    @Before
    fun setup() {
        every { view.hasRecipient(any()) }.returns(true)
        every { view.isAlwaysUnsecure }.returns(false)
        mockkStatic(K9::class)
        every { K9.isPlanckForwardWarningEnabled() }.returns(true)
        helper.initialize(view)
    }

    @Test
    fun `getRecipientRating uses PlanckProvider to get rating`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)


        helper.getRecipientRating(recipient, true, callback)


        verify { planck.getRating(address, any()) }
    }

    @Test
    fun `getRecipientRating calls callback_onLoaded when there is no problem`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingReliable) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel when it gets an unsecure rating`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingCannotDecrypt) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertTrue(helper.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel if recipient is not in view`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.hasRecipient(any()) }.returns(false)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingCannotDecrypt) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertEquals(0, helper.unsecureAddressChannelCount)
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel when getRating is successful and is not planck privacy protected`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingCannotDecrypt) }


        helper.getRecipientRating(recipient, false, callback)


        assertEquals(0, helper.unsecureAddressChannelCount)
    }

    @Test
    fun `getRecipientRating calls callback_onError when there is a problem`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        val testException = RuntimeException("test")
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(testException) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onError(testException) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel when getRating is not successful`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(RuntimeException()) }


        helper.getRecipientRating(recipient, true, callback)


        assertTrue(helper.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel when getRating is not successful and is not planck privacy protected`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(RuntimeException()) }


        helper.getRecipientRating(recipient, false, callback)


        assertEquals(0, helper.unsecureAddressChannelCount)
    }

    @Test
    fun `getRecipientRating calls callback_onLoaded with rating undefined if view is always unsecure`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.isAlwaysUnsecure }.returns(true)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingUnencrypted) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel if view is always unsecure`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.isAlwaysUnsecure }.returns(true)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        helper.getRecipientRating(recipient, true, callback)


        assertTrue(helper.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel when unsecure forward warning is disabled`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(RuntimeException()) }
        every { K9.isPlanckForwardWarningEnabled() }.returns(false)


        helper.getRecipientRating(recipient, true, callback)


        assertEquals(0, helper.unsecureAddressChannelCount)
    }

    @Test
    fun `getRecipientRating calls view_showError when getRating is not successful`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(TestException("test")) }


        helper.getRecipientRating(recipient, true, callback)


        verify { view.showError(TestException("test")) }
    }

    @Test
    fun `rateRecipients gets rating for recipients using PlanckProvider`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        helper.rateAlternateRecipients(listOf(recipient1, recipient2), ratedListener)
        advanceUntilIdle()


        coVerify { planck.getRating(address1) }
        coVerify { planck.getRating(address2) }
    }

    @Test
    fun `rateRecipients calls listener with rated recipients`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.success(Rating.pEpRatingTrustedAndAnonymized))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        helper.rateAlternateRecipients(listOf(recipient1, recipient2), ratedListener)
        advanceUntilIdle()

        val ratedRecipientsSlot = slot<MutableList<RatedRecipient>>()
        coVerify { ratedListener.ratedRecipientsReady(capture(ratedRecipientsSlot)) }

        assertEquals(RatedRecipient::class.java, ratedRecipientsSlot.captured[0]::class.java)
        assertEquals(recipient1, ratedRecipientsSlot.captured[0].baseRecipient)
        assertEquals(
            Rating.pEpRatingTrustedAndAnonymized,
            ratedRecipientsSlot.captured[0].rating
        )
        assertEquals(recipient2, ratedRecipientsSlot.captured[1].baseRecipient)
        assertEquals(
            Rating.pEpRatingUnencrypted,
            ratedRecipientsSlot.captured[1].rating
        )
    }

    @Test
    fun `rateRecipients uses rating undefined if PlanckProvider_getRating fails`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.failure(TestException("test")))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        helper.rateAlternateRecipients(listOf(recipient1, recipient2), ratedListener)
        advanceUntilIdle()

        val ratedRecipientsSlot = slot<MutableList<RatedRecipient>>()
        coVerify { ratedListener.ratedRecipientsReady(capture(ratedRecipientsSlot)) }

        assertEquals(RatedRecipient::class.java, ratedRecipientsSlot.captured[0]::class.java)
        assertEquals(recipient1, ratedRecipientsSlot.captured[0].baseRecipient)
        assertEquals(
            Rating.pEpRatingUndefined,
            ratedRecipientsSlot.captured[0].rating
        )
        assertEquals(recipient2, ratedRecipientsSlot.captured[1].baseRecipient)
        assertEquals(
            Rating.pEpRatingUnencrypted,
            ratedRecipientsSlot.captured[1].rating
        )
    }

    @Test
    fun `rateRecipients calls view_showError if PlanckProvider_getRating fails`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.failure(TestException("test")))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        helper.rateAlternateRecipients(listOf(recipient1, recipient2), ratedListener)
        advanceUntilIdle()


        verify { view.showError(TestException("test")) }
    }

    @Test
    fun `sortRecipientsByRating gets rating for recipients using PlanckProvider`() = runTest {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        helper.sortRecipientsByRating(
            arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
            listener
        )
        advanceUntilIdle()


        coVerify { planck.getRating(undefinedAddress) }
        coVerify { planck.getRating(trustedAddress) }
        coVerify { planck.getRating(secureAddress) }
    }

    @Test
    fun `sortRecipientsByRating calls listener with recipients sorted by rating`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        coEvery { planck.getRating(unencryptedAddress) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        coEvery { planck.getRating(secureAddress) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))
        coEvery { planck.getRating(trustedAddress) }
            .returns(ResultCompat.success(Rating.pEpRatingTrustedAndAnonymized))
        val undefinedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)


        helper.sortRecipientsByRating(
            arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
            listener
        )
        advanceUntilIdle()


        val sortedRecipientsSlot = slot<MutableList<Recipient>>()
        coVerify { listener.recipientsReady(capture(sortedRecipientsSlot)) }


        assertEquals(unencryptedAddress, sortedRecipientsSlot.captured[0].address)
        assertEquals(secureAddress, sortedRecipientsSlot.captured[1].address)
        assertEquals(trustedAddress, sortedRecipientsSlot.captured[2].address)
    }

    @Test
    fun `sortRecipientsByRating uses undefined rating as default if PlanckProvider_getRating fails`() =
        runTest {
            val unencryptedAddress: Address = mockk()
            val secureAddress: Address = mockk()
            val trustedAddress: Address = mockk()
            coEvery { planck.getRating(unencryptedAddress) }
                .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
            coEvery { planck.getRating(secureAddress) }
                .returns(ResultCompat.success(Rating.pEpRatingReliable))
            coEvery { planck.getRating(trustedAddress) }
                .returns(ResultCompat.failure(TestException("test")))
            val undefinedRecipient = Recipient(unencryptedAddress)
            val secureRecipient = Recipient(secureAddress)
            val trustedRecipient = Recipient(trustedAddress)


            helper.sortRecipientsByRating(
                arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
                listener
            )
            advanceUntilIdle()


            val sortedRecipientsSlot = slot<MutableList<Recipient>>()
            coVerify { listener.recipientsReady(capture(sortedRecipientsSlot)) }


            assertEquals(unencryptedAddress, sortedRecipientsSlot.captured[1].address)
            assertEquals(secureAddress, sortedRecipientsSlot.captured[2].address)
            assertEquals(trustedAddress, sortedRecipientsSlot.captured[0].address)
        }

    @Test
    fun `sortRecipientsByRating calls view_showError if PlanckProvider_getRating fails`() = runTest {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        helper.sortRecipientsByRating(
            arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
            listener
        )
        advanceUntilIdle()


        coVerify(exactly = 3) { view.showError(TestException("test")) }
    }

    @Test
    fun `updateRecipientsFromEcho gets rating for the recipient that sent the echo using PlanckProvider`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        helper.updateRecipientsFromEcho(
            listOf(trustedRecipient, secureRecipient, unencryptedRecipient),
            SENDER_ADDRESS,
            ratedListener
        )
        advanceUntilIdle()


        coVerify { planck.getRating(unencryptedAddress) }
        coVerify(exactly = 0) { planck.getRating(trustedAddress) }
        coVerify(exactly = 0) { planck.getRating(secureAddress) }
    }

    @Test
    fun `updateRecipientsFromEcho calls listener with updated recipient`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        helper.updateRecipientsFromEcho(
            listOf(trustedRecipient, secureRecipient, unencryptedRecipient),
            SENDER_ADDRESS,
            ratedListener
        )
        advanceUntilIdle()


        val ratedRecipientSlot = slot<MutableList<RatedRecipient>>()
        coVerify { ratedListener.ratedRecipientsReady(capture(ratedRecipientSlot)) }
        assertEquals(1, ratedRecipientSlot.captured.size)
        val ratedRecipient = ratedRecipientSlot.captured.first()
        assertEquals(unencryptedRecipient, ratedRecipient.baseRecipient)
        assertEquals(Rating.pEpRatingReliable, ratedRecipient.rating)
    }

    @Test
    fun `updateRecipientsFromEcho uses undefined rating as default if PlanckProvider_getRating fails`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        helper.updateRecipientsFromEcho(
            listOf(trustedRecipient, secureRecipient, unencryptedRecipient),
            SENDER_ADDRESS,
            ratedListener
        )
        advanceUntilIdle()


        val ratedRecipientSlot = slot<MutableList<RatedRecipient>>()
        coVerify { ratedListener.ratedRecipientsReady(capture(ratedRecipientSlot)) }
        assertEquals(1, ratedRecipientSlot.captured.size)
        val ratedRecipient = ratedRecipientSlot.captured.first()
        assertEquals(unencryptedRecipient, ratedRecipient.baseRecipient)
        assertEquals(Rating.pEpRatingUndefined, ratedRecipient.rating)
    }

    @Test
    fun `updateRecipientsFromEcho calls view_showError if PlanckProvider_getRating fails`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        helper.updateRecipientsFromEcho(
            listOf(trustedRecipient, secureRecipient, unencryptedRecipient),
            SENDER_ADDRESS,
            ratedListener
        )
        advanceUntilIdle()


        coVerify { view.showError(TestException("test")) }
    }

    private fun addUnsecureRecipient(recipient: Recipient) {
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(recipient.address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingCannotDecrypt) }


        helper.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertTrue(helper.hasHiddenUnsecureAddressChannel(arrayOf(recipient.address), 1))
    }

    private data class TestException(override val message: String) : Throwable()
}
