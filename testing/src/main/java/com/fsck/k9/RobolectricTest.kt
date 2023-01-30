package com.fsck.k9

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * A Robolectric test that does not create an instance of our [Application] class [K9].
 *
 * See also [K9RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class, sdk = [30], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
abstract class RobolectricTest

class EmptyApplication : Application()
