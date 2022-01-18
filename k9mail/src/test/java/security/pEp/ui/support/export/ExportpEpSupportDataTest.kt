package security.pEp.ui.support.export

import android.content.Context
import com.fsck.k9.pEp.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import com.nhaarman.mockito_kotlin.mock
import io.mockk.every
import io.mockk.spyk
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
class ExportpEpSupportDataTest {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()
    private val context: Context = mock()
    private val exportpEpSupportData = ExportpEpSupportData(context)
    private val homeFolder = File("src/test/java/security/pEp/ui/support/export/homeFolder")
    private val trustwordsFolder = File("src/test/java/security/pEp/ui/support/export/trustwordsFolder")
    private val fromFolders = listOf(homeFolder, trustwordsFolder)
    private val toFolder = File("src/test/java/security/pEp/ui/support/export/toFolder")

    @Before
    fun setUp() {
        cleanupFiles()
        fromFolders.forEach { it.mkdirs() }
    }

    @Test
    fun `export() copies files from origin to target directory`() {
        runBlocking {
            File(homeFolder, "management.db").writeText("test")
            File(homeFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")


            val result = exportpEpSupportData(fromFolders, toFolder)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            val expectedSystemDb = File(toFolder, "system.db")
            assertTrue(expectedManagementDb.exists())
            assertTrue(expectedKeysDb.exists())
            assertTrue(expectedSystemDb.exists())
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun `export() returns false if file copy fails`() {
        runBlocking {
            val result = exportpEpSupportData(fromFolders, toFolder)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            val expectedSystemDb = File(toFolder, "system.db")
            assertFalse(expectedManagementDb.exists())
            assertFalse(expectedKeysDb.exists())
            assertFalse(expectedSystemDb.exists())
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is CouldNotExportPEpDataException)
        }
    }

    @Test
    fun `export() returns false if an exception is thrown`() {
        runBlocking {
            val toFolderSpy = spyk(toFolder)
            every { toFolderSpy.mkdirs() }.throws(RuntimeException())

            File(homeFolder, "management.db").writeText("test")
            File(homeFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")

            val result = exportpEpSupportData(fromFolders, toFolderSpy)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            val expectedSystemDb = File(toFolder, "system.db")
            assertFalse(expectedManagementDb.exists())
            assertFalse(expectedKeysDb.exists())
            assertFalse(expectedSystemDb.exists())
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is CouldNotExportPEpDataException)
        }
    }

    @Test
    fun `export() returns false if there is not enough free space in device`() {
        runBlocking {
            val toFolderSpy = spyk(toFolder)
            every { toFolderSpy.freeSpace }.returns(0)

            File(homeFolder, "management.db").writeText("test")
            File(homeFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")


            val result = exportpEpSupportData(fromFolders, toFolderSpy)


            val expectedManagementDb = File(toFolder, "management.db")
            val expectedKeysDb = File(toFolder, "keys.db")
            val expectedSystemDb = File(toFolder, "system.db")
            assertFalse(expectedManagementDb.exists())
            assertFalse(expectedKeysDb.exists())
            assertFalse(expectedSystemDb.exists())
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NotEnoughSpaceInDeviceException)
        }
    }

    @After
    fun tearDown() {
        cleanupFiles()
    }

    private fun cleanupFiles() {
        fromFolders.forEach { it.deleteRecursively() }
        toFolder.deleteRecursively()
    }
}