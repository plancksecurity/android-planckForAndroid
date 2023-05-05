package security.planck.ui.support.export


import android.content.Context
import com.fsck.k9.RobolectricTest
import com.fsck.k9.pEp.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.pEp.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.pEp.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import security.planck.file.PEpSystemFileLocator
import java.io.File

@ExperimentalCoroutinesApi
@Config(sdk = [28])
class ExportpEpSupportDataTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()
    private val context: Context = mockk()
    private val systemFileLocator: PEpSystemFileLocator = mockk()
    private val exportpEpSupportData = ExportpEpSupportData(context, systemFileLocator)
    private val homeFolder = File("src/test/java/security/pEp/ui/support/export/homeFolder")
    private val pEpFolder = File(homeFolder, ".pEp")
    private val trustwordsFolder = File("src/test/java/security/pEp/ui/support/export/trustwordsFolder")
    private val fromFolders = listOf(pEpFolder, trustwordsFolder)
    private val destinationBaseFolder = File("src/test/java/security/pEp/ui/support/export")
    private val destinationSubFolder = "toFolder"
    private val toFolder = File(destinationBaseFolder, destinationSubFolder)

    @Before
    fun setUp() {
        //every { systemFileLocator.homeFolder }.returns(homeFolder)
        coEvery { systemFileLocator.trustwordsFolder }.returns(trustwordsFolder)
        coEvery { systemFileLocator.pEpFolder }.returns(pEpFolder)
        cleanupFiles()
        fromFolders.forEach { it.mkdirs() }
    }

    @Test
    fun `export() copies files from origin to target directory`() {
        runBlocking {
            File(pEpFolder, "management.db").writeText("test")
            File(pEpFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")


            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


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
            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


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
    fun `export() returns Result_failure if an exception is thrown`() {
        runBlocking {
            every { systemFileLocator.pEpFolder }.throws(RuntimeException())

            File(pEpFolder, "management.db").writeText("test")
            File(pEpFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")

            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


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
            val baseFolderSpy = spyk(destinationBaseFolder)
            every { baseFolderSpy.freeSpace }.returns(0)

            File(pEpFolder, "management.db").writeText("test")
            File(pEpFolder, "keys.db").writeText("test")
            File(trustwordsFolder, "system.db").writeText("test")


            val result = exportpEpSupportData(baseFolderSpy, destinationSubFolder)


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