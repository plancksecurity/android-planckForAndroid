package com.fsck.k9.pEp.ui.activities;

import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import org.junit.runner.RunWith;
/**
 * Created by juan on 11/10/17.
 */

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class GreyStatusEmailTestUIAutomator {

    private static final String BASIC_SAMPLE_PACKAGE = "com.fsck.k9.pEp.ui.activities";
    private static final int TIME = 2000;
    private static final String DESCRIPTION = "tester one";
    private static final String USER_NAME = "testerJ";
    private static final String EMAIL = "newemail@mail.es";
    private static final int LAUNCH_TIMEOUT = 5000;
    private UiDevice mDevice;
}
