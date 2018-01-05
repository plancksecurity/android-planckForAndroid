package com.fsck.k9.pEp.ui.handshake;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.helper.ContactPicture;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.PEpContactBadge;
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
    private PEpContactBadge badge;
    private View.OnClickListener onResetGreenClickListener;
    private View.OnClickListener onResetRedClickListener;
    private View.OnClickListener onHandshakeClickListener;
    private List<String> addressesOnDevice;
    private Rating rating;

    PEpIdentityViewHolder(View view, List<Account> accounts) {
        super(view);
        context = view.getContext();
        identityUserName = view.findViewById(R.id.tvUsername);
        identityAdress = view.findViewById(R.id.tvAddress);
        handshakeButton = view.findViewById(R.id.buttonHandshake);
        handshakeText = view.findViewById(R.id.handshake_button_text);
        container = view.findViewById(R.id.recipientContainer);
        badge = view.findViewById(R.id.contact_badge);
        initializeAddressesOnDevice(accounts);
    }

    private void renderRating(String address, Rating rating) {
        if (rating.value != Rating.pEpRatingMistrust.value
                && rating.value < Rating.pEpRatingReliable.value) {
            setHandshakeButtonVisibility(address, View.GONE);
            badge.setVisibility(View.VISIBLE);
        }else if (rating.value == Rating.pEpRatingMistrust.value) {
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeText.setText(context.getString(R.string.pep_handshake));
            handshakeButton.setOnClickListener(onResetRedClickListener);
            badge.setVisibility(View.VISIBLE);
        } else if (rating.value >= Rating.pEpRatingTrusted.value){
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeButton.setOnClickListener(onResetGreenClickListener);
            handshakeText.setText(context.getString(R.string.pep_reset_trust));
            badge.setVisibility(View.VISIBLE);
        } else if (rating.value == Rating.pEpRatingReliable.value){
            setHandshakeButtonVisibility(address, View.VISIBLE);
            handshakeButton.setOnClickListener(onHandshakeClickListener);
            handshakeText.setText(context.getString(R.string.pep_handshake));
            badge.setVisibility(View.VISIBLE);
        }
        identityAdress.setText(rating.name());
        badge.setPepRating(rating, true);
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

    private void setPosition(int position) {
        handshakeButton.setTag(position);
        container.setTag(position);
    }

    void render(int position, PEpIdentity identity, View.OnClickListener onResetGreenClickListener,
                View.OnClickListener onResetRedClickListener, View.OnClickListener onHandshakeClickListener) {
        this.onResetGreenClickListener = onResetGreenClickListener;
        this.onResetRedClickListener = onResetRedClickListener;
        this.onHandshakeClickListener = onHandshakeClickListener;
        this.rating = identity.getRating();
        setOnGroupClickListener(null);
        renderRating(identity.address, identity.getRating());
        setPosition(position);
        renderIdentity(identity);
    }

    private void renderIdentity(Identity identity) {
        if (identity.username != null && !identity.address.equals(identity.username) && !
                identity.username.isEmpty()) {
            String username = extractUsername(identity);
            identityUserName.setText(username);
        } else {
            identityUserName.setText(identity.address);
        }
        setStatus();
        setContactPhotoOrPlaceholder(badge, identity);
    }

    private void setStatus() {
        PePUIArtefactCache artefactCache = PePUIArtefactCache.getInstance(itemView.getContext());
        String statusTitle = artefactCache.getTitle(rating);
        identityAdress.setText(statusTitle);
    }

    private void setContactPhotoOrPlaceholder(ImageView imageView, Identity identity) {
        ContactPicture.getContactPictureLoader(context).loadContactPicture(new Address(identity.address), imageView);
    }

    @NonNull
    private String extractUsername(Identity identity) {
        int index = identity.username.indexOf("(");
        return identity.username.substring(0, index - 1);
    }
}
