package security.planck.ui.audit

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import security.planck.audit.AuditLogger

//@HiltAndroidTest
class AuditLogViewModelTest {
    private val auditLogger: AuditLogger = mockk()
    private val viewModel = AuditLogViewModel(auditLogger)
    private val receivedTrigger = mutableListOf<Boolean>()

    @Before
    fun setUp() {
        observeViewModel()
    }

    @Test
    fun `verify alert reset triggered on auditTamperingAlertDismissed`() = runTest {
        viewModel.auditTamperingAlertDismissed()

    }

    @Test
    fun `verify alert enablePersistentWarningOnStartup triggered on auditTamperingCloseApp`() =
        runTest {
            viewModel.auditTamperingCloseApp()

        }

    private fun observeViewModel() {
        viewModel.tamperAlert.observeForever { value ->
            val content: Boolean? = value.getContentIfNotHandled()
            content?.let {
                receivedTrigger.add(it)
            }
        }
    }
}