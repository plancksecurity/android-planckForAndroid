package com.fsck.k9.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs all unit tests.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        MessageListScreenshotTest.class,
        MessageComposeScreenshotTest.class,
        MessageViewScreenshotTest.class,
        SettingsScreenshotTest.class})
public class ScreenshotTestSuite {
}