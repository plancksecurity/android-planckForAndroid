package security.planck.ui.verifypartner

import android.app.Application
import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.models.PlanckIdentity
import com.fsck.k9.planck.models.mappers.PlanckIdentityMapper
import com.fsck.k9.planck.testutils.CoroutineTestRule
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.ui.verifypartner.VerifyPartnerState.ConfirmMistrust
import security.planck.ui.verifypartner.VerifyPartnerState.ConfirmTrust
import security.planck.ui.verifypartner.VerifyPartnerState.DeletedMessage
import security.planck.ui.verifypartner.VerifyPartnerState.ErrorGettingTrustwords
import security.planck.ui.verifypartner.VerifyPartnerState.ErrorLoadingMessage
import security.planck.ui.verifypartner.VerifyPartnerState.ErrorMistrusting
import security.planck.ui.verifypartner.VerifyPartnerState.ErrorTrusting
import security.planck.ui.verifypartner.VerifyPartnerState.Finish
import security.planck.ui.verifypartner.VerifyPartnerState.HandshakeReady
import security.planck.ui.verifypartner.VerifyPartnerState.Idle
import security.planck.ui.verifypartner.VerifyPartnerState.LoadingHandshakeData
import security.planck.ui.verifypartner.VerifyPartnerState.MistrustDone
import security.planck.ui.verifypartner.VerifyPartnerState.MistrustProgress
import security.planck.ui.verifypartner.VerifyPartnerState.TrustDone
import security.planck.ui.verifypartner.VerifyPartnerState.TrustProgress

private const val TEST_TRUSTWORDS = "TEST TRUSTWORDS"
private const val GERMAN_TRUSTWORDS = "GERMAN TRUSTWORDS"
private const val LONG_TRUSTWORDS = "LONG TRUSTWORDS"
private const val MYSELF_FPR = "MY_FPR"
private const val PARTNER_FPR = "PARTNER_FPR"
private const val ENGLISH_LANGUAGE = "en"
private const val GERMAN_LANGUAGE = "de"
private const val GERMAN_POSITION = 1

private const val RECIPIENT_ADDRESS = "partnerxplanck@hello.ch"
private const val OWN_ADDRESS = "myselfxplanck@hello.ch"
private val TEST_RATING = Rating.pEpRatingReliable
private val TRUSTED_RATING = Rating.pEpRatingTrusted
private val MISTRUSTED_RATING = Rating.pEpRatingMistrust

