package security.pEp.ui.support.export

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class ExportpEpSupportDataPresenterTest {
    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val context: Context = mockk()
    private val lifecycle: Lifecycle = mockk(relaxed = true)
    private val view: ExportpEpSupportDataView = mockk(relaxed = true)
    private val databaseExporter: PEpSupportDataExporter = mockk()
    private val presenter: ExportpEpSupportDataPresenter =
        ExportpEpSupportDataPresenter(
            context,
            databaseExporter,
        )

    @Before
    fun setup() {
        every { lifecycle.currentState }.returns(Lifecycle.State.STARTED)
        every { context.getExternalFilesDir(any()) }.returns(File("$DOCUMENTS_FOLDER/pEp/db-export/Date"))
        every { context.getDir("home", Context.MODE_PRIVATE) }.returns(File(PEP_HOME_FOLDER))
        every { context.getDir("trustwords", Context.MODE_PRIVATE) }.returns(File(PEP_TRUSTWORDS_FOLDER))
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
    fun `when presenter renders step Exporting, view shows loading screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.Exporting)

        verify { view.showLoading() }
    }

    @Test
    fun `when presenter renders step Failed, view shows failed screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.Failed())

        verify { view.showFailed() }
    }

    @Test
    fun `when presenter renders step Success, view shows successful screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.Success)

        verify { view.showSuccess() }
    }

    @Test
    fun `when presenter renders step and current lifecycle state is NOT at least STARTED, view does nothing`() {
        every { lifecycle.currentState }.returns(Lifecycle.State.CREATED)

        presenter.renderStep(ExportPEpDatabasesStep.Failed())

        verify { view.wasNot(called) }
    }

    @Test
    fun `presenter_export() uses PEpDatabaseExporter to export files`() = runBlocking {
        coEvery { databaseExporter.export(any(), any()) }.returns(Result.success(true))


        presenter.export()


        val fromFoldersSlot = slot<List<File>>()
        val toFolderSlot = slot<File>()

        coVerify { databaseExporter.export(capture(fromFoldersSlot), capture(toFolderSlot)) }


        val fromPaths = fromFoldersSlot.captured.map { it.absolutePath }
        val toPath = toFolderSlot.captured.absolutePath

        assertTrue(toPath.contains("$DOCUMENTS_FOLDER/pEp/db-export/"))
        assertEquals(2, fromPaths.size)
        assertTrue(fromPaths.first().contains("$PEP_HOME_FOLDER/.pEp"))
        assertTrue(fromPaths[1].contains(PEP_TRUSTWORDS_FOLDER))
    }

    @Test
    fun `when export is successful, view shows successful screen`() = runBlocking {
        coEvery { databaseExporter.export(any(), any()) }.returns(Result.success(true))


        presenter.export()


        coVerify { view.hideLoading() }
        coVerify { view.showSuccess() }
    }

    @Test
    fun `when export fails, view shows failed screen`() = runBlocking {
        coEvery { databaseExporter.export(any(), any()) }.returns(Result.success(false))


        presenter.export()


        coVerify { view.hideLoading() }
        coVerify { view.showFailed() }
    }

    @Test
    fun `when there is not enough space left in device, view shows it on screen`() = runBlocking {
        coEvery { databaseExporter.export(any(), any()) }
            .returns(Result.failure(NotEnoughSpaceInDeviceException(0, 0)))


        presenter.export()



        coVerify { view.hideLoading() }
        coVerify { view.showNotEnoughSpaceInDevice(0, 0) }
    }

    companion object {
        private const val DOCUMENTS_FOLDER = "Documents"
        private const val PEP_HOME_FOLDER = "pEpHome"
        private const val PEP_TRUSTWORDS_FOLDER = "trustwords"
    }
}