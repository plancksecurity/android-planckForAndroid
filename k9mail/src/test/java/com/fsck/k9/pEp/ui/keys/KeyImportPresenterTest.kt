package com.fsck.k9.pEp.ui.keys

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.mail.Address
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PEpUtils
import com.fsck.k9.pEp.testutils.AssertUtils.identityThat
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.fsck.k9.pEp.testutils.PEpProviderStubber
import com.nhaarman.mockito_kotlin.*
import foundation.pEp.jniadapter.Identity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.annotation.Config
import security.pEp.ui.keyimport.KeyImportPresenter
import security.pEp.ui.keyimport.KeyImportView


@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class KeyImportPresenterTest {

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val view: KeyImportView = mock()
    private val pEpProvider: PEpProvider = mock()
    private val pEpProviderStubber: PEpProviderStubber = PEpProviderStubber(pEpProvider)
    private lateinit var preferences: Preferences
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contextSpy: Context = spy(context)

    private val email1 = "account1@ignaciotest.ch"
    private val email2 = "account2@ignaciotest.ch"
    private val email3 = "account3@ignaciotest.ch"
    private val email4 = "account4@ignaciotest.ch"
    private val email5 = "account5@ignaciotest.ch"

    private val originalFingerPrint = "5E8AC44C88AC40F765E669BEB29D0FE10C05238E"

    private val importedFingerPrint = "EEB0B3646785B8E067AE745B4A5CBAACD5D9D435"

    // identities set in device
    private lateinit var firstIdentity: Identity
    private lateinit var secondIdentity: Identity
    private lateinit var thirdIdentity: Identity

    // identities NOT set in device
    private lateinit var fourthIdentity: Identity
    private lateinit var fifthIdentity: Identity

    private lateinit var presenter: KeyImportPresenter

    private fun Preferences.newReadyAccount() = newAccount().apply { this.setupState = Account.SetupState.READY }

    @Before
    fun setup() {
        doReturn(contextSpy).`when`(contextSpy).applicationContext
        doReturn("text").`when`(contextSpy).getString(anyInt())

        preferences = Preferences.getPreferences(contextSpy)

        preferences.newReadyAccount().email = email1
        preferences.newReadyAccount().email = email2
        preferences.newReadyAccount().email = email3

        // we set a fingeprint from the beginning for simplicity.
        firstIdentity =
            PEpUtils.createIdentity(Address(email1), context).apply { fpr = originalFingerPrint }
        secondIdentity =
            PEpUtils.createIdentity(Address(email2), context).apply { fpr = originalFingerPrint }
        thirdIdentity =
            PEpUtils.createIdentity(Address(email3), context).apply { fpr = originalFingerPrint }
        fourthIdentity =
            PEpUtils.createIdentity(Address(email4), context).apply { fpr = importedFingerPrint }
        fifthIdentity =
            PEpUtils.createIdentity(Address(email5), context).apply { fpr = importedFingerPrint }

        pEpProviderStubber.stubProviderMethodsForIdentity(firstIdentity)
        pEpProviderStubber.stubProviderMethodsForIdentity(secondIdentity)
        pEpProviderStubber.stubProviderMethodsForIdentity(thirdIdentity)
        pEpProviderStubber.stubProviderMethodsForIdentity(fourthIdentity)
        pEpProviderStubber.stubProviderMethodsForIdentity(fifthIdentity)


        presenter = KeyImportPresenter(
            preferences,
            pEpProvider,
            contextSpy,
            coroutinesTestRule.testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        preferences.accounts = mutableListOf() // clear the accounts in Preferences
    }

    @Test
    fun `initialize with accountUuid provided only calls myself for that account's identity`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            presenter.initialize(view, preferences.accounts.first().uuid)



            verify(pEpProvider).myself(identityThat { it.address == email1 })
            verify(pEpProvider, never()).myself(identityThat { it.address != email1 })
            verify(view).openFileChooser()
        }

    @Test
    fun `initialize with NO accountUuid provided calls myself for all accounts in preferences`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            presenter.initialize(view)



            preferences.accounts.forEach { account ->
                verify(pEpProvider).myself(identityThat { it.address == account.email })
            }
            verify(view).openFileChooser()
        }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FakeAndroidKeyStore.setup()
        }
    }
}