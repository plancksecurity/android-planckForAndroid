package com.fsck.k9.pEp.importAccount;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.SettingsActivity;
import com.fsck.k9.activity.misc.ExtendedAsyncTask;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpProviderFactory;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PepActivity;
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

import static com.fsck.k9.pEp.importAccount.PASSWORD.ACCOUNTS_ID;
import static com.fsck.k9.pEp.importAccount.PASSWORD.ACTIVITY_REQUEST_PROMPT_SERVER_PASSWORDS;

public abstract class PEpImporterActivity extends PepActivity {

    protected static final int ACTIVITY_REQUEST_PICK_SETTINGS_FILE = 1;
    protected static final int DIALOG_NO_FILE_MANAGER = 4;

    protected static final String CURRENT_ACCOUNT_UUID = "CURRENT_ACCOUNT_UUID";

    protected String currentAccountUuid;
    private ArrayList<String> disabledAccounts = new ArrayList<>();

    public void onSettingsImport() {
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

    private void promptServerPasswords(ArrayList<String> ids) {
        disabledAccounts = ids;
        // new ArrayList<>(ids) -> deep copy
        Intent intent = PasswordPromptKt.showPasswordDialog(this, new ArrayList<>(ids));
        startActivityForResult(intent, ACTIVITY_REQUEST_PROMPT_SERVER_PASSWORDS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Timber.i("onActivityResult requestCode = %d, resultCode = %s, data = %s", requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_PROMPT_SERVER_PASSWORDS) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                ArrayList<String> returnValue = data.getStringArrayListExtra(ACCOUNTS_ID);
                if (returnValue != null && !returnValue.isEmpty()) {
                    promptServerPasswords(returnValue);
                } else {
                    onImportFinished();
                }
            } else {
                promptServerPasswords(disabledAccounts);
            }
        }
    }

    protected abstract void onImportFinished();

    public static class ListImportContentsAsyncTask extends ExtendedAsyncTask<Boolean, Void, Boolean> {
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
        protected Boolean doInBackground(Boolean... booleans) {
            return importSettings();
        }

        @NonNull
        private Boolean importSettings() {
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

            onPostExecuteImportSettings(activity, success);

            activity.currentAccountUuid = "";
        }

        private void onPostExecuteImportSettings(PEpImporterActivity activity, Boolean success) {
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
            ArrayList<String> disabledAccounts = new ArrayList<>();
            for (SettingsImporter.AccountDescriptionPair accountPair : mImportResults.importedAccounts) {
                Account account = preferences.getAccount(accountPair.imported.uuid);
                if (account != null && !account.isEnabled()) {
                    disabledAccounts.add(account.getUuid());
                }
            }
            if (disabledAccounts.size() > 0) {
                activity.promptServerPasswords(disabledAccounts);
            } else {
                activity.setNonConfigurationInstance(null);
            }
        }
    }

    /**
     * Handles exporting of global settings and/or accounts in a background thread.
     */
    public static class ExportAsyncTask extends ExtendedAsyncTask<Void, Void, Boolean> {
        private boolean mIncludeGlobals;
        private Set<String> mAccountUuids;
        private String mFileName;
        private Uri mUri;


        public ExportAsyncTask(SettingsActivity activity, boolean includeGlobals,
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
