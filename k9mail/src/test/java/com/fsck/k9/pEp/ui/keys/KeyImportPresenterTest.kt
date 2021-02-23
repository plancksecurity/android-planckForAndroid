package com.fsck.k9.pEp.ui.keys

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import foundation.pEp.jniadapter.CommType
import foundation.pEp.jniadapter.Identity
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.robolectric.annotation.Config
import security.pEp.ui.keyimport.ACTIVITY_REQUEST_PICK_KEY_FILE
import security.pEp.ui.keyimport.KeyImportPresenter
import security.pEp.ui.keyimport.KeyImportView
import java.io.File
import java.io.FileInputStream


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
    private val resolver: ContentResolver = mock()

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

    private val firstImportedIdentity = Identity().apply {
        address = email1
        comm_type = CommType.PEP_ct_unknown
        flags = 0
        fpr = importedFingerPrint
        me = false
        user_id = null
        username = email1
    }

    private val secondImportedIdentity = Identity().apply {
        address = email2
        comm_type = CommType.PEP_ct_unknown
        flags = 0
        fpr = importedFingerPrint
        me = false
        user_id = null
        username = email2
    }

    private val thirdImportedIdentity = Identity().apply {
        address = email3
        comm_type = CommType.PEP_ct_unknown
        flags = 0
        fpr = importedFingerPrint
        me = false
        user_id = null
        username = email3
    }

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

        pEpProviderStubber.stubProviderMethodsForIdentity(firstIdentity, importedFingerPrint)
        pEpProviderStubber.stubProviderMethodsForIdentity(secondIdentity, importedFingerPrint)
        pEpProviderStubber.stubProviderMethodsForIdentity(thirdIdentity, importedFingerPrint)
        pEpProviderStubber.stubProviderMethodsForIdentity(fourthIdentity, importedFingerPrint)
        pEpProviderStubber.stubProviderMethodsForIdentity(fifthIdentity, importedFingerPrint)


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

    @Test
    fun `onActivityResult finishes view with bad result code`() {
        presenter.initialize(view, preferences.accounts.first().uuid)

        presenter.onActivityResult(
            Activity.RESULT_CANCELED,
            ACTIVITY_REQUEST_PICK_KEY_FILE,
            Intent()
        )

        verify(view).finish()
    }

    @Test
    fun `onActivityResult finishes view with bad request code`() {
        presenter.initialize(view, preferences.accounts.first().uuid)

        presenter.onActivityResult(Activity.RESULT_OK, 7, Intent())


        verify(view).finish()
    }

    @Test
    fun `onActivityResult finishes view with no data`() {
        presenter.initialize(view, preferences.accounts.first().uuid)

        presenter.onActivityResult(Activity.RESULT_OK, ACTIVITY_REQUEST_PICK_KEY_FILE, Intent())


        verify(view).finish()
    }

    @Test
    fun `importKey from account settings best case 1 account`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val expectedResult = listOf(firstImportedIdentity)
            pEpProviderStubber.stubImportKey(expectedResult)
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view, preferences.accounts.first().uuid)


            val result = presenter.importKey(uri)



            assertForImportKey(expectedResult, result)
        }

    @Test
    fun `importKey from account settings best case 3 accounts`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(
                listOf(
                    firstImportedIdentity,
                    secondImportedIdentity,
                    thirdImportedIdentity
                )
            )
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view, preferences.accounts.last().uuid)


            val result = presenter.importKey(uri)


            assertForImportKey(listOf(thirdImportedIdentity), result)
        }

    @Test
    fun `importKey from general settings best case 1 account`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val expectedResult = listOf(firstImportedIdentity)
            pEpProviderStubber.stubImportKey(expectedResult)
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view)


            val result = presenter.importKey(uri)


            assertForImportKey(expectedResult, result)
        }

    @Test
    fun `importKey removes duplicate identities from PEpProvider_importKey`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(
                listOf(
                    firstImportedIdentity,
                    secondImportedIdentity,
                    thirdImportedIdentity,
                    firstImportedIdentity,
                    secondImportedIdentity,
                    thirdImportedIdentity
                )
            )

            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view)


            val result = presenter.importKey(uri)


            verify(pEpProvider).importKey(any())
            TestCase.assertEquals(3, result.size)
        }

    @Test
    fun `importKey from general settings best case 3 accounts`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val expectedResult = listOf(
                firstImportedIdentity,
                secondImportedIdentity,
                thirdImportedIdentity
            )
            pEpProviderStubber.stubImportKey(expectedResult)
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view)


            val result = presenter.importKey(uri)


            assertForImportKey(expectedResult, result)
        }

    @Test
    fun `importKey from account settings, fail because all accounts in file are different from given one`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(
                listOf(
                    secondImportedIdentity,
                    thirdImportedIdentity
                )
            )
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view, preferences.accounts.first().uuid)


            val result = presenter.importKey(uri)



            assertForImportKey(emptyList(), result)
        }

    /**
     * If PEpProvider's importKey's result is an empty Vector, that means there was no private key in the file.
     */
    @Test
    fun `importKey from account settings, empty Vector from provider`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(listOf())
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view, preferences.accounts.first().uuid)


            val result = presenter.importKey(uri)


            assertForImportKey(emptyList(), result)
        }

    /**
     * If PEpProvider's importKey's result is an empty Vector, that means there was no private key in the file.
     */
    @Test
    fun `importKey from general settings, empty Vector from provider`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(listOf())
            val uri = stubContentResolverAndGetKeysFileUri()

            presenter.initialize(view)


            val result = presenter.importKey(uri)


            assertForImportKey(emptyList(), result)
        }

    /**
     * PEpProvider's importKey's result could be null. In that case we return an empty list.
     */
    @Test
    fun `importKey from account settings, null Vector from provider`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(null)
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view, preferences.accounts.first().uuid)


            val result = presenter.importKey(uri)


            assertForImportKey(emptyList(), result)
        }

    /**
     * PEpProvider's importKey's result could be null. In that case we return an empty list.
     */
    @Test
    fun `importKey from general settings, null Vector from provider`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(null)
            val uri = stubContentResolverAndGetKeysFileUri()


            presenter.initialize(view)


            val result = presenter.importKey(uri)


            assertForImportKey(emptyList(), result)
        }

    private fun assertForImportKey(
        expectedResult: List<Identity>,
        result: List<Identity>
    ) {
        verify(pEpProvider).importKey(any())
        TestCase.assertEquals(expectedResult.size, result.size)
        if (expectedResult.isNotEmpty()) {
            expectedResult.forEachIndexed { index, identity ->
                TestCase.assertEquals(identity, result[index])
            }
        }
    }

    private fun stubContentResolverAndGetKeysFileUri(): Uri {
        val (uri: Uri, fileInputStream) = getMultipleKeyFileInputStreamAndUri()
        stubContentResolver(fileInputStream, uri)
        return uri
    }

    private fun stubContentResolver(fileInputStream: FileInputStream, uri: Uri) {
        doReturn(resolver).`when`(contextSpy).contentResolver
        doReturn(fileInputStream).`when`(resolver).openInputStream(uri)
    }

    private fun retrieveMultipleKeyFileUriFromResources(): File {
        return File(javaClass.getResource("/keyimport/account1AsciiNoPassPhrase.asc")!!.file)
    }

    private fun getMultipleKeyFileInputStreamAndUri(): Pair<Uri, FileInputStream> {
        val resourceFile = retrieveMultipleKeyFileUriFromResources()
        val uri: Uri = Uri.fromFile(resourceFile)
        val fileInputStream = FileInputStream(resourceFile)
        return Pair(uri, fileInputStream)
    }


    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FakeAndroidKeyStore.setup()
        }
    }
}