package com.fsck.k9.pEp;

import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.idling.CountingIdlingResource;

import timber.log.Timber;

public class EspressoTestingIdlingResource {
    private static final String RESOURCE = "EspressoTestingIdlingResource";

    private static CountingIdlingResource mCountingIdlingResource;
    private static int contador = 0;

    public EspressoTestingIdlingResource(){
        mCountingIdlingResource =
                new CountingIdlingResource(RESOURCE);
    }

    public static void increment() {
        if (mCountingIdlingResource != null) {
            contador++;
            Timber.i("Contador: " + contador);
            mCountingIdlingResource.increment();
        }
    }

    public static void decrement() {
        if (mCountingIdlingResource != null) {
            contador--;
            Timber.i("Contador: " + contador);
            mCountingIdlingResource.decrement();
        }
    }

    public static IdlingResource getIdlingResource() {
        return mCountingIdlingResource;
    }

}