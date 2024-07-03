package security.planck.ui.setup

import androidx.lifecycle.LiveData
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import security.planck.mdm.ConfigurationManager
import security.planck.passphrase.PassphraseFormatValidator
import security.planck.provisioning.ProvisioningScope
import security.planck.ui.passphrase.models.AccountTextFieldState
import security.planck.ui.passphrase.models.PassphraseState
import security.planck.ui.passphrase.models.PassphraseVerificationStatus
import security.planck.ui.passphrase.models.TextFieldState
import security.planck.ui.passphrase.models.TextFieldStateContract
import java.util.Vector

private const val EMAIL = "test@mail.ch"
private const val UUID = "ACCOUNT-UUID"
private const val TEST_PASSPHRASE = "HelloPassphrase!!1"

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAccountKeysViewModelTest : LiveDataTest<PassphraseState>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    override val testLivedata: LiveData<PassphraseState>
        get() = viewModel.state

    private val account: Account = mockk {
        every { email }.returns(EMAIL)
        every { uuid }.returns(UUID)
        every { setOptionsOnInstall() }.just(runs)
        every { save(any()) }.just(runs)
        every { setupState = any() }.just(runs)
    }
    private val preferences: Preferences = mockk {
        every { getAccountAllowingIncomplete(any()) }.returns(account)
    }
    private val planckProvider: PlanckProvider = mockk {
        coEvery { unlockKeysWithPassphrase(any()) }.returns(Result.success(Vector()))
        coEvery { configPassphraseForNewKeys(any(), any(), any()) }.returns(Result.success(Unit))
    }
    private val k9: K9 = mockk {
        every { isRunningOnWorkProfile }.returns(false)
    }
    private val uiCache: PlanckUIArtefactCache = mockk {
        every { removeCredentialsInPreferences() }.just(runs)
    }
    private val controller: MessagingController = mockk {
        every { refreshRemoteSynchronous(any()) }.just(runs)
    }
    private val configManager: ConfigurationManager = mockk {
        coEvery { loadConfigurationsSuspend(any()) }.returns(Result.success(Unit))
    }

    private val passphraseValidator: PassphraseFormatValidator = PassphraseFormatValidator()
    private val viewModel = CreateAccountKeysViewModel(
        k9,
        preferences,
        planckProvider,
        controller,
        uiCache,
        configManager,
        coroutinesTestRule.testDispatcherProvider,
        passphraseValidator
    )

    override fun initialize() {
        mockkStatic(K9::class)
        mockkStatic(PlanckUtils::class)
        every { K9.isPlanckUsePassphraseForNewKeys() }.returns(true)
        every { K9.setServicesEnabled(any()) }.just(runs)
        every { PlanckUtils.pEpGenerateAccountKeys(any(), any()) }.just(runs)
    }

    @After
    fun tearDown() {
        unmockkStatic(K9::class)
        unmockkStatic(PlanckUtils::class)
    }

    @Test
    fun `initial state is Processing`() {
        assertObservedValues(PassphraseState.Processing)
    }

    @Test
    fun `initialize using passphrase for new keys, state is CreatingAccount`() {
        viewModel.initialize(UUID, false)

        verify { preferences.getAccountAllowingIncomplete(UUID) }
        verify { K9.isPlanckUsePassphraseForNewKeys() }
        assertObservedValues(
            PassphraseState.Processing,
            PassphraseState.CreatingAccount(newPasswordState = AccountTextFieldState(email = EMAIL)),
        )
    }

    @Test
    fun `when user accepts valid passphrase, passphrase is set and account keys are created`() =
        runTest {
            viewModel.initialize(UUID, false)


            viewModel.updateNewPassphrase(TEST_PASSPHRASE)
            viewModel.updateNewPassphraseVerification(TEST_PASSPHRASE)
            viewModel.createAccountKeys()
            advanceUntilIdle()


            verify { preferences.getAccountAllowingIncomplete(UUID) }
            verify { K9.isPlanckUsePassphraseForNewKeys() }
            verify { uiCache.removeCredentialsInPreferences() }
            verify { account.setupState = Account.SetupState.READY }
            verify(exactly = 0) { account.setOptionsOnInstall() }
            coVerify { planckProvider.configPassphraseForNewKeys(true, EMAIL, TEST_PASSPHRASE) }
            verify { account.save(preferences) }
            coVerify(exactly = 0) { configManager.loadConfigurationsSuspend(any()) }
            verify { controller.refreshRemoteSynchronous(account) }
            verify { PlanckUtils.pEpGenerateAccountKeys(k9, account) }
            verify { K9.setServicesEnabled(k9) }
            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.CreatingAccount(newPasswordState = AccountTextFieldState(email = EMAIL)),
                PassphraseState.CreatingAccount(
                    newPasswordState = AccountTextFieldState(
                        email = EMAIL,
                        text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    )
                ),
                PassphraseState.CreatingAccount(
                    newPasswordState = AccountTextFieldState(
                        email = EMAIL,
                        text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    ),
                    newPasswordVerificationState = TextFieldState(
                        TEST_PASSPHRASE, TextFieldStateContract.ErrorStatus.SUCCESS
                    ),
                ),
                PassphraseState.CreatingAccount(
                    newPasswordState = AccountTextFieldState(
                        email = EMAIL,
                        text = TEST_PASSPHRASE,
                        errorStatus = TextFieldStateContract.ErrorStatus.SUCCESS
                    ),
                    newPasswordVerificationState = TextFieldState(
                        TEST_PASSPHRASE, TextFieldStateContract.ErrorStatus.SUCCESS
                    ),
                    status = PassphraseVerificationStatus.SUCCESS,
                ),
                PassphraseState.Processing,
                PassphraseState.Success,
            )
        }

    @Test
    fun `initialize with no passphrase for new keys, state is Processing and finishes the process without asking for passphrases`() =
        runTest {
            every { K9.isPlanckUsePassphraseForNewKeys() }.returns(false)


            viewModel.initialize(UUID, false)
            advanceUntilIdle()

            verify { preferences.getAccountAllowingIncomplete(UUID) }
            verify { K9.isPlanckUsePassphraseForNewKeys() }
            verify { uiCache.removeCredentialsInPreferences() }
            verify { account.setupState = Account.SetupState.READY }
            verify(exactly = 0) { account.setOptionsOnInstall() }
            verify { account.save(preferences) }
            coVerify(exactly = 0) { configManager.loadConfigurationsSuspend(any()) }
            verify { controller.refreshRemoteSynchronous(account) }
            verify { PlanckUtils.pEpGenerateAccountKeys(k9, account) }
            verify { K9.setServicesEnabled(k9) }
            assertObservedValues(
                PassphraseState.Processing,
                PassphraseState.Processing,
                PassphraseState.Success,
            )
        }

    @Test
    fun `initialize on manual setup sets account options`() = runTest {
        every { K9.isPlanckUsePassphraseForNewKeys() }.returns(false)


        viewModel.initialize(UUID, true)
        advanceUntilIdle()

        verify { account.setOptionsOnInstall() }
    }

    @Test
    fun `initialize running on work profile loads configuration instead of directly saving account`() =
        runTest {
            every { K9.isPlanckUsePassphraseForNewKeys() }.returns(false)
            every { k9.isRunningOnWorkProfile }.returns(true)


            viewModel.initialize(UUID, false)
            advanceUntilIdle()

            coVerify {
                configManager.loadConfigurationsSuspend(
                    ProvisioningScope.SingleAccountSettings(
                        EMAIL
                    )
                )
            }
            verify(exactly = 0) { account.save(preferences) }
        }

    @Test
    fun `on any error, state becomes CoreError`() = runTest {
        every { K9.isPlanckUsePassphraseForNewKeys() }.returns(false)
        every { PlanckUtils.pEpGenerateAccountKeys(any(), any()) }.throws(TestException())


        viewModel.initialize(UUID, false)
        advanceUntilIdle()

        assertObservedValues(
            PassphraseState.Processing,
            PassphraseState.Processing,
            PassphraseState.CoreError(TestException()),
        )
    }


    private data class TestException(override val message: String = "test") : Throwable()
}