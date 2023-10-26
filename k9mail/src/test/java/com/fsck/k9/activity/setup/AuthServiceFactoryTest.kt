package com.fsck.k9.activity.setup

import android.app.Application
import com.fsck.k9.RobolectricTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertEquals
import net.openid.appauth.browser.BrowserDescriptor
import net.openid.appauth.browser.BrowserSelector
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

private const val MICROSOFT_BROWSER_PACKAGE = "com.microsoft.emx"
private const val OTHER_BROWSER_PACKAGE = "com.other.emx"
private const val VERSION = "1.0"

class AuthServiceFactoryTest : RobolectricTest() {
    private val app: Application = mockk(relaxed = true)
    private val factory = AuthServiceFactory(app)

    @Before
    fun setUp() {
        mockkStatic(BrowserSelector::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(BrowserSelector::class)
    }

    @Test
    fun `create() allowing MsBrowser returns a service with first browser available`() {
        every { BrowserSelector.getAllBrowsers(app) }.returns(
            listOf(
                BrowserDescriptor(
                    MICROSOFT_BROWSER_PACKAGE, emptySet(), VERSION, true
                ),
                BrowserDescriptor(
                    OTHER_BROWSER_PACKAGE, emptySet(), VERSION, true
                ),
            )
        )


        val service = factory.create(true)


        assertEquals(MICROSOFT_BROWSER_PACKAGE, service.browserDescriptor.packageName)
    }

    @Test
    fun `create() not allowing MsBrowser returns a service with a browser other than Microsoft if available`() {
        every { BrowserSelector.getAllBrowsers(app) }.returns(
            listOf(
                BrowserDescriptor(
                    MICROSOFT_BROWSER_PACKAGE, emptySet(), VERSION, true
                ),
                BrowserDescriptor(
                    OTHER_BROWSER_PACKAGE, emptySet(), VERSION, true
                ),
            )
        )


        val service = factory.create(false)


        assertEquals(OTHER_BROWSER_PACKAGE, service.browserDescriptor.packageName)
    }

    @Test
    fun `create() not allowing MsBrowser throws UnsuitableBrowserFoundException if only Microsoft browsers are available`() {
        every { BrowserSelector.getAllBrowsers(app) }.returns(
            listOf(
                BrowserDescriptor(
                    MICROSOFT_BROWSER_PACKAGE, emptySet(), VERSION, true
                ),
            )
        )


        assertThrows(UnsuitableBrowserFoundException::class.java) { factory.create(false) }
    }
}