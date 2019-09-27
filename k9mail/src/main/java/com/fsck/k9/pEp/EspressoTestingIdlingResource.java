package com.fsck.k9.pEp;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

public class EspressoTestingIdlingResource {
    private static final String RESOURCE = "EspressoTestingIdlingResource";

    private static CountingIdlingResource mCountingIdlingResource;

    public EspressoTestingIdlingResource(){
        mCountingIdlingResource =
                new CountingIdlingResource(RESOURCE);
    }

    public static void increment() {
        if (mCountingIdlingResource != null) {
            mCountingIdlingResource.increment();
        }
    }

    public static void decrement() {
        if (mCountingIdlingResource != null) {
            mCountingIdlingResource.decrement();
        }
    }

    public static IdlingResource getIdlingResource() {
        return mCountingIdlingResource;
    }

}