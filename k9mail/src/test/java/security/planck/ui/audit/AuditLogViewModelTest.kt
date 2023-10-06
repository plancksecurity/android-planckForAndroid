package security.planck.ui.audit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.audit.AuditLogger

@ExperimentalCoroutinesApi
class AuditLogViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private lateinit var viewModel: AuditLogViewModel
    private val observedStates = mutableListOf<Boolean>()
    private val testFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        observedStates.clear()
        every { auditLogger.tamperAlertFlow }.returns(testFlow)
        viewModel = AuditLogViewModel(auditLogger)
        observeViewModel()
    }

    @Test
    fun `initially ViewModel has waning set to false by default`() {
        assertWarningStates(false)
    }

    @Test
    fun `changes in the AuditLogger flow trigger LiveData warning`() = runTest {
        testFlow.value = true

        assertWarningStates(false)
    }

    @Test
    fun `auditTamperingCloseApp() resets tamper alert, enables persistent warning and calls lambda`() =
        runTest {
            val lambda: () -> Unit = mockk()
            viewModel.auditTamperingCloseApp(lambda)
            advanceUntilIdle()


            coVerify {
                auditLogger.resetTamperAlert()
                auditLogger.enablePersistentWarningOnStartup()
                lambda.invoke()
            }
        }

    @Test
    fun `auditTamperingAlertDismissed() resets tamper alert, enables persistent warning and calls lambda`() =
        runTest {
            viewModel.auditTamperingAlertDismissed()
            advanceUntilIdle()


            coVerify {
                auditLogger.resetTamperAlert()
                auditLogger.disablePersistentWarningOnStartup()
            }
        }

    private fun assertWarningStates(vararg states: Boolean) {
        TestCase.assertEquals(states.toList(), observedStates)
    }

    private fun observeViewModel() {
        viewModel.tamperAlert.observeForever { event ->
            event.getContentIfNotHandled()?.let { observedStates.add(it) }
        }
    }

}