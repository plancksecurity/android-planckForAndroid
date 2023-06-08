package com.fsck.k9.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ST1AccountSetupScreenshotTest.class,
        ST2MessageListScreenshotTest.class,
        ST4MessageComposeScreenshotTest.class,
        ST3MessageViewScreenshotTest.class,
        ST5SettingsScreenshotTest.class,
        ST6SyncScreenshotTest.class})
public class ScreenshotTestSuite {
}