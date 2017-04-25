package com.fsck.k9.pEp.infrastructure.modules;

import android.app.Fragment;

import com.fsck.k9.pEp.ui.fragments.PEpSettingsCheck;
import com.fsck.k9.pEp.ui.fragments.PEpSettingsChecker;

import dagger.Module;
import dagger.Provides;

@Module
public class FragmentModule {
    private final Fragment fragment;

    public FragmentModule(Fragment fragment) {
        this.fragment = fragment;
    }

    @Provides
    Fragment provideFragment() {
        return this.fragment;
    }

    @Provides
    public PEpSettingsChecker providePEpSettingsCheck() {
        return new PEpSettingsCheck(fragment.getActivity().getApplicationContext());
    }
}
