package com.fsck.k9.pEp.ui.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;

public abstract class PEpFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
    }

    private ApplicationComponent getApplicationComponent() {
        return getK9().getComponent();
    }

    public K9 getK9() {
        return (K9) getActivity().getApplication();
    }

    protected abstract void initializeInjector(ApplicationComponent applicationComponent);

    public void showDialogFragment(String customMessage) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());

        builder.setMessage(customMessage)
                .setTitle(R.string.account_setup_failed_dlg_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> dialog.cancel()).show();
    }
}
