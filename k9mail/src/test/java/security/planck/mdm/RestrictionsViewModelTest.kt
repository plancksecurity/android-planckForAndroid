package security.planck.mdm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.provisioning.AccountProvisioningSettings
import security.planck.provisioning.ProvisioningSettings

@ExperimentalCoroutinesApi
class RestrictionsViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val testRestrictionsUpdatedFlow = MutableStateFlow(0)
    private val testAccountRemovedFlow = MutableStateFlow(false)
    private val testWrongAccountSettingsFlow = MutableStateFlow(false)
    private val configurationManager: ConfigurationManager = mockk {
        every { resetWrongAccountSettingsWarning() }.just(runs)
        every { restrictionsUpdatedFlow }.returns(testRestrictionsUpdatedFlow)
        every { accountRemovedFlow }.returns(testAccountRemovedFlow)
        every { wrongAccountSettingsFlow }.returns(testWrongAccountSettingsFlow)
    }
    private lateinit var viewModel: RestrictionsViewModel
    private val observedRestrictionsUpdated = mutableListOf<Boolean>()
    private val observedAccountRemoved = mutableListOf<Boolean>()
    private val observedWrongSettings = mutableListOf<Boolean>()
    private val observedNextAccountToInstall = mutableListOf<AccountProvisioningSettings?>()
    private val provisioningSettings: ProvisioningSettings = mockk()

    @Before
    fun setUp() {
        observedRestrictionsUpdated.clear()
        observedNextAccountToInstall.clear()
        observedAccountRemoved.clear()
        observedWrongSettings.clear()
        every { provisioningSettings.findNextAccountToInstall() }.returns(
            null
        )
        viewModel = RestrictionsViewModel(configurationManager, provisioningSettings)
        observeViewModel()
    }

    @Test
    fun `initially restrictionsUpdated LiveData is false`() {
        assertRestrictionsUpdates(false)
    }

    @Test
    fun `on restrictions updates the restrictionsUpdated LiveData is updated`() = runTest {
        testRestrictionsUpdatedFlow.value = 1
        advanceUntilIdle()

        assertRestrictionsUpdates(false, true)
    }

    @Test
    fun `on restrictions updates the nextAccountToInstall LiveData is updated`() = runTest {
        every { provisioningSettings.findNextAccountToInstall() }.returns(
            AccountProvisioningSettings("email")
        )
        testRestrictionsUpdatedFlow.value = 1
        advanceUntilIdle()


        assertNextAccountToInstall(null, AccountProvisioningSettings("email"))
    }

    @Test
    fun `accountRemoved LiveData is updated when accountRemoved flow is updated`() = runTest {
        testAccountRemovedFlow.value = true
        advanceUntilIdle()

        assertAccountRemoved(false, true)
    }

    @Test
    fun `wrongAccountSettings LiveData is updated when wrongAccountSettings flow is updated`() =
        runTest {
            testWrongAccountSettingsFlow.value = true
            advanceUntilIdle()

            assertWrongAccountSettings(false, true)
        }

    @Test
    fun `resetWrongAccountSettingsWarning() calls same method in ConfigurationManager`() = runTest {
        viewModel.resetWrongAccountSettingsWarning()

        verify { configurationManager.resetWrongAccountSettingsWarning() }
    }

    private fun assertRestrictionsUpdates(vararg states: Boolean) {
        TestCase.assertEquals(states.toList(), observedRestrictionsUpdated)
    }

    private fun assertNextAccountToInstall(vararg states: AccountProvisioningSettings?) {
        TestCase.assertEquals(states.toList(), observedNextAccountToInstall)
    }

    private fun assertAccountRemoved(vararg states: Boolean) {
        TestCase.assertEquals(states.toList(), observedAccountRemoved)
    }

    private fun assertWrongAccountSettings(vararg states: Boolean) {
        TestCase.assertEquals(states.toList(), observedWrongSettings)
    }

    private fun observeViewModel() {
        viewModel.restrictionsUpdated.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedRestrictionsUpdated.add(it) }
        }
        viewModel.nextAccountToInstall.observeForever {
            observedNextAccountToInstall.add(it)
        }
        viewModel.accountRemoved.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedAccountRemoved.add(it) }
        }
        viewModel.wrongAccountSettings.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedWrongSettings.add(it) }
        }
    }
}