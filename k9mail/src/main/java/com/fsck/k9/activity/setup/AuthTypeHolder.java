package com.fsck.k9.activity.setup;

import android.content.res.Resources;

import com.fsck.k9.R;
import com.fsck.k9.mail.AuthType;

public class AuthTypeHolder {
    public final AuthType authType;
    private final Resources resources;
    private boolean insecure;

    public AuthTypeHolder(AuthType authType, Resources resources) {
        this.authType = authType;
        this.resources = resources;
    }

    public void setInsecure(boolean insecure) {
        this.insecure = insecure;
    }

    @Override
    public String toString() {
        if (authType == AuthType.EXTERNAL_PLAIN) {
            return String.format("%s & %s",
                    resources.getString(R.string.account_setup_auth_type_normal_password),
                    resources.getString(R.string.account_setup_auth_type_tls_client_certificate));
        }

        final int resourceId = resourceId();
        if (resourceId == 0) {
            return authType.name();
        } else {
            return resources.getString(resourceId);
        }
    }

    private int resourceId() {
        switch (authType) {
            case PLAIN:
                if (insecure) {
                    return R.string.account_setup_auth_type_insecure_password;
                } else {
                    return R.string.account_setup_auth_type_normal_password;
                }
            case CRAM_MD5:
                return R.string.account_setup_auth_type_encrypted_password;
            case XOAUTH2:
                return R.string.account_setup_auth_type_xoauth2;
            case EXTERNAL:
                return R.string.account_setup_auth_type_tls_client_certificate;

            default:
                return 0;
        }
    }
}
