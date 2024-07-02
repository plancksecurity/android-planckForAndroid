package com.fsck.k9.planck.ui.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.fragment.app.FragmentManager;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.planck.ui.fragments.AccountSetupBasicsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.planck.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.planck.ui.fragments.GoogleAuthGuideStep1Fragment;
import com.fsck.k9.planck.ui.fragments.GoogleAuthGuideStep2Fragment;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountSetupNavigator {

    private Boolean isEditing = false;

    public enum Step {
        SELECT_AUTH,
        BASICS,
        INCOMING,
        OUTGOING,
        OPTIONS,
        GOOGLE_GUIDE_STEP_1,
        GOOGLE_GUIDE_STEP_2
    }

    private Step currentStep;
    private Account account;

    public void createGmailAccount(Context context) {
        Intent addAccountIntent = new Intent(Settings.ACTION_ADD_ACCOUNT)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
        context.startActivity(addAccountIntent);
    }

    public void goForward(FragmentManager fragmentManager, Account account) {
        if (currentStep.equals(Step.BASICS) || currentStep.equals(Step.SELECT_AUTH)) {
            goFromChooseAccountTypeToIncomingSettings(fragmentManager, account);
        } else if(currentStep.equals(Step.INCOMING)) {
            goFromIncomingSettingsToOutgoingSettings(fragmentManager, account);
        } else if(currentStep.equals(Step.OUTGOING)) {
            goFromOutgoingSettingsToAccountSetupOptions(fragmentManager, account);
        }
    }

    public void goToAccountSetupBasicsFragment(FragmentManager fragmentManager) {
        AccountSetupBasicsFragment fragment = new AccountSetupBasicsFragment();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, fragment, "accountSetupBasicFragment")
                .addToBackStack(null)
                .commit();
    }

    public void goToGoogleAuthGuideStep1(FragmentManager fragmentManager) {
        GoogleAuthGuideStep1Fragment fragment = new GoogleAuthGuideStep1Fragment();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, fragment, "googleSetupGuideStep1")
                .addToBackStack(null)
                .commit();
    }

    public void goToGoogleAuthGuideStep2(FragmentManager fragmentManager) {
        GoogleAuthGuideStep2Fragment fragment = new GoogleAuthGuideStep2Fragment();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, fragment, "googleSetupGuideStep2")
                .addToBackStack(null)
                .commit();
    }

    private void goFromOutgoingSettingsToAccountSetupOptions(FragmentManager fragmentManager, Account account) {
        AccountSetupOptionsFragment accountSetupOptionsFragment = AccountSetupOptionsFragment.actionOptions(account);
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, accountSetupOptionsFragment, "accountSetupOptionsFragment")
                .addToBackStack(null)
                .commit();
    }

    private void goFromIncomingSettingsToOutgoingSettings(FragmentManager fragmentManager, Account account) {
        AccountSetupOutgoingFragment accountSetupOutgoingFragment = AccountSetupOutgoingFragment.actionOutgoingSettings(account);
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, accountSetupOutgoingFragment, "accountSetupOutgoingFragment")
                .addToBackStack(null)
                .commit();
    }

    private void goFromChooseAccountTypeToIncomingSettings(FragmentManager fragmentManager, Account account) {
        AccountSetupIncomingFragment accountSetupIncomingFragment = AccountSetupIncomingFragment.actionIncomingSettings(account);
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                .replace(R.id.account_setup_container, accountSetupIncomingFragment, "accountSetupIncomingFragment")
                .addToBackStack(null)
                .commit();
    }

    public void goBack(Activity activity, FragmentManager fragmentManager) {
        if (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStack();
        } else {
            activity.finish();
        }
    }

    @Inject
    public AccountSetupNavigator() {
    }

    public Boolean shouldDeleteAccount() {
        return currentStep.equals(Step.INCOMING); // TODO review this
    }

    public void setCurrentStep(Step currentStep, Account account) {
        this.currentStep = currentStep;
        this.account = account;
    }

    public void setIsEditing(Boolean isEditing) {
        this.isEditing = isEditing;
    }

    public Account getAccount() {
        return account;
    }

    public Step getCurrentStep() {
        return currentStep;
    }
}
