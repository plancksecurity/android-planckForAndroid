package com.fsck.k9.pEp.ui.viewholders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.IdentityClickListener;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.IdentityFlags;

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
        identityUserName.setText(keyItem.username);
        identityAddress.setText(keyItem.address);
        boolean flagged = IdentityFlags.PEPIdfNotForSync.value == keyItem.flags;
        isBlacklistedCheckbox.setChecked(flagged);
        isBlacklistedCheckbox.setOnClickListener(v -> {
            boolean checked = ((CheckBox) v).isChecked();
            identityClickListener.onClick(keyItem, checked);
        });


    }
}
