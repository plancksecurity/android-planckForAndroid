package com.fsck.k9.planck.ui.viewholders;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.planck.ui.IdentityClickListener;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.IdentityFlags;

public class IdentityViewHolder extends RecyclerView.ViewHolder {
    private final IdentityClickListener identityClickListener;
    public TextView identityUserName;
    public TextView identityAddress;

    public CheckBox isBlacklistedCheckbox;
    public View container;
    public Context context;

    public IdentityViewHolder(View view, IdentityClickListener identityClickListener) {
        super(view);
        context = view.getContext();
        this.identityClickListener = identityClickListener;
        identityUserName = ((TextView) view.findViewById(R.id.tvUsername));
        identityAddress = ((TextView) view.findViewById(R.id.tvAddress));
        isBlacklistedCheckbox = ((CheckBox) view.findViewById(R.id.checkboxIsBlacklisted));
        container = view.findViewById(R.id.recipientContainer);
    }

    public void render(Identity identity) {
        renderIdentity(identity);
    }

    private void renderIdentity(final Identity keyItem) {
        identityUserName.setText(keyItem.address);
        identityAddress.setText(keyItem.address);
        boolean flagged = IdentityFlags.pEpIdfNotForSync.value != keyItem.flags;
        isBlacklistedCheckbox.setChecked(flagged);
        isBlacklistedCheckbox.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();
            identityClickListener.onClick(keyItem, checked);
        });


    }
}
