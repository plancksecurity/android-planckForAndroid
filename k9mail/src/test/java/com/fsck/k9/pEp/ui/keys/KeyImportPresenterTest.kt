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
import com.fsck.k9.pEp.testutils.ReturnBehavior
import com.nhaarman.mockito_kotlin.*
import foundation.pEp.jniadapter.CommType
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.exceptions.pEpException
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
import java.io.IOException


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

    @Test
    fun `importKey returns empty list and closes PEpProvider on IOException`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            runImportKeyThrowingAndVerify(IOException("test IOException"))
        }

    @Test
    fun `importKey returns empty list and closes PEpProvider on pEpException`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            runImportKeyThrowingAndVerify(pEpException("test pEpException"))
        }

    private suspend fun runImportKeyThrowingAndVerify(e: Throwable) {
        // making PEpProvider.importKey throw any exception we need is convenient here
        pEpProviderStubber.stubImportKeyThrowing(e)
        val uri = stubContentResolverAndGetKeysFileUri()

        presenter.initialize(view)


        val result = presenter.importKey(uri)


        verify(pEpProvider).importKey(any())
        TestCase.assertEquals(0, result.size)
        verify(pEpProvider).close()
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

    @Test
    fun `onKeyImport calls view according when importKey fails`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(null)
            val uri = stubContentResolverAndGetKeysFileUri()
            val filename = uri.path.toString()

            presenter.initialize(view, preferences.accounts.first().uuid)


            presenter.onKeyImport(uri)


            verify(view).hideLoading()
            verify(view).showFailedKeyImport(filename)
        }

    @Test
    fun `onKeyImport calls view according when importKey succeeds`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            pEpProviderStubber.stubImportKey(listOf(firstImportedIdentity))
            val uri = stubContentResolverAndGetKeysFileUri()
            val filename = uri.path.toString()

            presenter.initialize(view, preferences.accounts.first().uuid)



            presenter.onKeyImport(uri)



            verify(view).hideLoading()
            verify(view).showKeyImportConfirmationDialog(any(), eq(filename))
        }

    @Test
    fun `onKeyImportRejected makes view finish`() {
        presenter.initialize(view, preferences.accounts.first().uuid)

        presenter.onKeyImportRejected()

        verify(view).finish()
    }

    private suspend fun runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
        identity: Identity,
        importedIdentities: List<Identity>?,
        accountUuid: String = "",
        returnOnCanEncrypt: Boolean = true,
        returnBehavior: ReturnBehavior<Identity> =
            ReturnBehavior.Return(identity)

    ): String {
        pEpProviderStubber.stubProviderMethodsForIdentity(
            identity, importedFingerPrint, returnOnCanEncrypt, returnBehavior
        )
        pEpProviderStubber.stubImportKey(importedIdentities)
        val uri = stubContentResolverAndGetKeysFileUri()
        val filename = uri.path.toString()

        presenter.initialize(view, accountUuid)
        val importResult = presenter.importKey(uri)


        presenter.onKeyImportAccepted(importResult, filename)

        verify(pEpProvider).close()

        return filename
    }

    /**
     * Case when we open key import from account settings and are importing a key for account 1,
     * which is registered in the device.
     */
    @Test
    fun `onKeyImportAccepted from AccountSettings success with account present in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                preferences.accounts.first().uuid
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(email1)
            verify(view).showCorrectKeyImport(importedFingerPrint, filename)
        }

    /**
     * Case when we open key import from general settings and are importing a key for account 1,
     * which is registered in the device.
     */
    @Test
    fun `onKeyImportAccepted from general settings success with account present in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity)
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(email1)
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename, mapOf(email1 to true))
        }

    /**
     * Case when we open key import from general settings and we are importing a key for an email account
     * we do not have registered in our device. Case when we have accounts 1, 2 and 3 in Preferences
     * and try to import an identity for account 4.
     */
    @Test
    fun `onKeyImportAccepted from general settings success with account NOT present in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                fourthIdentity,
                listOf(fourthIdentity)
            )


            verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email4)
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename, mapOf(email4 to true))
        }

    /**
     * Case when we open key import from account settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.setOwnIdentity] returns null.
     */
    @Test
    fun `onKeyImportAccepted from AccountSettings fail at setOwnIdentity`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                preferences.accounts.first().uuid,
                true,
                ReturnBehavior.Return(null)
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                setOwnIdentitySuccess = false
            )
            verify(view).showFailedKeyImport(filename)
        }

    /**
     * Case when we open key import from account settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.setOwnIdentity] throws exception.
     */
    @Test
    fun `onKeyImportAccepted from AccountSettings throw at setOwnIdentity`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                preferences.accounts.first().uuid,
                true,
                ReturnBehavior.Throw(pEpException("test exception"))
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                setOwnIdentitySuccess = false
            )
            verify(view).showFailedKeyImport(filename)
        }

    /**
     * Case when we open key import from account settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.canEncrypt] returns false.
     */
    @Test
    fun `onKeyImportAccepted from AccountSettings fail at canEncrypt`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                preferences.accounts.first().uuid,
                false
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                canEncryptSuccess = false
            )
            verify(view).showFailedKeyImport(filename)
        }

    /**
     * Case when we open key import from general settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.setOwnIdentity] returns null.
     */
    @Test
    fun `onKeyImportAccepted from general settings fail at setOwnIdentity with account in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                "",
                true,
                ReturnBehavior.Return(null)
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                setOwnIdentitySuccess = false
            )
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename)
        }

    /**
     * Case when we open key import from general settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.setOwnIdentity] throws exception.
     */
    @Test
    fun `onKeyImportAccepted from general settings throw at setOwnIdentity with account in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                "",
                true,
                ReturnBehavior.Throw(pEpException("test exception"))
            )


            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                setOwnIdentitySuccess = false
            )
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename)
        }

    /**
     * Case when we open key import from account settings and are importing a key for account 1,
     * which is registered in the device. Case when [PEpProvider.canEncrypt] returns false.
     */
    @Test
    fun `onKeyImportAccepted from general settings fail at canEncrypt with account in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                firstIdentity,
                listOf(firstImportedIdentity),
                "",
                false
            )



            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1,
                canEncryptSuccess = false
            )
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename)
        }

    /**
     * Case when we open key import from general settings and we are importing a key for an email account
     * we do not have registered in our device. Case when we have accounts 1, 2 and 3 in Preferences
     * and try to import an identity for account 4. Case when [PEpProvider.setOwnIdentity] fails.
     */
    @Test
    fun `onKeyImportAccepted from general settings fail at setOwnIdentity with account NOT in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                fourthIdentity,
                listOf(fourthIdentity),
                "",
                true,
                ReturnBehavior.Return(null)
            )



            verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email4)
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename)
        }

    /**
     * Case when we open key import from general settings and we are importing a key for an email account
     * we do not have registered in our device. Case when we have accounts 1, 2 and 3 in Preferences
     * and try to import an identity for account 4. Case when [PEpProvider.setOwnIdentity] throws exception.
     */
    @Test
    fun `onKeyImportAccepted from general settings throw at setOwnIdentity with account NOT in device`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val filename = runOnKeyImportAcceptedForSingleAccountAndReturnFileName(
                fourthIdentity,
                listOf(fourthIdentity),
                "",
                true,
                ReturnBehavior.Throw(pEpException("test exception"))
            )



            verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email4)
            verifyOnKeyImportConfirmedOutputFromGeneralSettings(filename)
        }

    /**
     * Case when we open key import from general settings and we are importing a key for several accounts, some of them are
     * setup on the device and some others are not. Case when we have accounts 1, 2 and 3 in Preferences, and try to import
     * a key for accounts 1, 2, 3, 4, 5.
     * Case when PEpProvider:
     * returns null on [PEpProvider.setOwnIdentity] when importing key for Account 1
     * returns false on [PEpProvider.canEncrypt] when importing key for Account 2
     * goes ok when importing key for Account 3
     * throws [pEpException] on [PEpProvider.setOwnIdentity] when importing key for Account 4
     * goes ok when importing key for Account 5
     */
    @Test
    fun `onKeyImportAccepted from general settings successs and fail mixed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {

            val testCases = listOf(
                OnKeyImportAcceptedTestCaseIdentityData(
                    firstIdentity, true,
                    ReturnBehavior.Return(null)
                ),
                OnKeyImportAcceptedTestCaseIdentityData(
                    secondIdentity, false
                ),
                OnKeyImportAcceptedTestCaseIdentityData(
                    thirdIdentity
                ),
                OnKeyImportAcceptedTestCaseIdentityData(
                    fourthIdentity, true,
                    ReturnBehavior.Throw(pEpException("test exception"))
                ),
                OnKeyImportAcceptedTestCaseIdentityData(
                    fifthIdentity
                )
            )

            val importedIdentities = listOf(
                firstImportedIdentity,
                secondImportedIdentity,
                thirdImportedIdentity,
                fourthIdentity,
                fifthIdentity
            )


            val filename = runOnKeyImportAcceptedForSeveralAccountsAndReturnFileName(
                testCases, importedIdentities
            )


            // Account 1: PEpProvider.setOwnIdentity returns null
            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email1, setOwnIdentitySuccess = false
            )
            // Account 2: PEpProvider.canEncrypt returns false
            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
                email2, canEncryptSuccess = false
            )
            // Account 3: ok
            verifyOnKeyImportConfirmedResultForOneAccountOnDevice(email3)
            // Account 4: PEpProvider.setOwnIdentity throws pEpException
            verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email4)
            // Account 5: ok
            verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email5)

            verifyOnKeyImportConfirmedOutputFromGeneralSettings(
                filename,
                mapOf(
                    email1 to false,
                    email2 to false,
                    email3 to true,
                    email4 to false,
                    email5 to true
                )
            )
        }

    private fun verifyOnKeyImportConfirmedOutputFromGeneralSettings(
        filename: String,
        expectedMap: Map<String, Boolean> = mapOf()
    ) {
        if (expectedMap.count { it.value } == 0) {
            verify(view).showFailedKeyImport(filename)
        } else {
            verifyOnKeyImportConfirmedOutputMap(filename, expectedMap)
        }
    }

    private fun verifyOnKeyImportConfirmedOutputMap(
        filename: String,
        expectedMap: Map<String, Boolean>
    ) {
        val mapArgumentCaptor = argumentCaptor<Map<Identity, Boolean>>()
        verify(view).showKeyImportResult(mapArgumentCaptor.capture(), eq(filename))
        val resultMap = mapArgumentCaptor.firstValue
        TestCase.assertEquals(expectedMap.size, resultMap.entries.size)
        val resultIdentities = resultMap.keys.toList()
        val resultBooleans = resultMap.values.toList()
        val expectedEmails = expectedMap.keys.toList()
        val expectedBooleans = expectedMap.values.toList()

        resultIdentities.forEachIndexed { index, identity ->
            TestCase.assertEquals(expectedEmails[index], identity.address)
            TestCase.assertEquals(expectedBooleans[index], resultBooleans[index])
        }
    }

    private fun verifyOnKeyImportConfirmed_SUCCESS_ForSingleAccountOnDevice(email: String) {
        verify(pEpProvider, never()).setOwnIdentity(
            identityThat { it.address == email },
            eq(originalFingerPrint)
        )
        verify(pEpProvider, times(2)).myself(identityThat { it.address == email })
    }

    private fun verifyOnKeyImportConfirmed_FAILURE_ForSingleAccountOnDevice(email: String) {
        verify(pEpProvider).setOwnIdentity(
            identityThat { it.address == email },
            eq(originalFingerPrint)
        )
        verify(pEpProvider).myself(identityThat { it.address == email })
    }

    private fun verifyOnKeyImportConfirmedForOneAccountNOTOnDevice(email: String) {
        verify(pEpProvider).setOwnIdentity(
            identityThat { it.address == email },
            eq(importedFingerPrint)
        )
        verify(pEpProvider).myself(identityThat { it.address == email })
    }

    private fun verifyOnKeyImportConfirmedResultForOneAccountOnDevice(
        email: String,
        canEncryptSuccess: Boolean = true,
        setOwnIdentitySuccess: Boolean = true
    ) {
        verify(pEpProvider).setOwnIdentity(
            identityThat { it.address == email },
            eq(importedFingerPrint)
        )
        if (setOwnIdentitySuccess) {
            verify(pEpProvider).canEncrypt(email)
            if (canEncryptSuccess) {
                verifyOnKeyImportConfirmed_SUCCESS_ForSingleAccountOnDevice(email)
            } else {
                verifyOnKeyImportConfirmed_FAILURE_ForSingleAccountOnDevice(email)
            }
        } else {
            verify(pEpProvider, never()).canEncrypt(email)
            verifyOnKeyImportConfirmed_FAILURE_ForSingleAccountOnDevice(email)
        }
    }

    class OnKeyImportAcceptedTestCaseIdentityData(
        val identity: Identity,
        val returnOnCanEncrypt: Boolean = true,
        val returnBehavior: ReturnBehavior<Identity> =
            ReturnBehavior.Return(identity)
    )

    private suspend fun runOnKeyImportAcceptedForSeveralAccountsAndReturnFileName(
        testCaseList: List<OnKeyImportAcceptedTestCaseIdentityData>,
        importedIdentities: List<Identity>?,
        accountUuid: String = ""
    ): String {
        testCaseList.forEach {
            pEpProviderStubber.stubProviderMethodsForIdentity(
                it.identity, importedFingerPrint, it.returnOnCanEncrypt, it.returnBehavior
            )
        }
        pEpProviderStubber.stubImportKey(importedIdentities)
        val uri = stubContentResolverAndGetKeysFileUri()
        val filename = uri.path.toString()

        presenter.initialize(view, accountUuid)
        val importResult = presenter.importKey(uri)


        presenter.onKeyImportAccepted(importResult, filename)

        verify(pEpProvider).close()

        return filename
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