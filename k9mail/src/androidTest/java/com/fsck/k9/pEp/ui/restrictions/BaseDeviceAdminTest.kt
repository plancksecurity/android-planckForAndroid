package com.fsck.k9.pEp.ui.restrictions

import android.content.Intent
import androidx.test.uiautomator.By
import com.fsck.k9.common.BaseTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test


open class BaseDeviceAdminTest : BaseTest() {

    @Test
    fun emptyTest1() {
    }

    fun checkSwitchPreferenceStatus(textId: Int) {
        val textViewFound = false
        val selector = By.clazz("android.widget.TextView")
        while (!textViewFound) {
            device.findObjects(selector).forEach { obj ->
                if (obj.text.contains(resources.getString(textId))) {
                    device.waitForIdle()
                    val checkbox = obj.parent.parent.children[1].children[0]
                    assertFalse(checkbox.isChecked)
                    return
                }
            }
        }
    }

    companion object {
        const val ENFORCER_PACKAGE_NAME: String = "com.example.android.apprestrictionenforcer"
    }


}

