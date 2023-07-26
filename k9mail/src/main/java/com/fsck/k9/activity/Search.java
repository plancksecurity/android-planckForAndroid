package com.fsck.k9.activity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class Search extends MessageList {
    protected static boolean isActive = false;

    public static boolean isActive() {
        return isActive;
    }

    public static void setActive(boolean val) {
        isActive = val;
    }

    @Override
    public void onStart() {
        setActive(true);
        super.onStart();
    }

    @Override
    public void onStop() {
        setActive(false);
        super.onStop();
    }



}
