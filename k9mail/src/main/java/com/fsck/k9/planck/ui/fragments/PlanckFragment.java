package com.fsck.k9.planck.ui.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;

import com.fsck.k9.K9;
import com.fsck.k9.planck.infrastructure.components.ApplicationComponent;
import com.fsck.k9.planck.infrastructure.components.DaggerPlanckComponent;
import com.fsck.k9.planck.infrastructure.components.PlanckComponent;
import com.fsck.k9.planck.infrastructure.modules.ActivityModule;
import com.fsck.k9.planck.infrastructure.modules.PlanckModule;
import com.fsck.k9.planck.ui.tools.ThemeManager;

import security.planck.ui.toolbar.ToolBarCustomizer;

public abstract class PlanckFragment extends Fragment {

    private PlanckComponent planckComponent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeInjector(getApplicationComponent());
        inject();
    }

    protected abstract void inject();

    protected void setupPlanckFragmentToolbar() {
        getToolbarCustomizer().setDefaultStatusBarColor();
    }

    private ApplicationComponent getApplicationComponent() {
        return getK9().getComponent();
    }

    public K9 getK9() {
        return (K9) getActivity().getApplication();
    }

    private void initializeInjector(ApplicationComponent applicationComponent) {
        applicationComponent.inject(this);
        planckComponent = DaggerPlanckComponent.builder()
                .applicationComponent(applicationComponent)
                .planckModule(new PlanckModule(getActivity(), LoaderManager.getInstance(this), getFragmentManager()))
                .activityModule(new ActivityModule(getActivity()))
                .build();
    }

    public PlanckComponent getPlanckComponent() {
        return planckComponent;
    }

    protected ToolBarCustomizer getToolbarCustomizer() {
        return planckComponent.toolbarCustomizer();
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
