package security.planck.network

import com.fsck.k9.RobolectricTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class UrlCheckerTest : RobolectricTest() {
    private val urlChecker = UrlChecker()

    @Test
    fun `check valid urls`() {
        assertTrue(urlChecker.isValidUrl("planck.dev"))
        assertTrue(urlChecker.isValidUrl("outlook.office365.com"))
        assertFalse(urlChecker.isValidUrl("planck"))
        assertFalse(urlChecker.isValidUrl(" "))
        assertFalse(urlChecker.isValidUrl(""))
        assertFalse(urlChecker.isValidUrl(null))
    }

    @Test
    fun `check reachable urls`() { // not working for imap urls, but it's ok to be used for http-like addresses
        assertTrue(urlChecker.isUrlReachable("http://google.com"))
        assertFalse(urlChecker.isUrlReachable("http://google.jjj"))
    }


}