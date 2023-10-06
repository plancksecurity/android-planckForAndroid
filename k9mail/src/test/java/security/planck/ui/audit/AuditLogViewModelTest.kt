package security.planck.ui.audit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fsck.k9.Clock
import com.fsck.k9.K9
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.testutils.CoroutineTestRule
import com.fsck.k9.preferences.Storage
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import security.planck.audit.PlanckAuditLogger
import java.io.File

class AuditLogViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val planckProvider: PlanckProvider = mockk()
    private val k9: K9 = mockk()
    private val clock: Clock = mockk()
    private val storage: Storage = mockk(relaxed = true)
    private val file = File("messageAudit.csv")
    private val auditLogger: PlanckAuditLogger = PlanckAuditLogger(
        planckProvider, file, storage, k9,
        clock, 31536000000L
    )
    private val viewModel = AuditLogViewModel(auditLogger)
    private val receivedTrigger = mutableListOf<Boolean>()

    @get:Rule
    var instantTaskRule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        receivedTrigger.clear()
        observeViewModel()
        every { auditLogger.tamperAlertFlow }.returns(MutableStateFlow(false).asStateFlow())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify alert reset triggered on auditTamperingAlertDismissed`() = runTest {
        viewModel.auditTamperingAlertDismissed()
        TestCase.assertEquals(true, receivedTrigger.size > 0 && !receivedTrigger.first())
    }

    @Test
    fun `verify alert enablePersistentWarningOnStartup triggered on auditTamperingCloseApp`() =
        runTest {
            viewModel.auditTamperingCloseApp()
            TestCase.assertEquals(true, receivedTrigger.size > 0 && !receivedTrigger.first())
            //TestCase.assertEquals(true, storage.persistentWarningOnStartup())
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