package com.fsck.k9.activity.compose

import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Rating
import io.mockk.*
import junit.framework.TestCase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class UnsecureAddressHelperTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val pEp: PEpProvider = mockk(relaxed = true)
    private val listener: RecipientsReadyListener = mockk(relaxed = true)
    private val ratedListener: RatedRecipientsReadyListener = mockk(relaxed = true)
    private val view: RecipientSelectViewContract = mockk()

    private val presenter = UnsecureAddressHelper(pEp)

    @Before
    fun setup() {
        every { view.hasRecipient(any()) }.returns(true)
        every { view.isAlwaysUnsecure }.returns(false)
        presenter.initialize(view)
    }

    @Test
    fun `getRecipientRating uses PEpProvider to get rating`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)


        presenter.getRecipientRating(recipient, true, callback)


        verify { pEp.getRating(address, any()) }
    }

    @Test
    fun `getRecipientRating calls callback_onLoaded when there is no problem`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingReliable) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel when it gets an unsecure rating`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingUndefined) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingUndefined) }
        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel if recipient is not in view`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.hasRecipient(any()) }.returns(false)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingUndefined) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingUndefined) }
        assertFalse(presenter.isUnsecureChannel())
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel when getRating is successful and is not pEp privacy protected`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingUndefined) }


        presenter.getRecipientRating(recipient, false, callback)


        assertFalse(presenter.isUnsecureChannel())
    }

    @Test
    fun `getRecipientRating calls callback_onError when there is a problem`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        val testException = RuntimeException("test")
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(testException) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onError(testException) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel when getRating is not successful`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(RuntimeException()) }


        presenter.getRecipientRating(recipient, true, callback)


        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }

    @Test
    fun `getRecipientRating does not add unsecure address channel when getRating is not successful and is not pEp privacy protected`() {
        every { view.hasRecipient(any()) }.returns(true)
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onError(RuntimeException()) }


        presenter.getRecipientRating(recipient, false, callback)


        assertFalse(presenter.isUnsecureChannel())
    }

    @Test
    fun `rateRecipients gets rating for recipients using PEpProvider`() {
        val address1: Address = mockk()
        val address2: Address = mockk()
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        presenter.rateRecipients(listOf(recipient1, recipient2), ratedListener)


        coVerify { pEp.getRating(address1) }
        coVerify { pEp.getRating(address2) }
    }

    @Test
    fun `rateRecipients calls listener with rated recipients`() {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { pEp.getRating(address1) }.returns(Rating.pEpRatingTrustedAndAnonymized)
        coEvery { pEp.getRating(address2) }.returns(Rating.pEpRatingUndefined)
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        presenter.rateRecipients(listOf(recipient1, recipient2), ratedListener)


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
            Rating.pEpRatingUndefined,
            ratedRecipientsSlot.captured[1].rating
        )
    }

    @Test
    fun `sortRecipientsByRating gets rating for recipients using PEpProvider`() {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)


        presenter.sortRecipientsByRating(
            arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
            listener
        )


        coVerify { pEp.getRating(undefinedAddress) }
        coVerify { pEp.getRating(trustedAddress) }
        coVerify { pEp.getRating(secureAddress) }
    }

    @Test
    fun `sortRecipientsByRating calls listener with recipients sorted by rating`() {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        coEvery { pEp.getRating(undefinedAddress) }.returns(Rating.pEpRatingUndefined)
        coEvery { pEp.getRating(secureAddress) }.returns(Rating.pEpRatingReliable)
        coEvery { pEp.getRating(trustedAddress) }.returns(Rating.pEpRatingTrustedAndAnonymized)
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)


        presenter.sortRecipientsByRating(
            arrayOf(trustedRecipient, secureRecipient, undefinedRecipient),
            listener
        )


        val sortedRecipientsSlot = slot<MutableList<Recipient>>()
        coVerify { listener.recipientsReady(capture(sortedRecipientsSlot)) }


        assertEquals(undefinedAddress, sortedRecipientsSlot.captured[0].address)
        assertEquals(secureAddress, sortedRecipientsSlot.captured[1].address)
        assertEquals(trustedAddress, sortedRecipientsSlot.captured[2].address)
    }

    @Test
    fun `getRecipientRating calls callback_onLoaded with rating undefined if view is always unsecure`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.isAlwaysUnsecure }.returns(true)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingUndefined) }
    }

    @Test
    fun `getRecipientRating adds unsecure address channel if view is always unsecure`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        every { view.isAlwaysUnsecure }.returns(true)
        val callback: PEpProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PEpProvider.ResultCallback<Rating>>()
        every { pEp.getRating(address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingReliable) }


        presenter.getRecipientRating(recipient, true, callback)


        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
    }
}
