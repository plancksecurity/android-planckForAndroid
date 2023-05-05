package security.planck.ui.support.export


import android.content.Context
import android.net.Uri
import com.fsck.k9.RobolectricTest
import com.fsck.k9.pEp.saveToDocuments
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import security.planck.file.PEpSystemFileLocator
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@ExperimentalCoroutinesApi
class ExportpEpSupportDataTestApi30 : RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()
    private val context: Context = mockk()
    private val systemFileLocator: PEpSystemFileLocator = mockk()
    private val exportpEpSupportData = ExportpEpSupportData(context, systemFileLocator)
    private val homeFolder = File("src/test/java/security/pEp/ui/support/export/homeFolder")
    private val pEpFolder = File(homeFolder, ".pEp")
    private val trustwordsFolder =
        File("src/test/java/security/pEp/ui/support/export/trustwordsFolder")
    private val fromFolders = listOf(pEpFolder, trustwordsFolder)
    private val destinationBaseFolder = File("src/test/java/security/pEp/ui/support/export")
    private val destinationSubFolder = "toFolder"
    private val toFolder = File(destinationBaseFolder, destinationSubFolder)

    @Before
    fun setUp() {
        mockkStatic(FileUtils::class)
        mockkStatic("com.fsck.k9.pEp.MediaStoreUtilsKt")
        coEvery { systemFileLocator.trustwordsFolder }.returns(trustwordsFolder)
        coEvery { systemFileLocator.pEpFolder }.returns(pEpFolder)
        cleanupFiles()
        fromFolders.forEach { it.mkdirs() }
        toFolder.mkdirs()
    }

    @Test
    fun `export() uses MediaStoreUtils to copy files`() {
        runBlocking {
            val managementDb = File(pEpFolder, "management.db").also { it.writeText("text") }
            val keystDb = File(pEpFolder, "keys.db").also { it.writeText("text") }
            val systemDb = File(trustwordsFolder, "system.db").also { it.writeText("text") }
            File(toFolder, "management.db").writeText("test")
            File(toFolder, "keys.db").writeText("test")
            File(toFolder, "system.db").writeText("test")

            coEvery {
                any<InputStream>().saveToDocuments(
                    any(),
                    any(),
                    any(),
                    any()
                )
            }.returns(Uri.EMPTY)

            val managementDbIs: FileInputStream = mockk<FileInputStream>().also {
                coEvery { FileUtils.openInputStream(managementDb) }.returns(it)
            }
            val keysDbIs: FileInputStream = mockk<FileInputStream>().also {
                coEvery { FileUtils.openInputStream(keystDb) }.returns(it)
            }
            val systemDbIs: FileInputStream = mockk<FileInputStream>().also {
                coEvery { FileUtils.openInputStream(systemDb) }.returns(it)
            }


            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


            verify { managementDbIs.saveToDocuments(context, "", "management.db", "toFolder") }
            verify { keysDbIs.saveToDocuments(context, "", "keys.db", "toFolder") }
            verify { systemDbIs.saveToDocuments(context, "", "system.db", "toFolder") }
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `export() returns Result_failure if an exception is thrown`() {
        runBlocking {
            File(pEpFolder, "management.db").writeText("text")
            File(pEpFolder, "keys.db").writeText("text")
            File(trustwordsFolder, "system.db").writeText("text")
            File(toFolder, "management.db").writeText("test")
            File(toFolder, "keys.db").writeText("test")
            File(toFolder, "system.db").writeText("test")

            coEvery { any<InputStream>().saveToDocuments(any(), any(), any(), any()) }.throws(
                RuntimeException("test")
            )
            coEvery { FileUtils.openInputStream(any()) }.returns(mockk())


            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is RuntimeException)
            assertEquals("ERROR: Could not export pEp data", result.exceptionOrNull()!!.message)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic("com.fsck.k9.pEp.MediaStoreUtilsKt")
        unmockkStatic(FileUtils::class)
        cleanupFiles()
    }

    private fun cleanupFiles() {
        fromFolders.forEach { it.deleteRecursively() }
        toFolder.deleteRecursively()
    }
}