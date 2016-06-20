/*
Created by Helm  17/06/16.
*/


package com.fsck.k9.pEp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.Toast;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpUtils;
import org.pEp.jniadapter.Identity;

public class ManualImportKeyDialogFragment extends DialogFragment {

    private static final String MESSAGE_DETAIL = "messageDetail";
    private static final String KEY_ADDRESS = "keyAddress";
    private static final String KEY_USERNAME = "keyUsername";
    private static final String KEY_FPR = "keyFingerprint";
    private String detail;
    private String address;
    private String username;
    private String fpr;

    public static ManualImportKeyDialogFragment newInstance(String detail, String fpr, String address, String username) {
        ManualImportKeyDialogFragment fragment = new ManualImportKeyDialogFragment();
        Bundle args = new Bundle();
        args.putString(MESSAGE_DETAIL, detail);
        args.putString(KEY_ADDRESS, address);
        args.putString(KEY_USERNAME, username);
        args.putString(KEY_FPR, fpr);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            Bundle arguments = getArguments();
            detail = arguments.getString(MESSAGE_DETAIL);
            address = arguments.getString(KEY_ADDRESS);
            username = arguments.getString(KEY_USERNAME);
            fpr = arguments.getString(KEY_FPR);
        }
        final Context context = getActivity().getApplicationContext();
        ContextThemeWrapper ctw = new ContextThemeWrapper(context, R.style.TextViewCustomFont);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);
        alertDialogBuilder.setTitle("Secret key replace")
                .setMessage(detail)
                .setCancelable(false)
                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context, "Key replaced", Toast.LENGTH_LONG).show();
                        Identity id = PEpUtils.createIdentity(new Address(address, username), context);
                        id.fpr = fpr;
                        ((K9) context).getpEpProvider().myself(id);
                    }
                }).setNegativeButton("Reject", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Key rejected", Toast.LENGTH_LONG).show();
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        return dialog;
    }
}
