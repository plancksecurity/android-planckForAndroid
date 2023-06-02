package com.fsck.k9

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that creates an instance of our [Application] class [K9]
 * without initialization.
 *
 * See also [K9RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class, manifest = Config.NONE, sdk = [30])
abstract class RobolectricTest

class RobolectricTestApp : K9() {
    override fun onCreate() {
        app = this
        Globals.setContext(this)
    }
}