@ExperimentalCoroutinesApi
class VerifyPartnerViewModelTest : LiveDataTest<VerifyPartnerState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val partner = Identity().apply {
        address = RECIPIENT_ADDRESS
        fpr = PARTNER_FPR
    }
    private val partnerMapped = mappedIdentity(partner)
    private val myself = Identity().apply {
        address = OWN_ADDRESS
        fpr = MYSELF_FPR
    }
    private val partnerAddress: Address = mockk {
        every { address }.returns(partner.address)
        every { personal }.returns(null)
    }
    private val myselfAddress: Address = mockk {
        every { address }.returns(myself.address)
        every { personal }.returns(null)
    }

    private val context: Application = mockk()
    private val account: Account = mockk()
    private val preferences: Preferences = mockk {
        every { getAccount(any()) }.returns(account)
    }
    private val localMessage: LocalMessage = mockk(relaxed = true) {
        every { planckRating }.returns(TEST_RATING)
    }
    private val controller: MessagingController = mockk()
    private val mapper: PlanckIdentityMapper = mockk {
        coEvery { updateAndMapRecipient(partner) }.returns(partnerMapped)
    }
    private val planckProvider: PlanckProvider = mockk(relaxed = true)
    private val cache: PlanckUIArtefactCache = mockk {
        every { recipients }.returns(arrayListOf(partner))
    }

    private lateinit var viewModel: VerifyPartnerViewModel
    override val testLivedata: LiveData<VerifyPartnerState>
        get() = viewModel.state

    private val testMessageReference =
        MessageReference("", "", "", null)

    override fun initialize() {
        mockkStatic(K9::class)
        every { K9.getK9CurrentLanguage() }.returns(ENGLISH_LANGUAGE)
        stubPlanckUtils()
        mockkStatic(Address::class)
        coEvery { Address.create(partner.address) }.returns(partnerAddress)
        coEvery { Address.create(myself.address) }.returns(myselfAddress)
        stupPlanckProvider()
        coEvery { controller.loadMessage(any(), any(), any()) }.returns(localMessage)
        createViewModel()
    }

    private fun stubPlanckUtils() {
        mockkStatic(PlanckUtils::class)
        coEvery { PlanckUtils.createIdentity(myselfAddress, context) }.returns(myself)
        coEvery { PlanckUtils.createIdentity(partnerAddress, context) }.returns(partner)
        coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)
        val slot = slot<String>()
        coEvery { PlanckUtils.formatFpr(capture(slot)) }.coAnswers { slot.captured }
        coEvery { PlanckUtils.trustWordsAvailableForLang(any()) }.returns(true)
    }

    private fun stupPlanckProvider() {
        val identitySlot = slot<Identity>()
        coEvery { planckProvider.myselfSuspend(capture(identitySlot)) }.coAnswers { identitySlot.captured }
        coEvery {
            planckProvider.trustwords(
                any(),
                any(),
                ENGLISH_LANGUAGE,
                true
            )
        }.returns(ResultCompat.success(TEST_TRUSTWORDS))
        coEvery {
            planckProvider.trustwords(
                any(),
                any(),
                GERMAN_LANGUAGE,
                true
            )
        }.returns(ResultCompat.success(GERMAN_TRUSTWORDS))
        coEvery {
            planckProvider.trustwords(
                any(),
                any(),
                any(),
                false
            )
        }.returns(ResultCompat.success(LONG_TRUSTWORDS))
        coEvery { planckProvider.incomingMessageRating(any()) }.returns(
            ResultCompat.success(
                TRUSTED_RATING
            )
        )
        coEvery {
            planckProvider.getRatingResult(
                any(),
                any(),
                any(),
                any()
            )
        }.returns(ResultCompat.success(TRUSTED_RATING))
    }

    private fun createViewModel() {
        viewModel = VerifyPartnerViewModel(
            context,
            preferences,
            controller,
            mapper,
            planckProvider,
            cache,
            coroutinesTestRule.testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(PlanckUtils::class)
        unmockkStatic(Address::class)
    }

    @Test
    fun `initial state is Idle`() {
        assertObservedValues(Idle)
    }

    @Test
    fun `initialize() sets state to LoadingHandshakeData twice`() =
        runTest { // once to load message and once to retrieve trustwords
            initializeViewModel()
            advanceUntilIdle()


            assertFirstObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData
            )
        }

    @Test
    fun `initialize() sets state to ErrorLoadingMessage if MessageReference is null for incoming message`() = runTest {
        initializeViewModel(incoming = true, messageReference = null)
        advanceUntilIdle()


        verify(exactly = 0) { Address.create(any()) }
        verify(exactly = 0) { PlanckUtils.createIdentity(any(), any()) }
        verify(exactly = 0) { planckProvider.myself(any()) }
        assertObservedValues(Idle, LoadingHandshakeData, ErrorLoadingMessage)
    }

    @Test
    fun `initialize() creates the identity for myself`() = runTest {
        initializeViewModel()
        advanceUntilIdle()


        verify { Address.create(myself.address) }
        verify { Address.create(partner.address) }
        verify { PlanckUtils.createIdentity(myselfAddress, context) }
        coVerify { planckProvider.myselfSuspend(myself) }
    }

    @Test
    fun `initialize() calls MessagingController_loadMessage if message reference is provided`() = runTest {
        initializeViewModel()
        advanceUntilIdle()


        coVerify { controller.loadMessage(account, testMessageReference.folderName, testMessageReference.uid) }
    }

    @Test
    fun `initialize() does not call MessagingController_loadMessage if message reference is not provided`() = runTest {
        initializeViewModel(incoming = false, messageReference = null)
        advanceUntilIdle()


        coVerify { controller.wasNot(called) }
    }

    @Test
    fun `initialize() sets state to ErrorLoadingMessage if there is an issue during initialization`() =
        runTest {
            coEvery { PlanckUtils.createIdentity(any(), any()) }.throws(RuntimeException("test"))


            initializeViewModel()
            advanceUntilIdle()


            verify { PlanckUtils.createIdentity(myselfAddress, context) }
            verify(exactly = 0) { planckProvider.myself(myself) }
            assertObservedValues(Idle, LoadingHandshakeData, ErrorLoadingMessage)
        }

    @Test
    fun `initialize() sets state to DeletedMessage if loadedMessage was deleted`() = runTest {
        coEvery { localMessage.isSet(Flag.DELETED) }.returns(true)


        initializeViewModel()
        advanceUntilIdle()


        assertObservedValues(Idle, LoadingHandshakeData, DeletedMessage)
    }

    @Test
    fun `initialize() sets state to ErrorLoadingMessage if there is an issue during message load`() =
        runTest {
            coEvery { controller.loadMessage(any(), any(), any()) }.throws(RuntimeException("test"))


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(Idle, LoadingHandshakeData, ErrorLoadingMessage)
        }

    @Test
    fun `initialize() sets state to HandshakeReady with empty trustwords if partner is not a planck user`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(false)


            initializeViewModel()
            advanceUntilIdle()


            coVerify(exactly = 0) { planckProvider.trustwords(any(), any(), any(), any()) }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    "",
                    shortTrustwords = true,
                    allowChangeTrust = true
                )
            )
        }

    @Test
    fun `initialize() uses PlanckIdentityMapper to map first cache recipent's identity`() =
        runTest {
            initializeViewModel()
            advanceUntilIdle()


            coVerify { cache.recipients }
            coVerify { mapper.updateAndMapRecipient(partner) }
        }

    @Test
    fun `initialize() uses PlanckProvider to get trustwords if partner is a planck user`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)


            initializeViewModel()
            advanceUntilIdle()


            coVerify { planckProvider.trustwords(myself, partnerMapped, ENGLISH_LANGUAGE, true) }
        }

    @Test
    fun `initialize() sets state to HandshakeReady with trustwords if partner is a planck user`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                )
            )
        }

    @Test
    fun `initialize() sets state to HandshakeReady without allowing to change trust if message rating is not handshake rating`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)
            coEvery { PlanckUtils.isHandshakeRating(any()) }.returns(false)


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = false
                )
            )
        }

    @Test
    fun `initialize() sets state to ErrorGettingTrustwords if PlanckIdentityMapper call fails`() =
        runTest {
            coEvery { mapper.updateAndMapRecipient(any()) }.throws(RuntimeException("test"))


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                ErrorGettingTrustwords
            )
        }

    @Test
    fun `initialize() sets state to ErrorGettingTrustwords if PlanckProvider_trustwords returns a blanck string`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)
            coEvery {
                planckProvider.trustwords(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }.returns(ResultCompat.success(" "))


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                ErrorGettingTrustwords
            )
        }

    @Test
    fun `initialize() sets state to ErrorGettingTrustwords if PlanckProvider_trustwords fails`() =
        runTest {
            coEvery { PlanckUtils.isPEpUser(any()) }.returns(true)
            coEvery {
                planckProvider.trustwords(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }.returns(ResultCompat.failure(RuntimeException("test")))


            initializeViewModel()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                ErrorGettingTrustwords
            )
        }

    @Test
    fun `positiveAction() after message loaded sets state to ConfirmTrust`() = runTest {
        initializeViewModel()
        advanceUntilIdle()
        viewModel.positiveAction()


        assertObservedValues(
            Idle,
            LoadingHandshakeData,
            LoadingHandshakeData,
            HandshakeReady(
                myself.address,
                partner.address,
                MYSELF_FPR,
                PARTNER_FPR,
                TEST_TRUSTWORDS,
                shortTrustwords = true,
                allowChangeTrust = true
            ),
            ConfirmTrust(partner.address)
        )
    }

    @Test
    fun `negativeAction() after message loaded sets state to ConfirmMistrust`() = runTest {
        initializeViewModel()
        advanceUntilIdle()
        viewModel.negativeAction()


        assertObservedValues(
            Idle,
            LoadingHandshakeData,
            LoadingHandshakeData,
            HandshakeReady(
                myself.address,
                partner.address,
                MYSELF_FPR,
                PARTNER_FPR,
                TEST_TRUSTWORDS,
                shortTrustwords = true,
                allowChangeTrust = true
            ),
            ConfirmMistrust(partner.address)
        )
    }

    @Test
    fun `positiveAction() on trust confirmation sets state to TrustProgress`() = runTest {
        initializeViewModel()
        advanceUntilIdle()
        viewModel.positiveAction()
        viewModel.positiveAction()
        advanceUntilIdle()


        assertFirstObservedValues(
            Idle,
            LoadingHandshakeData,
            LoadingHandshakeData,
            HandshakeReady(
                myself.address,
                partner.address,
                MYSELF_FPR,
                PARTNER_FPR,
                TEST_TRUSTWORDS,
                shortTrustwords = true,
                allowChangeTrust = true
            ),
            ConfirmTrust(partner.address),
            TrustProgress(partner.address)
        )
    }

    @Test
    fun `positiveAction() on mistrust confirmation sets state to MistrustProgress`() = runTest {
        initializeViewModel()
        advanceUntilIdle()
        viewModel.negativeAction()
        viewModel.positiveAction()
        advanceUntilIdle()


        assertFirstObservedValues(
            Idle,
            LoadingHandshakeData,
            LoadingHandshakeData,
            HandshakeReady(
                myself.address,
                partner.address,
                MYSELF_FPR,
                PARTNER_FPR,
                TEST_TRUSTWORDS,
                shortTrustwords = true,
                allowChangeTrust = true
            ),
            ConfirmMistrust(partner.address),
            MistrustProgress(partner.address)
        )
    }

    @Test
    fun `positiveAction() on trust confirmation uses PlanckProvider to trust partner`() = runTest {
        initializeViewModel()
        advanceUntilIdle()
        viewModel.positiveAction()
        viewModel.positiveAction()
        advanceUntilIdle()


        coVerify { planckProvider.trustPersonalKey(partnerMapped) }
    }

    @Test
    fun `positiveAction() on trust confirmation sets state to ErrorTrusting if PlanckProvider_trustPersonalKey fails`() =
        runTest {
            coEvery { planckProvider.trustPersonalKey(any()) }.throws(RuntimeException("test"))
            initializeViewModel()
            advanceUntilIdle()
            viewModel.positiveAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmTrust(partner.address),
                TrustProgress(partner.address),
                ErrorTrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on mistrust confirmation uses PlanckProvider to mistrust partner`() =
        runTest {
            initializeViewModel()
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.keyMistrusted(partnerMapped) }
        }

    @Test
    fun `positiveAction() on mistrust confirmation sets state to ErrorMistrusting if PlanckProvider_keyMistrusted fails`() =
        runTest {
            coEvery { planckProvider.keyMistrusted(any()) }.throws(RuntimeException("test"))
            initializeViewModel()
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                ErrorMistrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on confirmation retrieves new rating for incoming message and updates local message rating`() =
        runTest {
            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.incomingMessageRating(localMessage) }
            coVerify { localMessage.planckRating = TRUSTED_RATING }
        }

    @Test
    fun `positiveAction() on trust confirmation for incoming message sets state to ErrorTrusting if PlanckProvider_incomingMessageRating() fails`() =
        runTest {
            coEvery { planckProvider.incomingMessageRating(any()) }.returns(
                ResultCompat.failure(
                    RuntimeException("test")
                )
            )


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.positiveAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.incomingMessageRating(localMessage) }
            coVerify(exactly = 0) { localMessage.planckRating = any() }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmTrust(partner.address),
                TrustProgress(partner.address),
                ErrorTrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on trust confirmation for incoming message sets state to ErrorTrusting if it fails to save new rating to message`() =
        runTest {
            coEvery { localMessage.planckRating = any() }.throws(RuntimeException("test"))


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.positiveAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.incomingMessageRating(localMessage) }
            coVerify { localMessage.planckRating = TRUSTED_RATING }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmTrust(partner.address),
                TrustProgress(partner.address),
                ErrorTrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on mistrust confirmation for incoming message sets state to ErrorMistrusting if PlanckProvider_incomingMessageRating() fails`() =
        runTest {
            coEvery { planckProvider.incomingMessageRating(any()) }.returns(
                ResultCompat.failure(
                    RuntimeException("test")
                )
            )


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.incomingMessageRating(localMessage) }
            coVerify(exactly = 0) { localMessage.planckRating = any() }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                ErrorMistrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on mistrust confirmation for incoming message sets state to ErrorMistrusting if it fails to save new rating to message`() =
        runTest {
            coEvery { planckProvider.incomingMessageRating(any()) }.returns(
                ResultCompat.success(
                    MISTRUSTED_RATING
                )
            )
            coEvery { localMessage.planckRating = any() }.throws(RuntimeException("test"))


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify { planckProvider.incomingMessageRating(localMessage) }
            coVerify { localMessage.planckRating = MISTRUSTED_RATING }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                ErrorMistrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on confirmation retrieves new rating for outgoing message and does not update local message rating`() =
        runTest {
            initializeViewModel(incoming = false)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify {
                planckProvider.getRatingResult(
                    myselfAddress,
                    listOf(partnerAddress),
                    emptyList(),
                    emptyList()
                )
            }
            coVerify(exactly = 0) { localMessage.planckRating = any() }
        }

    @Test
    fun `positiveAction() on trust confirmation for outgoing message sets state to ErrorTrusting if PlanckProvider_getRatingResult() fails`() =
        runTest {
            coEvery { planckProvider.getRatingResult(any(), any(), any(), any()) }.returns(
                ResultCompat.failure(RuntimeException("test"))
            )


            initializeViewModel(incoming = false)
            advanceUntilIdle()
            viewModel.positiveAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify {
                planckProvider.getRatingResult(
                    myselfAddress,
                    listOf(partnerAddress),
                    emptyList(),
                    emptyList()
                )
            }
            coVerify(exactly = 0) { localMessage.planckRating = any() }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmTrust(partner.address),
                TrustProgress(partner.address),
                ErrorTrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on mistrust confirmation for outgoing message sets state to ErrorMistrusting if PlanckProvider_getRatingResult() fails`() =
        runTest {
            coEvery { planckProvider.getRatingResult(any(), any(), any(), any()) }.returns(
                ResultCompat.failure(RuntimeException("test"))
            )


            initializeViewModel(incoming = false)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            coVerify {
                planckProvider.getRatingResult(
                    myselfAddress,
                    listOf(partnerAddress),
                    emptyList(),
                    emptyList()
                )
            }
            coVerify(exactly = 0) { localMessage.planckRating = any() }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                ErrorMistrusting(partner.address)
            )
        }

    @Test
    fun `positiveAction() on trust confirmation sets state to TrustDone if operation is successful`() =
        runTest {
            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.positiveAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmTrust(partner.address),
                TrustProgress(partner.address),
                TrustDone(
                    partner.address,
                    mapOf(VerifyPartnerFragment.RESULT_KEY_RATING to TRUSTED_RATING.toString())
                )
            )
        }

    @Test
    fun `positiveAction() on mistrust confirmation sets state to MistrustDone if operation is successful`() =
        runTest {
            coEvery { planckProvider.incomingMessageRating(any()) }.returns(
                ResultCompat.success(
                    MISTRUSTED_RATING
                )
            )


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                MistrustDone(
                    partner.address,
                    mapOf(VerifyPartnerFragment.RESULT_KEY_RATING to MISTRUSTED_RATING.toString())
                )
            )
        }

    @Test
    fun `negativeAction() on confirmation retrieves handshake data and displays handshake again`() =
        runTest {
            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.negativeAction()
            advanceUntilIdle()


            coVerify { cache.recipients }
            coVerify { mapper.updateAndMapRecipient(partner) }
            coVerify { planckProvider.trustwords(myself, partnerMapped, ENGLISH_LANGUAGE, true) }


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
            )
        }

    @Test
    fun `changeTrustwordsLanguage() resets HandshakeReady state with new trustwords in new language`() =
        runTest {
            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.changeTrustwordsLanguage(GERMAN_POSITION)
            advanceUntilIdle()


            coVerify { PlanckUtils.getPlanckLocales() }
            coVerify { planckProvider.trustwords(myself, partnerMapped, ENGLISH_LANGUAGE, true) }
            coVerify { planckProvider.trustwords(myself, partnerMapped, GERMAN_LANGUAGE, true) }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    GERMAN_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
            )
        }

    @Test
    fun `switchTrustwordsLength() resets HandshakeReady state with new trustwords with new length`() =
        runTest {
            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.switchTrustwordsLength()
            advanceUntilIdle()


            coVerify { planckProvider.trustwords(myself, partnerMapped, ENGLISH_LANGUAGE, true) }
            coVerify { planckProvider.trustwords(myself, partnerMapped, ENGLISH_LANGUAGE, false) }
            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    LONG_TRUSTWORDS,
                    shortTrustwords = false,
                    allowChangeTrust = true
                ),
            )
        }

    @Test
    fun `finish() sets state to Finish with null-filled result map if not filled yet`() = runTest {
        initializeViewModel(incoming = true)
        advanceUntilIdle()
        viewModel.finish()


        assertObservedValues(
            Idle,
            LoadingHandshakeData,
            LoadingHandshakeData,
            HandshakeReady(
                myself.address,
                partner.address,
                MYSELF_FPR,
                PARTNER_FPR,
                TEST_TRUSTWORDS,
                shortTrustwords = true,
                allowChangeTrust = true
            ),
            Finish(mapOf(VerifyPartnerFragment.RESULT_KEY_RATING to null))
        )
    }

    @Test
    fun `finish() sets state to Finish with empty result map if result was already delivered`() =
        runTest {
            coEvery { planckProvider.incomingMessageRating(any()) }.returns(
                ResultCompat.success(
                    MISTRUSTED_RATING
                )
            )


            initializeViewModel(incoming = true)
            advanceUntilIdle()
            viewModel.negativeAction()
            viewModel.positiveAction()
            advanceUntilIdle()
            viewModel.finish()


            assertObservedValues(
                Idle,
                LoadingHandshakeData,
                LoadingHandshakeData,
                HandshakeReady(
                    myself.address,
                    partner.address,
                    MYSELF_FPR,
                    PARTNER_FPR,
                    TEST_TRUSTWORDS,
                    shortTrustwords = true,
                    allowChangeTrust = true
                ),
                ConfirmMistrust(partner.address),
                MistrustProgress(partner.address),
                MistrustDone(
                    partner.address,
                    mapOf(VerifyPartnerFragment.RESULT_KEY_RATING to MISTRUSTED_RATING.toString())
                ),
                Finish(emptyMap())
            )
        }


    private fun initializeViewModel(
        incoming: Boolean = true,
        messageReference: MessageReference? = testMessageReference,
    ) {
        viewModel.initialize(
            sender = if (incoming) partner.address
            else myself.address,
            myself = myself.address,
            messageReference = messageReference,
            isMessageIncoming = incoming
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
        out.rating = TEST_RATING
        return out
    }
}