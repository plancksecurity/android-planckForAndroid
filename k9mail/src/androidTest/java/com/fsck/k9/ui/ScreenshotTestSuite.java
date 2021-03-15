package com.fsck.k9.ui;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        MessageListScreenshotTest.class,
        MessageComposeScreenshotTest.class,
        MessageViewScreenshotTest.class,
        SettingsScreenshotTest.class})
public class ScreenshotTestSuite {
}