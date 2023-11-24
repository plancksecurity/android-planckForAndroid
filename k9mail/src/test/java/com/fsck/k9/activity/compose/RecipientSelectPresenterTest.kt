package com.fsck.k9.activity.compose

import com.fsck.k9.K9
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Rating
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
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
    private val view: RecipientSelectContract = mockk(relaxed = true)
    private val facadePresenter: RecipientPresenter = mockk(relaxed = true)

    private val presenter = RecipientSelectPresenter(planck)

    @Before
    fun setup() {
        every { view.hasRecipient(any()) }.returns(true)
        every { view.isAlwaysUnsecure }.returns(false)
        mockkStatic(K9::class)
        every { K9.isPlanckForwardWarningEnabled() }.returns(true)
        presenter.initialize(view)
        presenter.setPresenter(facadePresenter, Message.RecipientType.TO)
    }

    @Test
    fun `setPresenter calls RecipientPresenter_setPresenter`() {
        verify { facadePresenter.setPresenter(presenter, Message.RecipientType.TO) }
    }

    @Test
    fun `getRecipientRating uses PlanckProvider to get rating`() {
        val address: Address = mockk()
        val recipient: Recipient = mockk()
        every { recipient.address }.returns(address)
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)


        presenter.getRecipientRating(recipient, true, callback)


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


        presenter.getRecipientRating(recipient, true, callback)


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


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
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


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertEquals(0, presenter.unsecureAddressChannelCount)
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


        presenter.getRecipientRating(recipient, false, callback)


        assertEquals(0, presenter.unsecureAddressChannelCount)
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


        presenter.getRecipientRating(recipient, true, callback)


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


        presenter.getRecipientRating(recipient, true, callback)


        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
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


        presenter.getRecipientRating(recipient, false, callback)


        assertEquals(0, presenter.unsecureAddressChannelCount)
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


        presenter.getRecipientRating(recipient, true, callback)


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


        presenter.getRecipientRating(recipient, true, callback)


        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(address), 1))
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


        presenter.getRecipientRating(recipient, true, callback)


        assertEquals(0, presenter.unsecureAddressChannelCount)
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


        presenter.getRecipientRating(recipient, true, callback)


        verify { facadePresenter.showError(TestException("test")) }
    }

    @Test
    fun `rateAlternateRecipients gets rating for recipients using PlanckProvider`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        presenter.rateAlternateRecipients(listOf(recipient1, recipient2))
        advanceUntilIdle()


        coVerify { planck.getRating(address1) }
        coVerify { planck.getRating(address2) }
    }

    @Test
    fun `rateAlternateRecipients calls view with rated recipients`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.success(Rating.pEpRatingTrustedAndAnonymized))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        presenter.rateAlternateRecipients(listOf(recipient1, recipient2))
        advanceUntilIdle()

        val ratedRecipientsSlot = slot<MutableList<RatedRecipient>>()
        coVerify { view.showAlternatesPopup(capture(ratedRecipientsSlot)) }

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
    fun `rateAlternateRecipients uses rating undefined if PlanckProvider_getRating fails`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.failure(TestException("test")))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        presenter.rateAlternateRecipients(listOf(recipient1, recipient2))
        advanceUntilIdle()

        val ratedRecipientsSlot = slot<MutableList<RatedRecipient>>()
        coVerify { view.showAlternatesPopup(capture(ratedRecipientsSlot)) }

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
    fun `rateAlternateRecipients calls RecipientPresenter_showError if PlanckProvider_getRating fails`() = runTest {
        val address1: Address = mockk()
        val address2: Address = mockk()
        coEvery { planck.getRating(address1) }
            .returns(ResultCompat.failure(TestException("test")))
        coEvery { planck.getRating(address2) }
            .returns(ResultCompat.success(Rating.pEpRatingUnencrypted))
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)


        presenter.rateAlternateRecipients(listOf(recipient1, recipient2))
        advanceUntilIdle()


        verify { facadePresenter.showError(TestException("test")) }
    }

    @Test
    fun `addRecipients calls view_addRecipient when adding only one recipient`() {
        val recipient = Recipient(mockk())


        presenter.addRecipients(recipient)


        verify { view.addRecipient(recipient) }
    }

    @Test
    fun `addRecipients gets rating for recipients using PlanckProvider`() = runTest {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        presenter.addRecipients(trustedRecipient, secureRecipient, undefinedRecipient)
        advanceUntilIdle()


        coVerify { planck.getRating(undefinedAddress) }
        coVerify { planck.getRating(trustedAddress) }
        coVerify { planck.getRating(secureAddress) }
    }

    @Test
    fun `addRecipients calls view with recipients sorted by rating`() = runTest {
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


        presenter.addRecipients(trustedRecipient, secureRecipient, undefinedRecipient)
        advanceUntilIdle()


        val sortedRecipientsSlot = mutableListOf<Recipient>()
        coVerify { view.addRecipient(capture(sortedRecipientsSlot)) }


        assertEquals(unencryptedAddress, sortedRecipientsSlot[0].address)
        assertEquals(secureAddress, sortedRecipientsSlot[1].address)
        assertEquals(trustedAddress, sortedRecipientsSlot[2].address)
    }

    @Test
    fun `addRecipients uses undefined rating as default if PlanckProvider_getRating fails`() =
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


            presenter.addRecipients(trustedRecipient, secureRecipient, undefinedRecipient)
            advanceUntilIdle()


            val sortedRecipientsSlot = mutableListOf<Recipient>()
            coVerify { view.addRecipient(capture(sortedRecipientsSlot)) }


            assertEquals(unencryptedAddress, sortedRecipientsSlot[1].address)
            assertEquals(secureAddress, sortedRecipientsSlot[2].address)
            assertEquals(trustedAddress, sortedRecipientsSlot[0].address)
        }

    @Test
    fun `addRecipients calls view_showError if PlanckProvider_getRating fails`() = runTest {
        val undefinedAddress: Address = mockk()
        val secureAddress: Address = mockk()
        val trustedAddress: Address = mockk()
        val undefinedRecipient = Recipient(undefinedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        presenter.addRecipients(trustedRecipient, secureRecipient, undefinedRecipient)
        advanceUntilIdle()


        coVerify(exactly = 3) { facadePresenter.showError(TestException("test")) }
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
        coEvery { view.recipients }.returns(listOf(secureRecipient, trustedRecipient, unencryptedRecipient))
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        presenter.updateRecipientsFromMessage()
        advanceUntilIdle()


        coVerify { planck.getRating(unencryptedAddress) }
        coVerify(exactly = 0) { planck.getRating(trustedAddress) }
        coVerify(exactly = 0) { planck.getRating(secureAddress) }
    }

    @Test
    fun `updateRecipientsFromEcho calls view with updated recipient`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { view.recipients }.returns(listOf(secureRecipient, trustedRecipient, unencryptedRecipient))
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.success(Rating.pEpRatingReliable))


        presenter.updateRecipientsFromMessage()
        advanceUntilIdle()


        val ratedRecipientSlot = slot<MutableList<RatedRecipient>>()
        coVerify { view.updateRecipients(capture(ratedRecipientSlot)) }
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
        coEvery { view.recipients }.returns(listOf(secureRecipient, trustedRecipient, unencryptedRecipient))
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        presenter.updateRecipientsFromMessage()
        advanceUntilIdle()


        val ratedRecipientSlot = slot<MutableList<RatedRecipient>>()
        coVerify { view.updateRecipients(capture(ratedRecipientSlot)) }
        assertEquals(1, ratedRecipientSlot.captured.size)
        val ratedRecipient = ratedRecipientSlot.captured.first()
        assertEquals(unencryptedRecipient, ratedRecipient.baseRecipient)
        assertEquals(Rating.pEpRatingUndefined, ratedRecipient.rating)
    }

    @Test
    fun `updateRecipientsFromEcho calls RecipientPresenter_showError if PlanckProvider_getRating fails`() = runTest {
        val unencryptedAddress: Address = mockk()
        val secureAddress: Address = mockk(relaxed = true)
        val trustedAddress: Address = mockk(relaxed = true)
        val unencryptedRecipient = Recipient(unencryptedAddress)
        val secureRecipient = Recipient(secureAddress)
        val trustedRecipient = Recipient(trustedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { view.recipients }.returns(listOf(secureRecipient, trustedRecipient, unencryptedRecipient))
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        presenter.updateRecipientsFromMessage()
        advanceUntilIdle()


        coVerify { facadePresenter.showError(TestException("test")) }
    }

    @Test
    fun `updateRecipientsFromEcho calls RecipientPresenter_handleUnsecureDeliveryWarning`() = runTest {
        val unencryptedAddress: Address = mockk()
        val unencryptedRecipient = Recipient(unencryptedAddress)
        addUnsecureRecipient(unencryptedRecipient)
        coEvery { view.recipients }.returns(listOf(unencryptedRecipient))
        coEvery { unencryptedAddress.address }.returns(SENDER_ADDRESS)
        coEvery { planck.getRating(any<Address>()) }
            .returns(ResultCompat.failure(TestException("test")))


        presenter.updateRecipientsFromMessage()
        advanceUntilIdle()


        coVerify { facadePresenter.handleUnsecureDeliveryWarning() }
    }

    @Test
    fun `reportedUncompleteRecipients calls view_showUncompletedError if view_hasUncompleteRecipients is true`() {
        every { view.hasUncompletedRecipients() }.returns(true)


        val result = presenter.reportedUncompletedRecipients()


        verify { view.hasUncompletedRecipients() }
        verify { view.showUncompletedError() }
        assertTrue(result)
    }

    @Test
    fun `tryPerformCompletion calls view_tryPerformCompletion`() {
        presenter.tryPerformCompletion()


        verify { view.tryPerformCompletion() }
    }

    @Test
    fun `notifyRecipientsChanged removes and adds all recipients and restores first recipient truncation`() {
        val address1: Address = mockk()
        val address2: Address = mockk()
        val address3: Address = mockk()
        val recipient1 = Recipient(address1)
        val recipient2 = Recipient(address2)
        val recipient3 = Recipient(address3)
        every { view.recipients }.returns(listOf(recipient1, recipient2, recipient3))


        presenter.notifyRecipientsChanged()


        verify { view.removeRecipient(recipient1) }
        verify { view.removeRecipient(recipient2) }
        verify { view.removeRecipient(recipient3) }
        verify { view.addRecipient(recipient1) }
        verify { view.addRecipient(recipient2) }
        verify { view.addRecipient(recipient3) }
        verify { view.restoreFirstRecipientTruncation() }
    }

    @Test
    fun `onRecipientsChanged calls RecipientPresenter_onRecipientsChanged`() {
        presenter.onRecipientsChanged()


        verify { facadePresenter.onRecipientsChanged() }
    }

    @Test
    fun `handleUnsecureTokenWarning calls RecipientPresenter_handleUnsecureDeliveryWarning`() {
        presenter.handleUnsecureTokenWarning()


        verify { facadePresenter.handleUnsecureDeliveryWarning() }
    }

    private fun addUnsecureRecipient(recipient: Recipient) {
        val callback: PlanckProvider.ResultCallback<Rating> = mockk(relaxed = true)
        val callbackSlot = slot<PlanckProvider.ResultCallback<Rating>>()
        every { planck.getRating(recipient.address, capture(callbackSlot)) }
            .answers { callbackSlot.captured.onLoaded(Rating.pEpRatingCannotDecrypt) }


        presenter.getRecipientRating(recipient, true, callback)


        verify { callback.onLoaded(Rating.pEpRatingCannotDecrypt) }
        assertTrue(presenter.hasHiddenUnsecureAddressChannel(arrayOf(recipient.address), 1))
    }

    private data class TestException(override val message: String) : Throwable()
}
