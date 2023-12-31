package com.fsck.k9;

import android.app.Application;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

public class K9RobolectricTestRunner extends RobolectricTestRunner {

    public K9RobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setSdk(22)
                .setManifest(Config.NONE)
                .setApplication(Application.class)
                .build();
    }
}