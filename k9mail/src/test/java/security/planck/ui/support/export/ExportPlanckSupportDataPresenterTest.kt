package security.planck.ui.support.export

import androidx.lifecycle.Lifecycle
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.planck.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class ExportPlanckSupportDataPresenterTest : RobolectricTest() {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val lifecycle: Lifecycle = mockk(relaxed = true)
    private val view: ExportpEpSupportDataView = mockk(relaxed = true)
    private val exportPlanckSupportData: ExportPlanckSupportData = mockk()
    private val presenter: ExportPlanckSupportDataPresenter =
        ExportPlanckSupportDataPresenter(
            exportPlanckSupportData,
        )

    @Before
    fun setup() {
        every { lifecycle.currentState }.returns(Lifecycle.State.STARTED)
        presenter.initialize(
            view,
            lifecycle,
        )
    }

    @Test
    fun `when cancel button is clicked, view finishes`() {
        presenter.cancel()

        verify { view.finish() }
    }

    @Test
    fun `when presenter renders state Exporting, view shows loading screen`() {
        presenter.renderState(ExportpEpDataState.Exporting)

        verify { view.showLoading() }
    }

    @Test
    fun `when presenter renders state Failed, view shows failed screen`() {
        presenter.renderState(ExportpEpDataState.Failed(RuntimeException()))

        verify { view.showFailed() }
    }

    @Test
    fun `when presenter renders state Succeeded, view shows successful screen`() {
        presenter.renderState(ExportpEpDataState.Succeeded)

        verify { view.showSuccess() }
    }

    @Test
    fun `when presenter renders state and current lifecycle state is NOT at least STARTED, view does nothing`() {
        every { lifecycle.currentState }.returns(Lifecycle.State.CREATED)

        presenter.renderState(ExportpEpDataState.Failed(RuntimeException()))

        verify { view.wasNot(called) }
    }

    @Test
    fun `presenter_export() uses PEpDatabaseExporter to export files`() = runTest {
        coEvery { exportPlanckSupportData(any(), any()) }.returns(Result.success(Unit))


        presenter.export()
        advanceUntilIdle()


        val baseFolderSlot = slot<File>()
        val subFolderSlot = slot<String>()

        coVerify { exportPlanckSupportData(capture(baseFolderSlot), capture(subFolderSlot)) }


        val baseFolder = baseFolderSlot.captured
        val subFolder = subFolderSlot.captured
        val toPath = File(baseFolder, subFolder).absolutePath

        assertTrue(toPath.contains("$DOCUMENTS_FOLDER/pEp/db-export/"))
    }

    @Test
    fun `when export is successful, view shows successful screen`() = runTest {
        coEvery { exportPlanckSupportData(any(), any()) }.returns(Result.success(Unit))


        presenter.export()
        advanceUntilIdle()


        coVerify { view.hideLoading() }
        coVerify { view.showSuccess() }
    }

    @Test
    fun `when export fails, view shows failed screen`() = runTest {
        coEvery { exportPlanckSupportData(any(), any()) }.returns(Result.failure(CouldNotExportPEpDataException()))


        presenter.export()
        advanceUntilIdle()


        coVerify { view.hideLoading() }
        coVerify { view.showFailed() }
    }

    @Test
    fun `when there is not enough space left in device, view shows it on screen`() = runTest {
        coEvery { exportPlanckSupportData(any(), any()) }
            .returns(Result.failure(NotEnoughSpaceInDeviceException(0, 0)))


        presenter.export()
        advanceUntilIdle()


        coVerify { view.hideLoading() }
        coVerify { view.showNotEnoughSpaceInDevice() }
    }

    companion object {
        private const val DOCUMENTS_FOLDER = "Documents"
        private const val PEP_HOME_FOLDER = "pEpHome"
        private const val PEP_TRUSTWORDS_FOLDER = "trustwords"
    }
}