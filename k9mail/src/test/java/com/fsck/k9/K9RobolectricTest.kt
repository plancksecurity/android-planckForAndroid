package com.fsck.k9

import android.app.Application
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A Robolectric test that does not create an instance of our [Application] class [K9].
 *
 * See also [RobolectricTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = EmptyApplication::class, sdk = [30], manifest = Config.NONE)
abstract class K9RobolectricTest : AutoCloseKoinTest()
