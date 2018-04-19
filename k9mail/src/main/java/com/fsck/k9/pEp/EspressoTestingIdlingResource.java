package com.fsck.k9.pEp;

import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;

public class EspressoTestingIdlingResource {
    private static final String RESOURCE = "EspressoTestingIdlingResource";

    private static CountingIdlingResource mCountingIdlingResource;

    public EspressoTestingIdlingResource(){
        mCountingIdlingResource =
                new CountingIdlingResource(RESOURCE);
    }

    public static void increment() {
        mCountingIdlingResource.increment();
    }

    public static void decrement() {
        mCountingIdlingResource.decrement();
    }

    public static IdlingResource getIdlingResource() {
        return mCountingIdlingResource;
    }

}