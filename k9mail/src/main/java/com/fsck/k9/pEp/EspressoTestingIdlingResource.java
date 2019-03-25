package com.fsck.k9.pEp;

import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;

import timber.log.Timber;

public class EspressoTestingIdlingResource {
    private static final String RESOURCE = "EspressoTestingIdlingResource";

    private static CountingIdlingResource mCountingIdlingResource;
    private static int contador = 0;
    private static boolean testStarted;

    public EspressoTestingIdlingResource(){
        mCountingIdlingResource =
                new CountingIdlingResource(RESOURCE);
        testStarted = false;
    }

    public static void increment() {
        if (mCountingIdlingResource != null) {
            contador++;
            testStarted = true;
            Timber.i("Contador: " + contador);
            mCountingIdlingResource.increment();
        }
    }

    public static void decrement() {
        Timber.i("Contador entra en decrement");
        if (mCountingIdlingResource != null && testStarted) {
            contador--;
            Timber.i("Contador: " + contador);
            mCountingIdlingResource.decrement();
        }
    }

    public static IdlingResource getIdlingResource() {
        return mCountingIdlingResource;
    }

}