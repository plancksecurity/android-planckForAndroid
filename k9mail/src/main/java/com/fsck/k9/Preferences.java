
package com.fsck.k9;

import android.content.Context;

import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.preferences.Storage;
import com.fsck.k9.preferences.StorageEditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

import static security.pEp.remoteConfiguration.ConfigurationManager.RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION_MANAGED;


public class Preferences {

    private static Preferences preferences;

    public static synchronized Preferences getPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        if (preferences == null) {
            preferences = new Preferences(appContext);
        }
        return preferences;
    }


    private Storage storage;
    private Map<String, Account> accounts = null;
    private List<Account> accountsInOrder = null;
    private Account newAccount;
    private Context context;

    private Preferences(Context context) {
        storage = Storage.getStorage(context);
        this.context = context;
        if (storage.isEmpty()) {
            Timber.i("Preferences storage is zero-size, importing from Android-style preferences");
            StorageEditor editor = storage.edit();
            editor.copy(context.getSharedPreferences("AndroidMail.Main", Context.MODE_PRIVATE));
            editor.commit();
        }
    }

    public synchronized void loadAccounts() {
        accounts = new HashMap<>();
        accountsInOrder = new LinkedList<>();
        String accountUuids = getStorage().getString("accountUuids", null);
        if ((accountUuids != null) && (accountUuids.length() != 0)) {
            String[] uuids = accountUuids.split(",");
            for (String uuid : uuids) {
                Account newAccount = new Account(this, uuid);
                accounts.put(uuid, newAccount);
                accountsInOrder.add(newAccount);
            }
        }
        if ((newAccount != null) && newAccount.getAccountNumber() != -1) {
            accounts.put(newAccount.getUuid(), newAccount);
            if (!accountsInOrder.contains(newAccount)) {
                accountsInOrder.add(newAccount);
            }
            newAccount = null;
        }
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts
     */
    public synchronized List<Account> getAccounts() {
        if (accounts == null) {
            loadAccounts();
        }
        List<Account> theAccounts = new ArrayList<>();
        for(Account account : accountsInOrder) {
            if(account.getSetupState() == Account.SetupState.READY) {
                theAccounts.add(account);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(theAccounts));
    }

    public synchronized List<Account> getAccountsAllowingIncomplete() {
        if (accounts == null) {
            loadAccounts();
        }
        return Collections.unmodifiableList(new ArrayList<>(accountsInOrder));
    }

    /**
     * Returns an array of the accounts on the system. If no accounts are
     * registered the method returns an empty array.
     *
     * @return all accounts with {@link Account#isAvailable(Context)}
     */
    public synchronized Collection<Account> getAvailableAccounts() {
        List<Account> allAccounts = getAccounts();
        Collection<Account> retval = new ArrayList<>(accounts.size());
        for (Account account : allAccounts) {
            if (account.isEnabled() && account.isAvailable(context)) {
                retval.add(account);
            }
        }

        return retval;
    }

    public synchronized Account getAccount(String uuid) {
        if (accounts == null) {
            loadAccounts();
        }
        Account account = accounts.get(uuid);
        if(account != null && account.getSetupState() == Account.SetupState.READY) {
            return account;
        }
        else return null;
    }

    public synchronized Account getAccountAllowingIncomplete(String uuid) {
        if (accounts == null) {
            loadAccounts();
        }
        return accounts.get(uuid);
    }

    public synchronized Account newAccount() {
        newAccount = new Account(context);
        accounts.put(newAccount.getUuid(), newAccount);
        accountsInOrder.add(newAccount);

        return newAccount;
    }

    public synchronized void deleteAccount(Account account) {
        if (accounts != null) {
            accounts.remove(account.getUuid());
        }
        if (accountsInOrder != null) {
            accountsInOrder.remove(account);
        }

        try {
            RemoteStore.removeInstance(account);
        } catch (Exception e) {
            Timber.e(e, "Failed to reset remote store for account %s", account.getUuid());
        }
        LocalStore.removeAccount(account);

        account.delete(this);
        getStorage().edit().putString("defaultAccountUuid", null).commit();
        if (newAccount == account) {
            newAccount = null;
        }
    }

    /**
     * Returns the Account marked as default. If no account is marked as default
     * the first account in the list is marked as default and then returned. If
     * there are no accounts on the system the method returns null.
     */
    public Account getDefaultAccount() {
        String defaultAccountUuid = getStorage().getString("defaultAccountUuid", null);
        Account defaultAccount = getAccount(defaultAccountUuid);

        if (defaultAccount == null) {
            Collection<Account> accounts = getAvailableAccounts();
            if (!accounts.isEmpty()) {
                defaultAccount = accounts.iterator().next();
                setDefaultAccount(defaultAccount);
            }
        }

        return defaultAccount;
    }

    public void setDefaultAccount(Account account) {
        getStorage().edit().putString("defaultAccountUuid", account.getUuid()).commit();
    }

    public Storage getStorage() {
        return storage;
    }

    static <T extends Enum<T>> T getEnumStringPref(Storage storage, String key, T defaultEnum) {
        String stringPref = storage.getString(key, null);

        if (stringPref == null) {
            return defaultEnum;
        } else {
            try {
                return Enum.valueOf(defaultEnum.getDeclaringClass(), stringPref);
            } catch (IllegalArgumentException ex) {
                Timber.w(ex, "Unable to convert preference key [%s] value [%s] to enum of type %s",
                        key, stringPref, defaultEnum.getDeclaringClass());

                return defaultEnum;
            }
        }
    }

    public void setAccounts(List<Account> reorderedAccounts) {
        accountsInOrder = reorderedAccounts;
        List<String> uuids = new ArrayList<>(reorderedAccounts.size());
        for (Account account : reorderedAccounts) {
            uuids.add(account.getUuid());
        }
        String accountUuids = Utility.combine(uuids.toArray(), ',');

        StorageEditor editor = getStorage().edit();
        editor.putString("accountUuids", accountUuids);
        editor.commit();
    }

    public boolean isPepEnablePrivacyProtectionManaged() {
        return storage.getBoolean(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION_MANAGED, false);
    }

    public void setPepEnablePrivacyProtectionManaged(boolean isManaged) {
        getStorage().edit().putBoolean(RESTRICTION_PEP_DISABLE_PRIVACY_PROTECTION_MANAGED, isManaged).commit();
    }

    /*public synchronized List<String> getMasterKeys(String uid) {
        keysInOrder = new LinkedList<>();
        String keysFRPs = getStorage().getString(uid, null);
        if ((keysFRPs != null) && (keysFRPs.length() != 0)) {
            String[] fprs = keysFRPs.split(",");
            for (String fpr : fprs) {
                keysInOrder.add(fpr);
            }
        }
        return keysInOrder;
    }

    public synchronized String[] getMasterKeysArray(String uid) {
        keysInOrder = getMasterKeys(uid);
        String[] keysArray = new String[keysInOrder.size()];
        for (int i = 0; i < keysArray.length; i++) {
            keysArray[i] = keysInOrder.get(i);
        }
        return keysArray;
    }

    public void setMasterKeysFPRs(String accountUuid, List<String> keys) {
        List<String> fprs = new ArrayList<>(keys.size());
        for (String fpr : keys) {
            fprs.add(fpr);
        }
        String accountUuids = Utility.combine(fprs.toArray(), ',');

        StorageEditor editor = getStorage().edit();
        editor.putString(accountUuid, accountUuids);
        editor.commit();
    }

     */
}
