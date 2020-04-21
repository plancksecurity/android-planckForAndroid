 package com.fsck.k9.pEp.importAccount;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.misc.NonConfigurationInstance;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.RemoteStore;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Ask the user for the incoming/outgoing server passwords.
 */
public class PasswordPromptDialog implements NonConfigurationInstance, TextWatcher {
    private AlertDialog mDialog;
    private EditText incomingPasswordEditText;
    private TextInputLayout incomingTIL;
    private EditText outgoingPasswordEditText;
    private TextInputLayout outgoingTIL;

    private TextView intro;
    private CheckBox useIncomingCheckbox;

    private Account mAccount;
    private List<Account> remainingAccounts;

    private String incomingPassword;
    private String outgoingPassword;

    private boolean useIncoming;
    private boolean configureOutgoingServer, configureIncomingServer;

    /**
     * Constructor
     *
     * @param account  The {@link Account} to ask the server passwords for. Never {@code null}.
     * @param accounts The (possibly empty) list of remaining accounts to ask passwords for. Never
     *                 {@code null}.
     */
    public PasswordPromptDialog(Account account, List<Account> accounts) {
        mAccount = account;
        remainingAccounts = accounts;
    }

    @Override
    public void restore(Activity activity) {
        show((PEpImporterActivity) activity, true);
    }

    @Override
    public boolean retain() {
        if (mDialog != null) {
            // Retain entered passwords and checkbox state
            if (incomingPasswordEditText != null) {
                incomingPassword = incomingPasswordEditText.getText().toString();
            }
            if (outgoingPasswordEditText != null) {
                outgoingPassword = outgoingPasswordEditText.getText().toString();
                useIncoming = useIncomingCheckbox.isChecked();
            }

            // Dismiss dialog
            mDialog.dismiss();

            // Clear all references to UI objects
            mDialog = null;
            incomingPasswordEditText = null;
            outgoingPasswordEditText = null;
            useIncomingCheckbox = null;
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
        configureOutgoingServer = AuthType.EXTERNAL != outgoing.authenticationType
                && AuthType.XOAUTH2 != outgoing.authenticationType
                && !(ServerSettings.Type.WebDAV == outgoing.type)
                && outgoing.username != null
                && !outgoing.username.isEmpty()
                && (outgoing.password == null || outgoing.password
                .isEmpty());

        configureIncomingServer = AuthType.EXTERNAL != incoming.authenticationType
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
                (dialog, which) -> {
                    String incomingPassword = null;
                    if (configureIncomingServer) {
                        incomingPassword = incomingPasswordEditText.getText().toString();
                    }
                    String outgoingPassword = null;
                    if (configureOutgoingServer) {
                        outgoingPassword = (useIncomingCheckbox.isChecked()) ?
                                incomingPassword : outgoingPasswordEditText.getText().toString();
                    }

                    dialog.dismiss();

                    // Set the server passwords in the background
                    SetPasswordsKt.setPasswords(activity,
                            mAccount,
                            incomingPassword,
                            outgoingPassword,
                            remainingAccounts);
                });
        builder.setNegativeButton(activity.getString(R.string.cancel_action),
                (dialog, which) -> {
                    dialog.dismiss();
                    activity.setNonConfigurationInstance(null);
                });
        mDialog = builder.create();

        // Use the dialog's layout inflater so its theme is used (and not the activity's theme).
        setView(mDialog, scrollView);


        setTexts(activity);
        setCheckbox();
        configureIncomingServer(incoming, activity);
        configureOutgoingServer(outgoing, activity);


        // Show the dialog
        mDialog.show();

        // Restore the contents of the password boxes and the checkbox (if the dialog was
        // retained during a configuration change).
        if (restore) {
            if (configureIncomingServer) {
                incomingPasswordEditText.setText(incomingPassword);
            }
            if (configureOutgoingServer) {
                outgoingPasswordEditText.setText(outgoingPassword);
                useIncomingCheckbox.setChecked(useIncoming);
            }
        } else {
            if (configureIncomingServer) {
                incomingPasswordEditText.setText(incomingPasswordEditText.getText());
            } else {
                outgoingPasswordEditText.setText(outgoingPasswordEditText.getText());
            }
        }
    }

    private void setView(AlertDialog mDialog, ScrollView scrollView) {
        View layout = mDialog.getLayoutInflater().inflate(R.layout.accounts_password_prompt, scrollView);
        intro = layout.findViewById(R.id.password_prompt_intro);
        useIncomingCheckbox = layout.findViewById(R.id.use_incoming_server_password);
        incomingPasswordEditText = layout.findViewById(R.id.incoming_server_password);
        outgoingPasswordEditText = layout.findViewById(R.id.outgoing_server_password);
        incomingTIL = layout.findViewById(R.id.incomingTIL);
        outgoingTIL = layout.findViewById(R.id.outgoingTIL);
    }

    private void setTexts(Activity activity) {
        String serverPasswords = activity.getResources()
                .getQuantityString(R.plurals.settings_import_server_passwords,
                (configureIncomingServer && configureOutgoingServer) ? 2 : 1);
        intro.setText(activity.getString(R.string.settings_import_activate_account_intro,
                mAccount.getDescription(), serverPasswords));
    }

    private void setCheckbox() {
        useIncomingCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                outgoingPasswordEditText.setText(null);
                outgoingPasswordEditText.setEnabled(false);
                outgoingTIL.setVisibility(View.INVISIBLE);
            } else {
                outgoingPasswordEditText.setText(incomingPasswordEditText.getText());
                outgoingPasswordEditText.setEnabled(true);
                outgoingTIL.setVisibility(VISIBLE);
            }
        });
        useIncomingCheckbox.setVisibility(configureIncomingServer && configureOutgoingServer ? VISIBLE : GONE);
        useIncomingCheckbox.setChecked(configureIncomingServer);
    }

    private void configureIncomingServer(ServerSettings incoming, Activity activity) {
        if (configureIncomingServer) {
            incomingTIL.setHint(activity.getString(R.string.settings_import_incoming_server, incoming.host));
            incomingPasswordEditText.addTextChangedListener(this);
        } else {
            incomingTIL.setVisibility(GONE);
        }
    }

    private void configureOutgoingServer(ServerSettings outgoing, PEpImporterActivity activity) {
        if (configureOutgoingServer) {
            outgoingTIL.setHint(activity.getString(R.string.settings_import_outgoing_server, outgoing.host));
            outgoingPasswordEditText.addTextChangedListener(this);
        } else {
            outgoingTIL.setVisibility(GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable arg0) {
        boolean enable = false;
        // Is the password box for the incoming server password empty?
        if (configureIncomingServer) {
            if (incomingPasswordEditText.getText().length() > 0) {
                if (!configureOutgoingServer) {
                    enable = true;
                } else if (useIncomingCheckbox.isChecked() ||
                        outgoingPasswordEditText.getText().length() > 0) {
                    enable = true;
                }
            }
        } else {
            enable = outgoingPasswordEditText.getText().length() > 0;
        }

        // Disable "OK" button if the user hasn't specified all necessary passwords.
        if (mDialog != null && mDialog.getButton(DialogInterface.BUTTON_POSITIVE) != null)
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