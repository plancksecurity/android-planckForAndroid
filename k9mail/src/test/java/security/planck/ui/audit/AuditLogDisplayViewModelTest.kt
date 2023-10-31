package security.planck.ui.audit

import android.app.Application
import androidx.lifecycle.LiveData
import com.fsck.k9.planck.infrastructure.ListState
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import security.planck.common.LiveDataTest
import java.io.File

@ExperimentalCoroutinesApi
class AuditLogDisplayViewModelTest : LiveDataTest<ListState<String>>() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val app: Application = mockk()
    private val file: File = spyk(File("messageAudit.csv"))

    private lateinit var viewModel: AuditLogDisplayViewModel

    override val testLivedata: LiveData<ListState<String>>
        get() = viewModel.auditText

    override fun liveDataTestSetup() {
        observedValues.clear()
        file.delete()
    }

    @After
    fun tearDown() {
        file.delete()
    }

    override fun initialize() {
        initializeViewModel()
        observeLiveData()
    }

    @Test
    fun `if file does not exist, viewModel sets state EmptyList`() = runTest {


        initialize()
        advanceUntilIdle()


        assertObservedValues(ListState.Loading, ListState.Loading, ListState.EmptyList)
        assertEquals("", viewModel.longestItem)
    }

    @Test
    fun `if file is blank, viewModel sets state EmptyList`() = runTest {
        file.writeText(" ")


        initialize()
        advanceUntilIdle()


        assertObservedValues(ListState.Loading, ListState.Loading, ListState.EmptyList)
        assertEquals("", viewModel.longestItem)
    }

    @Test
    fun `if file is not blank, viewModel sets state Ready with the list of lines`() = runTest {
        file.writeText("hello${NEW_LINE}there")


        initialize()
        advanceUntilIdle()


        assertObservedValues(
            ListState.Loading,
            ListState.Loading,
            ListState.Ready(listOf("hello", "there"))
        )
        assertEquals("hello", viewModel.longestItem)
    }

    @Test
    fun `if an error happens, viewModel sets state Error`() = runTest {
        every { file.exists() }.throws(RuntimeException("test"))


        initialize()
        advanceUntilIdle()


        assertFirstObservedValues(ListState.Loading, ListState.Loading)
        assertTrue(observedValues.last() is ListState.Error)
    }

    private fun initializeViewModel() {
        viewModel = AuditLogDisplayViewModel(
            app,
            coroutinesTestRule.testDispatcherProvider,
            file
        )
    }
}