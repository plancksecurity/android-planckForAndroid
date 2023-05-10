package com.fsck.k9.pEp.ui.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.pEp.models.PEpIdentity;

import java.util.ArrayList;
import java.util.List;

import foundation.pEp.jniadapter.Identity;
import foundation.pEp.jniadapter.Rating;
import security.planck.ui.PlanckUIUtils;

public class PEpIdentitiesAdapter extends RecyclerView.Adapter<PEpIdentitiesAdapter.ViewHolder> {
    private final ContextActions contextActions;

    private final View.OnClickListener onHandshakeClick;
    private final View.OnClickListener onResetGreenClick;
    private final View.OnClickListener onResetRedClick;

    private List<PEpIdentity> identities;
    private List<String> addressesOnDevice;

    public PEpIdentitiesAdapter(List<Account> accounts, View.OnClickListener onResetGreenClick,
                                View.OnClickListener onResetRedClick,
                                View.OnClickListener onHandshakeClick,
                                ContextActions contextActions) {
        initializeAddressesOnDevice(accounts);
        this.onResetGreenClick = onResetGreenClick;
        this.onResetRedClick = onResetRedClick;
        this.onHandshakeClick = onHandshakeClick;
        this.contextActions = contextActions;
    }

    private void initializeAddressesOnDevice(List<Account> accounts) {
        addressesOnDevice = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            String email = account.getEmail();
            addressesOnDevice.add(email);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.planck_recipient_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        PEpIdentity identity = identities.get(position);
        holder.render(position, identity);
    }


    @Override
    public int getItemCount() {
        return identities.size();
    }

    public void handshake(View view) {
        onHandshakeClick.onClick(view);
    }

    public PEpIdentity get(int position) {
        return identities.get(position);
    }

    public List<PEpIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(List<PEpIdentity> identities) {
        this.identities = identities;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private static final int KEY_RESET_CONTEXT_POSITION = 0;

        // each data item is just a string in this case
        public TextView identityUserName;
        public TextView identityAdress;

        public FrameLayout handshakeButton;
        private TextView handshakeText;
        public View container;
        public Context context;
        private ImageView badge;

        public ViewHolder(View view) {
            super(view);
            view.setOnCreateContextMenuListener(this);
            context = view.getContext();
            identityUserName = view.findViewById(R.id.tvUsername);
            identityAdress = view.findViewById(R.id.tvAddress);
            handshakeButton = view.findViewById(R.id.buttonHandshake);
            handshakeText = view.findViewById(R.id.handshake_button_text);
            container = view.findViewById(R.id.recipientContainer);
            badge = view.findViewById(R.id.status_badge);
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
                handshakeButton.setOnClickListener(onResetRedClick);
                badge.setVisibility(View.VISIBLE);
                identityUserName.setTextColor(context.getResources().getColor(R.color.white));
                identityAdress.setTextColor(context.getResources().getColor(R.color.white));
                handshakeText.setTextColor(context.getResources().getColor(R.color.white));
            } else if (rating.value >= Rating.pEpRatingTrusted.value){
                setHandshakeButtonVisibility(address, View.VISIBLE);
                handshakeButton.setOnClickListener(onResetGreenClick);
                handshakeText.setText(context.getString(R.string.pep_reset_trust));
                badge.setVisibility(View.VISIBLE);
                identityUserName.setTextColor(context.getResources().getColor(R.color.white));
                identityAdress.setTextColor(context.getResources().getColor(R.color.white));
                        handshakeText.setTextColor(context.getResources().getColor(R.color.white));
            } else if (rating.value == Rating.pEpRatingReliable.value){
                setHandshakeButtonVisibility(address, View.VISIBLE);
                handshakeButton.setOnClickListener(onHandshakeClick);
                handshakeText.setText(context.getString(R.string.pep_handshake));
                badge.setVisibility(View.VISIBLE);
                identityUserName.setTextColor(context.getResources().getColor(android.R.color.black));
                identityAdress.setTextColor(context.getResources().getColor(android.R.color.black));
                handshakeText.setTextColor(context.getResources().getColor(android.R.color.black));
            }
            Drawable drawableForRating = PlanckUIUtils.getDrawableForRatingRecipient(context, rating);
            badge.setImageDrawable(drawableForRating);
        }

        private void setHandshakeButtonVisibility(String address, int visibility) {
            if (addressesOnDevice.contains(address)) {
                handshakeButton.setVisibility(View.GONE);
            } else {
                handshakeButton.setVisibility(visibility);
            }
        }

        private void renderColor(Rating rating) {
            int colorCode = PlanckUIUtils.getRatingColor(context, rating);
            container.setBackgroundColor(colorCode);
        }

        private void setPosition(int position) {
            handshakeButton.setTag(position);
            container.setTag(position);
        }

        public void render(int position, PEpIdentity identity) {
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

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            Integer position = ((Integer) v.getTag());
            new MenuInflater(v.getContext()).inflate(R.menu.menu_pep_status_identity_context, menu);
            menu.getItem(KEY_RESET_CONTEXT_POSITION).setOnMenuItemClickListener(item -> {
                contextActions.keyReset(position);
                return true;
            });
        }
    }

    public interface ContextActions {
        void keyReset(int position);
    }
}
