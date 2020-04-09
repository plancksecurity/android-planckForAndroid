package com.fsck.k9.pEp.ui.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.K9;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
import com.fsck.k9.pEp.infrastructure.modules.ActivityModule;
import com.fsck.k9.pEp.infrastructure.modules.ContactLoaderModule;
import com.fsck.k9.pEp.infrastructure.modules.PEpModule;

public abstract class PEpFragment extends Fragment {

    private PEpComponent pEpComponent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
        inject();
    }

    protected abstract void inject();

    private ApplicationComponent getApplicationComponent() {
        return getK9().getComponent();
    }

    public K9 getK9() {
        return (K9) getActivity().getApplication();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        pEpComponent = DaggerPEpComponent.builder()
                .applicationComponent(applicationComponent)
                .pEpModule(new PEpModule(getActivity(), LoaderManager.getInstance(this), getFragmentManager()))
                .activityModule(new ActivityModule(getActivity()))
                .contactLoaderModule(new ContactLoaderModule(getContext()))
                .build();
    }

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }

    @NonNull
    public String extractErrorDialogTitle(int stringResource) {
        String originalString = getResources().getString(stringResource);
        int cuttingPosition = originalString.indexOf(".");
        if (cuttingPosition > 0) {
            return originalString.substring(0, cuttingPosition);
        } else {
            return originalString;
        }
    }
}
