package com.fsck.k9.ui;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@Ignore("not used for now")
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