package security.planck.ui.support.export


import android.content.Context
import android.net.Uri
import com.fsck.k9.RobolectricTest
import com.fsck.k9.planck.infrastructure.exceptions.CouldNotExportPEpDataException
import com.fsck.k9.planck.infrastructure.exceptions.NotEnoughSpaceInDeviceException
import com.fsck.k9.planck.saveToDocuments
import com.fsck.k9.planck.testutils.CoroutineTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.FileUtils
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import security.planck.file.PlanckSystemFileLocator
import java.io.File
import java.io.FileInputStream

@ExperimentalCoroutinesApi
@Config(sdk = [30])
class ExportpEpSupportDataTest: RobolectricTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()
    private val context: Context = mockk()
    private val systemFileLocator: PlanckSystemFileLocator = mockk()
    private val exportpEpSupportData = ExportpEpSupportData(context, systemFileLocator)
    private val homeFolder = File("src/test/java/security/planck/ui/support/export/homeFolder")
    private val planckFolder = File(homeFolder, ".pEp")
    private val trustwordsFolder = File("src/test/java/security/planck/ui/support/export/trustwordsFolder")
    private val fromFolders = listOf(planckFolder, trustwordsFolder)
    private val destinationBaseFolder = File("src/test/java/security/planck/ui/support/export")
    private val destinationSubFolder = "toFolder"
    private val toFolder = File(destinationBaseFolder, destinationSubFolder)

    @Before
    fun setUp() {
        coEvery { systemFileLocator.trustwordsFolder }.returns(trustwordsFolder)
        coEvery { systemFileLocator.pEpFolder }.returns(planckFolder)
        mockkStatic("com.fsck.k9.planck.MediaStoreUtilsKt")
        mockkStatic(FileUtils::class)
        cleanupFiles()
        fromFolders.forEach { it.mkdirs() }
    }

    @Test
    fun `export() copies files from origin to target directory using MediaStoreUtils`() {
        runBlocking {
            val managementDb = File(planckFolder, "management.db").apply { writeText("test") }
            val keysDb = File(planckFolder, "keys.db").apply { writeText("test") }
            val systemDb = File(trustwordsFolder, "system.db").apply { writeText("test") }
            val managementDbIs: FileInputStream = mockk()
            val keysDbIs: FileInputStream = mockk()
            val systemDbIs: FileInputStream = mockk()
            // stub FileUtils
            coEvery { FileUtils.openInputStream(managementDb) }.returns(managementDbIs)
            coEvery { FileUtils.openInputStream(keysDb) }.returns(keysDbIs)
            coEvery { FileUtils.openInputStream(systemDb) }.returns(systemDbIs)
            // stub MediaStoreUtils
            coEvery { managementDbIs.saveToDocuments(any(), any(), any(), any()) }.returns(Uri.EMPTY)
            coEvery { keysDbIs.saveToDocuments(any(), any(), any(), any()) }.returns(Uri.EMPTY)
            coEvery { systemDbIs.saveToDocuments(any(), any(), any(), any()) }.returns(Uri.EMPTY)
            // simulate files written in target folder
            toFolder.mkdirs()
            File(toFolder, "management.db").writeText("test")
            File(toFolder, "keys.db").writeText("test")
            File(toFolder, "system.db").writeText("test")


            val result = exportpEpSupportData(destinationBaseFolder, destinationSubFolder)


            coVerify { FileUtils.openInputStream(managementDb) }
            coVerify { FileUtils.openInputStream(keysDb) }
            coVerify { FileUtils.openInputStream(systemDb) }

            coVerify { managementDbIs.saveToDocuments(context, "", "management.db", "toFolder") }
            coVerify { keysDbIs.saveToDocuments(context, "", "keys.db", "toFolder") }
            coVerify { systemDbIs.saveToDocuments(context, "", "system.db", "toFolder") }

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

            File(planckFolder, "management.db").writeText("test")
            File(planckFolder, "keys.db").writeText("test")
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

            File(planckFolder, "management.db").writeText("test")
            File(planckFolder, "keys.db").writeText("test")
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
        unmockkStatic("com.fsck.k9.planck.MediaStoreUtilsKt")
        unmockkStatic(FileUtils::class)
        cleanupFiles()
    }

    private fun cleanupFiles() {
        fromFolders.forEach { it.deleteRecursively() }
        toFolder.deleteRecursively()
    }
}