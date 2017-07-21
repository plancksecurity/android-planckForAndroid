package com.fsck.k9.pEp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.misc.ExtendedAsyncTask;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.preferences.SettingsExporter;
import com.fsck.k9.preferences.SettingsImportExportException;
import com.fsck.k9.preferences.SettingsImporter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public abstract class PEpImporterActivity extends PepPermissionActivity {

    private static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    private static final int DIALOG_NO_FILE_MANAGER = 4;

    protected abstract void refresh();

    public void onImport() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> infos = packageManager.queryIntentActivities(i, 0);

        if (infos.size() > 0) {
            startActivityForResult(Intent.createChooser(i, null),
                    ACTIVITY_REQUEST_PICK_SETTINGS_FILE);
        } else {
            showDialog(DIALOG_NO_FILE_MANAGER);
        }
    }

    public abstract void onImport(Uri uri);

    public abstract void setNonConfigurationInstance(NonConfigurationInstance inst);

    private void showImportSelectionDialog(SettingsImporter.ImportContents importContents, Uri uri) {
        ImportSelectionDialog dialog = new ImportSelectionDialog(importContents, uri);
        dialog.show(this);
        setNonConfigurationInstance(dialog);
    }

    private void showSimpleDialog(int headerRes, int messageRes, Object... args) {
        SimpleDialog dialog = new SimpleDialog(headerRes, messageRes, args);
        dialog.show(this);
        setNonConfigurationInstance(dialog);
    }

    /**
     * Shows a dialog that displays how many accounts were successfully imported.
     *
     * @param importResults
     *         The {@link SettingsImporter.ImportResults} instance returned by the {@link SettingsImporter}.
     * @param filename
     *         The name of the settings file that was imported.
     */
    private void showAccountsImportedDialog(SettingsImporter.ImportResults importResults, String filename) {
        AccountsImportedDialog dialog = new AccountsImportedDialog(importResults, filename);
        dialog.show(this);
        setNonConfigurationInstance(dialog);
    }

    private void promptForServerPasswords(final List<Account> disabledAccounts) {
        Account account = disabledAccounts.remove(0);
        PasswordPromptDialog dialog = new PasswordPromptDialog(account, disabledAccounts);
        setNonConfigurationInstance(dialog);
        dialog.show(this);
    }

    public static class ListImportContentsAsyncTask extends ExtendedAsyncTask<Void, Void, Boolean> {
        private Uri mUri;
        private SettingsImporter.ImportContents mImportContents;

        public ListImportContentsAsyncTask(PEpImporterActivity activity, Uri uri) {
            super(activity);

            mUri = uri;
        }

        @Override
        protected void showProgressDialog() {
            String title = mContext.getString(R.string.settings_import_dialog_title);
            String message = mContext.getString(R.string.settings_import_scanning_file);
            mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                ContentResolver resolver = mContext.getContentResolver();
                InputStream is = resolver.openInputStream(mUri);
                try {
                    mImportContents = SettingsImporter.getImportStreamContents(is);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        /* Ignore */
                    }
                }
            } catch (SettingsImportExportException e) {
                Timber.w(e, "Exception during export");
                return false;
            } catch (FileNotFoundException e) {
                Timber.w("Couldn't read content from URI %s", mUri);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            PEpImporterActivity activity = (PEpImporterActivity) mActivity;

            // Let the activity know that the background task is complete
            activity.setNonConfigurationInstance(null);

            removeProgressDialog();

            if (success) {
                activity.showImportSelectionDialog(mImportContents, mUri);
            } else {
                String filename = mUri.getLastPathSegment();
                //TODO: better error messages
                activity.showSimpleDialog(R.string.settings_import_failed_header,
                        R.string.settings_import_failure, filename);
            }
        }
    }

    public static class SimpleDialog implements NonConfigurationInstance {
        private final int mHeaderRes;
        private final int mMessageRes;
        private Object[] mArguments;
        private Dialog mDialog;

        SimpleDialog(int headerRes, int messageRes, Object... args) {
            this.mHeaderRes = headerRes;
            this.mMessageRes = messageRes;
            this.mArguments = args;
        }

        @Override
        public void restore(Activity activity) {
            show((PEpImporterActivity) activity);
        }

        @Override
        public boolean retain() {
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
                return true;
            }
            return false;
        }

        public void show(final PEpImporterActivity activity) {
            final String message = generateMessage(activity);

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(mHeaderRes);
            builder.setMessage(message);
            builder.setPositiveButton(R.string.okay_action,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            activity.setNonConfigurationInstance(null);
                            okayAction(activity);
                        }
                    });
            mDialog = builder.show();
        }

        /**
         * Returns the message the dialog should display.
         *
         * @param activity
         *         The {@code Activity} this dialog belongs to.
         *
         * @return The message the dialog should display
         */
        protected String generateMessage(PEpImporterActivity activity) {
            return activity.getString(mMessageRes, mArguments);
        }

        /**
         * This method is called after the "OK" button was pressed.
         *
         * @param activity
         *         The {@code Activity} this dialog belongs to.
         */
        protected void okayAction(PEpImporterActivity activity) {
            // Do nothing
        }
    }

    public static class ImportSelectionDialog implements NonConfigurationInstance {
        private SettingsImporter.ImportContents mImportContents;
        private Uri mUri;
        private AlertDialog mDialog;
        private SparseBooleanArray mSelection;


        ImportSelectionDialog(SettingsImporter.ImportContents importContents, Uri uri) {
            mImportContents = importContents;
            mUri = uri;
        }

        @Override
        public void restore(Activity activity) {
            show((PEpImporterActivity) activity, mSelection);
        }

        @Override
        public boolean retain() {
            if (mDialog != null) {
                // Save the selection state of each list item
                mSelection = mDialog.getListView().getCheckedItemPositions();

                mDialog.dismiss();
                mDialog = null;
                return true;
            }
            return false;
        }

        public void show(PEpImporterActivity activity) {
            show(activity, null);
        }

        public void show(final PEpImporterActivity activity, SparseBooleanArray selection) {
            List<String> contents = new ArrayList<String>();

            if (mImportContents.globalSettings) {
                contents.add(activity.getString(R.string.settings_import_global_settings));
            }

            for (SettingsImporter.AccountDescription account : mImportContents.accounts) {
                contents.add(account.name);
            }

            int count = contents.size();
            boolean[] checkedItems = new boolean[count];
            if (selection != null) {
                for (int i = 0; i < count; i++) {
                    checkedItems[i] = selection.get(i);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    checkedItems[i] = true;
                }
            }

            //TODO: listview header: "Please select the settings you wish to import"
            //TODO: listview footer: "Select all" / "Select none" buttons?
            //TODO: listview footer: "Overwrite existing accounts?" checkbox

            DialogInterface.OnMultiChoiceClickListener listener = new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    ((AlertDialog) dialog).getListView().setItemChecked(which, isChecked);
                }
            };

            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMultiChoiceItems(contents.toArray(new String[0]), checkedItems, listener);
            builder.setTitle(activity.getString(R.string.settings_import_selection));
            builder.setInverseBackgroundForced(true);
            builder.setPositiveButton(R.string.okay_action,
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ListView listView = ((AlertDialog) dialog).getListView();
                            SparseBooleanArray pos = listView.getCheckedItemPositions();

                            boolean includeGlobals = mImportContents.globalSettings ? pos.get(0) : false;
                            List<String> accountUuids = new ArrayList<String>();
                            int start = mImportContents.globalSettings ? 1 : 0;
                            for (int i = start, end = listView.getCount(); i < end; i++) {
                                if (pos.get(i)) {
                                    accountUuids.add(mImportContents.accounts.get(i - start).uuid);
                                }
                            }

                    /*
                     * TODO: Think some more about this. Overwriting could change the store
                     * type. This requires some additional code in order to work smoothly
                     * while the app is running.
                     */
                            boolean overwrite = false;

                            dialog.dismiss();
                            activity.setNonConfigurationInstance(null);

                            ImportAsyncTask importAsyncTask = new ImportAsyncTask(activity,
                                    includeGlobals, accountUuids, overwrite, mUri);
                            activity.setNonConfigurationInstance(importAsyncTask);
                            importAsyncTask.execute();
                        }
                    });
            builder.setNegativeButton(R.string.cancel_action,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            activity.setNonConfigurationInstance(null);
                        }
                    });
            mDialog = builder.show();
        }
    }

    private static class ImportAsyncTask extends ExtendedAsyncTask<Void, Void, Boolean> {
        private boolean mIncludeGlobals;
        private List<String> mAccountUuids;
        private boolean mOverwrite;
        private Uri mUri;
        private SettingsImporter.ImportResults mImportResults;

        private ImportAsyncTask(PEpImporterActivity activity, boolean includeGlobals,
                                List<String> accountUuids, boolean overwrite, Uri uri) {
            super(activity);
            mIncludeGlobals = includeGlobals;
            mAccountUuids = accountUuids;
            mOverwrite = overwrite;
            mUri = uri;
        }

        @Override
        protected void showProgressDialog() {
            String title = mContext.getString(R.string.settings_import_dialog_title);
            String message = mContext.getString(R.string.settings_importing);
            mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                InputStream is = mContext.getContentResolver().openInputStream(mUri);
                try {
                    mImportResults = SettingsImporter.importSettings(mContext, is,
                            mIncludeGlobals, mAccountUuids, mOverwrite);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        /* Ignore */
                    }
                }
            } catch (SettingsImportExportException e) {
                Timber.w(e, "Exception during import");
                return false;
            } catch (FileNotFoundException e) {
                Timber.w(e, "Couldn't open import file");
                return false;
            } catch (Exception e) {
                Timber.w(e, "Unknown error");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            PEpImporterActivity activity = (PEpImporterActivity) mActivity;

            // Let the activity know that the background task is complete
            activity.setNonConfigurationInstance(null);

            removeProgressDialog();

            String filename = mUri.getLastPathSegment();
            boolean globalSettings = mImportResults.globalSettings;
            int imported = mImportResults.importedAccounts.size();
            if (success && (globalSettings || imported > 0)) {
                if (imported == 0) {
                    activity.showSimpleDialog(R.string.settings_import_success_header,
                            R.string.settings_import_global_settings_success, filename);
                } else {
                    activity.showAccountsImportedDialog(mImportResults, filename);
                }

                activity.refresh();
            } else {
                //TODO: better error messages
                activity.showSimpleDialog(R.string.settings_import_failed_header,
                        R.string.settings_import_failure, filename);
            }
        }
    }

    /**
     * A dialog that displays how many accounts were successfully imported.
     */
    public static class AccountsImportedDialog extends SimpleDialog {
        private SettingsImporter.ImportResults mImportResults;
        private String mFilename;

        public AccountsImportedDialog(SettingsImporter.ImportResults importResults, String filename) {
            super(R.string.settings_import_success_header, R.string.settings_import_success);
            mImportResults = importResults;
            mFilename = filename;
        }

        @Override
        protected String generateMessage(PEpImporterActivity activity) {
            //TODO: display names of imported accounts (name from file *and* possibly new name)

            int imported = mImportResults.importedAccounts.size();
            String accounts = activity.getResources().getQuantityString(
                    R.plurals.settings_import_accounts, imported, imported);
            return activity.getString(R.string.settings_import_success, accounts, mFilename);
        }

        @Override
        protected void okayAction(PEpImporterActivity activity) {
            Context context = activity.getApplicationContext();
            Preferences preferences = Preferences.getPreferences(context);
            List<Account> disabledAccounts = new ArrayList<Account>();
            for (SettingsImporter.AccountDescriptionPair accountPair : mImportResults.importedAccounts) {
                Account account = preferences.getAccount(accountPair.imported.uuid);
                if (account != null && !account.isEnabled()) {
                    disabledAccounts.add(account);
                }
            }
            if (disabledAccounts.size() > 0) {
                activity.promptForServerPasswords(disabledAccounts);
            } else {
                activity.setNonConfigurationInstance(null);
            }
        }
    }

    /**
     * Ask the user for the incoming/outgoing server passwords.
     */
    public static class PasswordPromptDialog implements NonConfigurationInstance, TextWatcher {
        private AlertDialog mDialog;
        private EditText mIncomingPasswordView;
        private EditText mOutgoingPasswordView;
        private CheckBox mUseIncomingView;

        private Account mAccount;
        private List<Account> mRemainingAccounts;
        private String mIncomingPassword;
        private String mOutgoingPassword;
        private boolean mUseIncoming;

        /**
         * Constructor
         *
         * @param account
         *         The {@link Account} to ask the server passwords for. Never {@code null}.
         * @param accounts
         *         The (possibly empty) list of remaining accounts to ask passwords for. Never
         *         {@code null}.
         */
        public PasswordPromptDialog(Account account, List<Account> accounts) {
            mAccount = account;
            mRemainingAccounts = accounts;
        }

        @Override
        public void restore(Activity activity) {
            show((PEpImporterActivity) activity, true);
        }

        @Override
        public boolean retain() {
            if (mDialog != null) {
                // Retain entered passwords and checkbox state
                if (mIncomingPasswordView != null) {
                    mIncomingPassword = mIncomingPasswordView.getText().toString();
                }
                if (mOutgoingPasswordView != null) {
                    mOutgoingPassword = mOutgoingPasswordView.getText().toString();
                    mUseIncoming = mUseIncomingView.isChecked();
                }

                // Dismiss dialog
                mDialog.dismiss();

                // Clear all references to UI objects
                mDialog = null;
                mIncomingPasswordView = null;
                mOutgoingPasswordView = null;
                mUseIncomingView = null;
                return true;
            }
            return false;
        }

        public void show(PEpImporterActivity activity) {
            show(activity, false);
        }

        private void show(final PEpImporterActivity activity, boolean restore) {
            ServerSettings incoming = RemoteStore.decodeStoreUri(mAccount.getStoreUri());
            ServerSettings outgoing = Transport.decodeTransportUri(mAccount.getTransportUri());

            /*
             * Don't ask for the password to the outgoing server for WebDAV
             * accounts, because incoming and outgoing servers are identical for
             * this account type. Also don't ask when the username is missing.
             * Also don't ask when the AuthType is EXTERNAL or XOAUTH2
             */
            boolean configureOutgoingServer = AuthType.EXTERNAL != outgoing.authenticationType
                    && AuthType.XOAUTH2 != outgoing.authenticationType
                    && !(ServerSettings.Type.WebDAV == outgoing.type)
                    && outgoing.username != null
                    && !outgoing.username.isEmpty()
                    && (outgoing.password == null || outgoing.password
                    .isEmpty());

            boolean configureIncomingServer = AuthType.EXTERNAL != incoming.authenticationType
                    && AuthType.XOAUTH2 != incoming.authenticationType
                    && (incoming.password == null || incoming.password
                    .isEmpty());

            // Create a ScrollView that will be used as container for the whole layout
            final ScrollView scrollView = new ScrollView(activity);

            // Create the dialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.settings_import_activate_account_header));
            builder.setView(scrollView);
            builder.setPositiveButton(activity.getString(R.string.okay_action),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String incomingPassword = null;
                            if (mIncomingPasswordView != null) {
                                incomingPassword = mIncomingPasswordView.getText().toString();
                            }
                            String outgoingPassword = null;
                            if (mOutgoingPasswordView != null) {
                                outgoingPassword = (mUseIncomingView.isChecked()) ?
                                        incomingPassword : mOutgoingPasswordView.getText().toString();
                            }

                            dialog.dismiss();

                            // Set the server passwords in the background
                            SetPasswordsAsyncTask asyncTask = new SetPasswordsAsyncTask(activity, mAccount,
                                    incomingPassword, outgoingPassword, mRemainingAccounts);
                            activity.setNonConfigurationInstance(asyncTask);
                            asyncTask.execute();
                        }
                    });
            builder.setNegativeButton(activity.getString(R.string.cancel_action),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            activity.setNonConfigurationInstance(null);
                        }
                    });
            mDialog = builder.create();

            // Use the dialog's layout inflater so its theme is used (and not the activity's theme).
            View layout = mDialog.getLayoutInflater().inflate(R.layout.accounts_password_prompt, scrollView);

            // Set the intro text that tells the user what to do
            TextView intro = (TextView) layout.findViewById(R.id.password_prompt_intro);
            String serverPasswords = activity.getResources().getQuantityString(
                    R.plurals.settings_import_server_passwords,
                    (configureIncomingServer && configureOutgoingServer) ? 2 : 1);
            intro.setText(activity.getString(R.string.settings_import_activate_account_intro,
                    mAccount.getDescription(), serverPasswords));

            if (configureIncomingServer) {
                // Display the hostname of the incoming server
                TextView incomingText = (TextView) layout.findViewById(
                        R.id.password_prompt_incoming_server);
                incomingText.setText(activity.getString(R.string.settings_import_incoming_server,
                        incoming.host));

                mIncomingPasswordView = (EditText) layout.findViewById(R.id.incoming_server_password);
                mIncomingPasswordView.addTextChangedListener(this);
            } else {
                layout.findViewById(R.id.incoming_server_prompt).setVisibility(View.GONE);
            }

            if (configureOutgoingServer) {
                // Display the hostname of the outgoing server
                TextView outgoingText = (TextView) layout.findViewById(
                        R.id.password_prompt_outgoing_server);
                outgoingText.setText(activity.getString(R.string.settings_import_outgoing_server,
                        outgoing.host));

                mOutgoingPasswordView = (EditText) layout.findViewById(
                        R.id.outgoing_server_password);
                mOutgoingPasswordView.addTextChangedListener(this);

                mUseIncomingView = (CheckBox) layout.findViewById(
                        R.id.use_incoming_server_password);

                if (configureIncomingServer) {
                    mUseIncomingView.setChecked(true);
                    mUseIncomingView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked) {
                                mOutgoingPasswordView.setText(null);
                                mOutgoingPasswordView.setEnabled(false);
                            } else {
                                mOutgoingPasswordView.setText(mIncomingPasswordView.getText());
                                mOutgoingPasswordView.setEnabled(true);
                            }
                        }
                    });
                } else {
                    mUseIncomingView.setChecked(false);
                    mUseIncomingView.setVisibility(View.GONE);
                    mOutgoingPasswordView.setEnabled(true);
                }
            } else {
                layout.findViewById(R.id.outgoing_server_prompt).setVisibility(View.GONE);
            }

            // Show the dialog
            mDialog.show();

            // Restore the contents of the password boxes and the checkbox (if the dialog was
            // retained during a configuration change).
            if (restore) {
                if (configureIncomingServer) {
                    mIncomingPasswordView.setText(mIncomingPassword);
                }
                if (configureOutgoingServer) {
                    mOutgoingPasswordView.setText(mOutgoingPassword);
                    mUseIncomingView.setChecked(mUseIncoming);
                }
            } else {
                if (configureIncomingServer) {
                    // Trigger afterTextChanged() being called
                    // Work around this bug: https://code.google.com/p/android/issues/detail?id=6360
                    mIncomingPasswordView.setText(mIncomingPasswordView.getText());
                } else {
                    mOutgoingPasswordView.setText(mOutgoingPasswordView.getText());
                }
            }
        }

        @Override
        public void afterTextChanged(Editable arg0) {
            boolean enable = false;
            // Is the password box for the incoming server password empty?
            if (mIncomingPasswordView != null) {
                if (mIncomingPasswordView.getText().length() > 0) {
                    // Do we need to check the outgoing server password box?
                    if (mOutgoingPasswordView == null) {
                        enable = true;
                    }
                    // If the checkbox to use the incoming server password is checked we need to make
                    // sure that the password box for the outgoing server isn't empty.
                    else if (mUseIncomingView.isChecked() ||
                            mOutgoingPasswordView.getText().length() > 0) {
                        enable = true;
                    }
                }
            } else {
                enable = mOutgoingPasswordView.getText().length() > 0;
            }

            // Disable "OK" button if the user hasn't specified all necessary passwords.
            mDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enable);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not used
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Not used
        }
    }

    /**
     * Set the incoming/outgoing server password in the background.
     */
    private static class SetPasswordsAsyncTask extends ExtendedAsyncTask<Void, Void, Void> {
        private Account mAccount;
        private String mIncomingPassword;
        private String mOutgoingPassword;
        private List<Account> mRemainingAccounts;
        private Application mApplication;

        protected SetPasswordsAsyncTask(Activity activity, Account account,
                                        String incomingPassword, String outgoingPassword,
                                        List<Account> remainingAccounts) {
            super(activity);
            mAccount = account;
            mIncomingPassword = incomingPassword;
            mOutgoingPassword = outgoingPassword;
            mRemainingAccounts = remainingAccounts;
            mApplication = mActivity.getApplication();
        }

        @Override
        protected void showProgressDialog() {
            String title = mActivity.getString(R.string.settings_import_activate_account_header);
            int passwordCount = (mOutgoingPassword == null) ? 1 : 2;
            String message = mActivity.getResources().getQuantityString(
                    R.plurals.settings_import_setting_passwords, passwordCount);
            mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (mIncomingPassword != null) {
                    // Set incoming server password
                    String storeUri = mAccount.getStoreUri();
                    ServerSettings incoming = RemoteStore.decodeStoreUri(storeUri);
                    ServerSettings newIncoming = incoming.newPassword(mIncomingPassword);
                    String newStoreUri = RemoteStore.createStoreUri(newIncoming);
                    mAccount.setStoreUri(newStoreUri);
                }

                if (mOutgoingPassword != null) {
                    // Set outgoing server password
                    String transportUri = mAccount.getTransportUri();
                    ServerSettings outgoing = Transport.decodeTransportUri(transportUri);
                    ServerSettings newOutgoing = outgoing.newPassword(mOutgoingPassword);
                    String newTransportUri = Transport.createTransportUri(newOutgoing);
                    mAccount.setTransportUri(newTransportUri);
                }

                // Mark account as enabled
                mAccount.setEnabled(true);

                // Save the account settings
                mAccount.save(Preferences.getPreferences(mContext));

                // Start services if necessary
                K9.setServicesEnabled(mContext);

                // Get list of folders from remote server
                MessagingController.getInstance(mApplication).listFolders(mAccount, true, null);
            } catch (Exception e) {
                Timber.e(e, "Something went while setting account passwords");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            PEpImporterActivity activity = (PEpImporterActivity) mActivity;

            // Let the activity know that the background task is complete
            activity.setNonConfigurationInstance(null);

            activity.refresh();
            removeProgressDialog();

            if (mRemainingAccounts.size() > 0) {
                activity.promptForServerPasswords(mRemainingAccounts);
            }
            activity.onImportFinished();
        }
    }

    protected abstract void onImportFinished();

    /**
     * Handles exporting of global settings and/or accounts in a background thread.
     */
    public static class ExportAsyncTask extends ExtendedAsyncTask<Void, Void, Boolean> {
        private boolean mIncludeGlobals;
        private Set<String> mAccountUuids;
        private String mFileName;
        private Uri mUri;


        public ExportAsyncTask(Accounts activity, boolean includeGlobals,
                               List<String> accountUuids, Uri uri) {
            super(activity);
            mIncludeGlobals = includeGlobals;
            mUri = uri;
            if (accountUuids != null) {
                mAccountUuids = new HashSet<>(accountUuids);
            }
        }

        @Override
        protected void showProgressDialog() {
            String title = mContext.getString(R.string.settings_export_dialog_title);
            String message = mContext.getString(R.string.settings_exporting);
            mProgressDialog = ProgressDialog.show(mActivity, title, message, true);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (mUri == null) {
                    mFileName = SettingsExporter.exportToFile(mContext, mIncludeGlobals,
                            mAccountUuids);
                } else {
                    SettingsExporter.exportToUri(mContext, mIncludeGlobals, mAccountUuids, mUri);
                }

            } catch (SettingsImportExportException e) {
                Timber.w(e, "Exception during export");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            PEpImporterActivity activity = (PEpImporterActivity) mActivity;

            // Let the activity know that the background task is complete
            activity.setNonConfigurationInstance(null);

            removeProgressDialog();

            if (success) {
                if (mFileName != null) {
                    activity.showSimpleDialog(R.string.settings_export_success_header,
                            R.string.settings_export_success, mFileName);
                } else {
                    activity.showSimpleDialog(R.string.settings_export_success_header,
                            R.string.settings_export_success_generic);
                }
            } else {
                //TODO: better error messages
                activity.showSimpleDialog(R.string.settings_export_failed_header,
                        R.string.settings_export_failure);
            }
        }
    }
}
