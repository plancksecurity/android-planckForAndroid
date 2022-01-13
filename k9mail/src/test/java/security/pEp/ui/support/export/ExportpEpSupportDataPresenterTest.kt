package security.pEp.ui.support.export

import android.content.Context
import android.os.Environment
import androidx.lifecycle.Lifecycle
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.nhaarman.mockito_kotlin.*
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

    private val context: Context = mock()
    private val lifecycle: Lifecycle = mock()
    private val view: ExportpEpSupportDataView = mock()
    private val databaseExporter: PEpSupportDataExporter = mock()
    private val presenter: ExportpEpSupportDataPresenter =
        ExportpEpSupportDataPresenter(
            context,
            databaseExporter,
        )

    @Before
    fun setup() {
        stubLifecycle()
        presenter.initialize(
            view,
            lifecycle,
        )
    }

    @Test
    fun `when cancel button is clicked, view finishes`() {
        presenter.cancel()

        verify(view).finish()
    }

    @Test
    fun `when presenter renders step EXPORTING, view shows loading screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.EXPORTING)

        verify(view).showLoading()
    }

    @Test
    fun `when presenter renders step FAILED, view shows failed screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.FAILED)

        verify(view).showFailed()
    }

    @Test
    fun `when presenter renders step SUCCESS, view shows successful screen`() {
        presenter.renderStep(ExportPEpDatabasesStep.SUCCESS)

        verify(view).showSuccess()
    }

    @Test
    fun `when presenter renders step and current lifecycle state is NOT at least STARTED, view does nothing`() {
        doReturn(Lifecycle.State.CREATED).`when`(lifecycle).currentState

        presenter.renderStep(ExportPEpDatabasesStep.FAILED)

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `presenter_export() uses PEpDatabaseExporter to export files`() = runBlocking {
        doReturn(true).`when`(databaseExporter).export(any(), any())
        doReturn(File(DOCUMENTS_FOLDER)).`when`(context)
            .getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        doReturn(File(PEP_HOME_FOLDER)).`when`(context).getDir("home", Context.MODE_PRIVATE)


        presenter.export()


        val fromFolderCaptor = argumentCaptor<File>()
        val toFolderCaptor = argumentCaptor<File>()

        verify(databaseExporter).export(fromFolderCaptor.capture(), toFolderCaptor.capture())

        val fromPath = fromFolderCaptor.firstValue.absolutePath
        val toPath = toFolderCaptor.firstValue.absolutePath

        assertTrue(toPath.contains("$DOCUMENTS_FOLDER/pEp/db-export/"))
        assertTrue(fromPath.contains("$PEP_HOME_FOLDER/.pEp"))
    }

    @Test
    fun `when export is successful, view shows successful screen`() = runBlocking {
        doReturn(true).`when`(databaseExporter).export(any(), any())


        presenter.export()


        verify(view).hideLoading()
        verify(view).showSuccess()
    }

    @Test
    fun `when export fails, view shows failed screen`() = runBlocking {
        doReturn(false).`when`(databaseExporter).export(any(), any())


        presenter.export()


        verify(view).hideLoading()
        verify(view).showFailed()
    }

    private fun stubLifecycle() {
        doReturn(Lifecycle.State.STARTED).`when`(lifecycle).currentState
    }

    companion object {
        private const val DOCUMENTS_FOLDER = "Documents"
        private const val PEP_HOME_FOLDER = "AppInternal"
    }
}
