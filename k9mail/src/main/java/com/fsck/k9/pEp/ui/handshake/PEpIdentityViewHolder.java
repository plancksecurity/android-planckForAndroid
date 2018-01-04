package com.fsck.k9.pEp.ui.handshake;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.thoughtbot.expandablerecyclerview.viewholders.GroupViewHolder;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.List;

class PEpIdentityViewHolder extends GroupViewHolder {

    private TextView identityUserName;
    private TextView identityAdress;
    private FrameLayout handshakeButton;
    private TextView handshakeText;
    private View container;
    private Context context;
    private ImageView badge;
    private View.OnClickListener onResetGreenClickListener;
    private View.OnClickListener onResetRedClickListener;
    private View.OnClickListener onHandshakeClickListener;
    private List<String> addressesOnDevice;

    PEpIdentityViewHolder(View view, List<Account> accounts) {
        super(view);
        context = view.getContext();
        identityUserName = view.findViewById(R.id.tvUsername);
        identityAdress = view.findViewById(R.id.tvAddress);
        handshakeButton = view.findViewById(R.id.buttonHandshake);
        handshakeText = view.findViewById(R.id.handshake_button_text);
        container = view.findViewById(R.id.recipientContainer);
        badge = view.findViewById(R.id.status_badge);
        initializeAddressesOnDevice(accounts);
    }

    private void renderRating(String address, Rating rating) {
        renderColor(rating);
        if (rating.value != Rating.pEpRatingMistrust.value
                && rating.value < Rating.pEpRatingReliable.value) {
            setHandshakeButtonVisibility(address, View.GONE);
            badge.setVisibility(View.VISIBLE);
        }else if (rating.value == Rating.pEpRatingMistrust.value) {
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeText.setText(context.getString(R.string.pep_handshake));
            handshakeButton.setOnClickListener(onResetRedClickListener);
            badge.setVisibility(View.VISIBLE);
            identityUserName.setTextColor(context.getResources().getColor(R.color.white));
            identityAdress.setTextColor(context.getResources().getColor(R.color.white));
            handshakeText.setTextColor(context.getResources().getColor(R.color.white));
        } else if (rating.value >= Rating.pEpRatingTrusted.value){
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeButton.setOnClickListener(onResetGreenClickListener);
            handshakeText.setText(context.getString(R.string.pep_reset_trust));
            badge.setVisibility(View.VISIBLE);
            identityUserName.setTextColor(context.getResources().getColor(R.color.white));
            identityAdress.setTextColor(context.getResources().getColor(R.color.white));
            handshakeText.setTextColor(context.getResources().getColor(R.color.white));
        } else if (rating.value == Rating.pEpRatingReliable.value){
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeButton.setOnClickListener(onHandshakeClickListener);
            handshakeText.setText(context.getString(R.string.pep_handshake));
            badge.setVisibility(View.VISIBLE);
            identityUserName.setTextColor(context.getResources().getColor(R.color.openpgp_black));
            identityAdress.setTextColor(context.getResources().getColor(R.color.openpgp_black));
            handshakeText.setTextColor(context.getResources().getColor(R.color.openpgp_black));
        }
        Drawable drawableForRating = PEpUtils.getDrawableForRatingRecipient(context, rating);
        badge.setImageDrawable(drawableForRating);
    }

    private void initializeAddressesOnDevice(List<Account> accounts) {
        addressesOnDevice = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            String email = account.getEmail();
            addressesOnDevice.add(email);
        }
    }

    private void setHandshakeButtonVisibility(String address, int visibility) {
        if (addressesOnDevice.contains(address)) {
            handshakeButton.setVisibility(View.GONE);
        } else {
            handshakeButton.setVisibility(visibility);
        }
    }

    private void renderColor(Rating rating) {
        int colorCode = PePUIArtefactCache.getInstance(context).getColor(rating);
        container.setBackgroundColor(colorCode);
    }

    private void setPosition(int position) {
        handshakeButton.setTag(position);
        container.setTag(position);
    }

    void render(int position, PEpIdentity identity, View.OnClickListener onResetGreenClickListener,
                View.OnClickListener onResetRedClickListener, View.OnClickListener onHandshakeClickListener) {
        this.onResetGreenClickListener = onResetGreenClickListener;
        this.onResetRedClickListener = onResetRedClickListener;
        this.onHandshakeClickListener = onHandshakeClickListener;
        setOnGroupClickListener(null);
        renderRating(identity.address, identity.getRating());
        setPosition(position);
        renderIdentity(identity);
    }

    private void renderIdentity(Identity identity) {
        if (identity.username != null && !identity.address.equals(identity.username) && !
                identity.username.isEmpty()) {
            identityUserName.setText(identity.username);
            if (identity.address != null) {
                identityAdress.setVisibility(View.VISIBLE);
                identityAdress.setText(identity.address);
            } else {
                identityAdress.setVisibility(View.VISIBLE);
            }

        } else {
            identityUserName.setVisibility(View.GONE);
            identityAdress.setVisibility(View.VISIBLE);
            identityAdress.setText(identity.address);
        }
    }
}
