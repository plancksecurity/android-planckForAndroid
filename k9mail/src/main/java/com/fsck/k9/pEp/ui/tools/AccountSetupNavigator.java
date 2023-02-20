package com.fsck.k9.pEp.ui.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.fragments.AccountSetupIncomingFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOptionsFragment;
import com.fsck.k9.pEp.ui.fragments.AccountSetupOutgoingFragment;
import com.fsck.k9.pEp.ui.fragments.ChooseAccountTypeFragment;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AccountSetupNavigator {

    private Boolean isEditing = false;
    private boolean loading;

    public enum Step {
        BASICS,
        INCOMING,
        OUTGOING,
        OPTIONS
    }

    private Step currentStep;
    private Account account;

    public void createGmailAccount(Context context) {
        Intent addAccountIntent = new Intent(Settings.ACTION_ADD_ACCOUNT)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        addAccountIntent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[] {"com.google"});
        context.startActivity(addAccountIntent);
    }

    public void goForward(FragmentManager fragmentManager, Account account, @Nullable Boolean makeDefault) {
        loading = false;
        if (currentStep.equals(Step.BASICS)) {
            goFromChooseAccountTypeToIncomingSettings(fragmentManager, account, makeDefault);
        } else if(currentStep.equals(Step.INCOMING)) {
            goFromIncomingSettingsToOutgoingSettings(fragmentManager, account, makeDefault);
        } else if(currentStep.equals(Step.OUTGOING)) {
            goFromOutgoingSettingsToAccountSetupOptions(fragmentManager, account, makeDefault);
        }
    }

    private void goFromOutgoingSettingsToAccountSetupOptions(FragmentManager fragmentManager, Account account, Boolean makeDefault) {
        if (makeDefault != null) {
            AccountSetupOptionsFragment accountSetupOptionsFragment = AccountSetupOptionsFragment.actionOptions(account, makeDefault);
            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                    .replace(R.id.account_setup_container, accountSetupOptionsFragment, "accountSetupOptionsFragment")
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void goFromIncomingSettingsToOutgoingSettings(FragmentManager fragmentManager, Account account, @Nullable Boolean makeDefault) {
        if (makeDefault != null) {
            AccountSetupOutgoingFragment accountSetupOutgoingFragment = AccountSetupOutgoingFragment.actionOutgoingSettings(account, makeDefault);
            fragmentManager
                    .beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                    .replace(R.id.account_setup_container, accountSetupOutgoingFragment, "accountSetupOutgoingFragment")
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void goFromChooseAccountTypeToIncomingSettings(FragmentManager fragmentManager, Account account, @Nullable Boolean makeDefault) {
        if (makeDefault != null) {
            AccountSetupIncomingFragment accountSetupIncomingFragment = AccountSetupIncomingFragment.actionIncomingSettings(account, makeDefault);
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                    .replace(R.id.account_setup_container, accountSetupIncomingFragment, "accountSetupIncomingFragment")
                    .addToBackStack(null)
                    .commit();
        } else {
            AccountSetupIncomingFragment accountSetupIncomingFragment = AccountSetupIncomingFragment.actionEditIncomingSettings(account);
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                    .replace(R.id.account_setup_container, accountSetupIncomingFragment, "accountSetupIncomingFragment")
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void goFromBasicsToChooseAccountTypesSettings(FragmentManager fragmentManager, Account account, @Nullable Boolean makeDefault) {
        if (makeDefault != null) {
            ChooseAccountTypeFragment chooseAccountTypeFragment = ChooseAccountTypeFragment.actionSelectAccountType(account, makeDefault);
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right)
                    .replace(R.id.account_setup_container, chooseAccountTypeFragment, "chooseAccountTypeFragment")
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void goBack(Activity activity, FragmentManager fragmentManager) {
        if(loading) {
            loading = false;
            return;
        }
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
        return currentStep.equals(Step.INCOMING) && !loading; // TODO review this
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

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isLoading() {
        return loading;
    }
}
