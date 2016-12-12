/*
Created by Helm  23/03/16.
*/


package com.fsck.k9.pEp.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.models.PEpIdentity;

import org.pEp.jniadapter.Identity;
import org.pEp.jniadapter.Rating;

import java.util.List;

public class PEpIdentitiesAdapter extends RecyclerView.Adapter<PEpIdentitiesAdapter.ViewHolder> {
    private final List<PEpIdentity> identities;

    private View.OnClickListener onHandshakeClick;
    private View.OnClickListener onResetGreenClick;
    private View.OnClickListener onResetRedClick;

    public PEpIdentitiesAdapter(List<PEpIdentity> identities,
                                View.OnClickListener onResetGreenClick,
                                View.OnClickListener onResetRedClick,
                                View.OnClickListener onHandshakeClick) {
        this.identities = identities;
        this.onResetGreenClick = onResetGreenClick;
        this.onResetRedClick = onResetRedClick;
        this.onHandshakeClick = onHandshakeClick;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pep_recipient_row, parent, false);
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

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView identityUserName;
        public TextView identityAdress;

        public Button handshakeButton;
        public View container;
        public Context context;

        public ViewHolder(View view) {
            super(view);
            context = view.getContext();
            identityUserName = ((TextView) view.findViewById(R.id.tvUsername));
            identityAdress = ((TextView) view.findViewById(R.id.tvAddress));
            handshakeButton = ((Button) view.findViewById(R.id.buttonHandshake));
            container = view.findViewById(R.id.recipientContainer);
        }

        private void renderButton(Rating rating) {
            if (rating.value != Rating.pEpRatingMistrust.value
                    && rating.value < Rating.pEpRatingReliable.value) {
                handshakeButton.setVisibility(View.GONE);
            }else if (rating.value == Rating.pEpRatingMistrust.value) {
                handshakeButton.setVisibility(View.VISIBLE);
                handshakeButton.setText(context.getString(R.string.pep_handshake));
                handshakeButton.setOnClickListener(onResetRedClick);
            } else if (rating.value >= Rating.pEpRatingTrusted.value){
                handshakeButton.setVisibility(View.VISIBLE);
                handshakeButton.setText(context.getString(R.string.pep_reset_trust));
                handshakeButton.setOnClickListener(onResetGreenClick);
            } else if (rating.value == Rating.pEpRatingReliable.value){
                handshakeButton.setVisibility(View.VISIBLE);
                handshakeButton.setText(context.getString(R.string.pep_handshake));
                handshakeButton.setOnClickListener(onHandshakeClick);
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

        public void render(int position, PEpIdentity identity) {
            renderColor(identity.getRating());
            renderButton(identity.getRating());
            setPosition(position);
            renderIdentity(identity);
        }

        private void renderIdentity(Identity identity) {
            if (identity.username != null && !identity.address.equals(identity.username)) {
                identityUserName.setText(identity.username);
                if (identity.address != null) identityAdress.setText(identity.address);

            } else {
                identityAdress.setVisibility(View.GONE);
                if (identity.address != null) identityUserName.setText(identity.address);

            }
        }
    }
}
