package security.planck.mdm

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.*
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class UserProfileTest {
    private val context: Context = mockk()
    private val userProfile = UserProfile()
    private val devicePolicyManager: DevicePolicyManager = mockk()
    private val componentName: ComponentName = mockk()
    private val packageManager: PackageManager = mockk()

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        every { componentName.packageName }.returns("")
        every { context.packageManager }.returns(packageManager)
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) }.returns(true)
    }

    @Test
    fun `isRunningOnWorkProfile uses DevicePolicyManager to find if running on work profile`() {
        every { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }.returns(devicePolicyManager)
        every { devicePolicyManager.activeAdmins }.returns(listOf(componentName))
        every { devicePolicyManager.isProfileOwnerApp(any()) }.returns(true)


        val result = userProfile.isRunningOnWorkProfile(context)


        verify { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }
        verify { devicePolicyManager.activeAdmins }
        verify { devicePolicyManager.isProfileOwnerApp(any()) }
        assertEquals(true, result)
    }

    @Test
    fun `isRunningOnWorkProfile returns false if DevicePolicyManager service not found`() {
        every { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }.returns(null)


        val result = userProfile.isRunningOnWorkProfile(context)


        verify { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }
        verify { devicePolicyManager.wasNot(called) }
        assertEquals(false, result)
    }

    @Test
    fun `isRunningOnWorkProfile returns false if DevicePolicyManager has no active admins`() {
        every { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }.returns(devicePolicyManager)
        every { devicePolicyManager.activeAdmins }.returns(emptyList())
        every { devicePolicyManager.isProfileOwnerApp(any()) }.returns(true)


        val result = userProfile.isRunningOnWorkProfile(context)


        verify { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }
        verify { devicePolicyManager.activeAdmins }
        verify(exactly = 0) { devicePolicyManager.isProfileOwnerApp(any()) }
        assertEquals(false, result)
    }

    @Test
    fun `isRunningOnWorkProfile returns false if no active admin is profile owner app`() {
        every { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }.returns(devicePolicyManager)
        every { devicePolicyManager.activeAdmins }.returns(listOf(componentName))
        every { devicePolicyManager.isProfileOwnerApp(any()) }.returns(false)


        val result = userProfile.isRunningOnWorkProfile(context)


        verify { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }
        verify { devicePolicyManager.activeAdmins }
        verify { devicePolicyManager.isProfileOwnerApp(any()) }
        assertEquals(false, result)
    }

    @Test
    fun `isRunningOnWorkProfile returns false if PackageManager has not required feature`() {
        every { packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) }.returns(false)


        val result = userProfile.isRunningOnWorkProfile(context)


        verify { packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) }
        verify(exactly = 0) { ContextCompat.getSystemService(context, DevicePolicyManager::class.java) }
        verify { devicePolicyManager.wasNot(called) }
        assertEquals(false, result)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }
}