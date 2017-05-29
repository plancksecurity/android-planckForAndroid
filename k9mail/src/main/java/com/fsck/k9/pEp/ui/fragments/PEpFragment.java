package com.fsck.k9.pEp.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.pEp.infrastructure.components.DaggerPEpComponent;
import com.fsck.k9.pEp.infrastructure.components.PEpComponent;
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
                .pEpModule(new PEpModule(getActivity(), getLoaderManager(), getFragmentManager()))
                .build();
    }

    public void showDialogFragment(String customMessage) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());

        builder.setMessage(customMessage)
                .setTitle(R.string.account_setup_failed_dlg_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> dialog.cancel()).show();
    }

    public PEpComponent getpEpComponent() {
        return pEpComponent;
    }
}
