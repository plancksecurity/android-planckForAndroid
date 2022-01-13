package security.pEp.ui.support.export

import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.spy
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@ExperimentalCoroutinesApi
class PEpSupportDataExporterTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    private val exporter = PEpSupportDataExporter()
    private val fromFolder = File("src/test/java/security/pEp/ui/support/export/fromFolder")
    private val toFolder = File("src/test/java/security/pEp/ui/support/export/toFolder")

    @Before
    fun setUp() {
        cleanupFiles()
    }

    @Test
    fun `export() copies files from origin to target directory`() {
        runBlocking {
            fromFolder.mkdirs()
            File(fromFolder, "management.db").writeText("test")
            File(fromFolder, "keys.db").writeText("test")


            val result = exporter.export(fromFolder, toFolder)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            assertTrue(expectedManagementDb.exists())
            assertTrue(expectedKeysDb.exists())
            assertTrue(result)
        }
    }

    @Test
    fun `export() returns false if file copy fails`() {
        runBlocking {
            fromFolder.mkdirs()


            val result = exporter.export(fromFolder, toFolder)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            assertFalse(expectedManagementDb.exists())
            assertFalse(expectedKeysDb.exists())
            assertFalse(result)
        }
    }

    @Test
    fun `export() returns false if an exception is thrown`() {
        runBlocking {
            val toFolderSpy = spy(toFolder)
            doThrow(RuntimeException()).`when`(toFolderSpy).mkdirs()

            fromFolder.mkdirs()
            File(fromFolder, "management.db").writeText("test")
            File(fromFolder, "keys.db").writeText("test")

            val result = exporter.export(fromFolder, toFolderSpy)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            assertFalse(expectedManagementDb.exists())
            assertFalse(expectedKeysDb.exists())
            assertFalse(result)
        }
    }

    @After
    fun tearDown() {
        cleanupFiles()
    }

    private fun cleanupFiles() {
        fromFolder.deleteRecursively()
        toFolder.deleteRecursively()
    }
}