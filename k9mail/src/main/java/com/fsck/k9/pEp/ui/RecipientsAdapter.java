/*
Created by Helm  23/03/16.
*/


package com.fsck.k9.pEp.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.fsck.k9.R;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

import java.util.List;

class RecipientsAdapter extends RecyclerView.Adapter<RecipientsAdapter.ViewHolder> {
    private final PEpProvider pEp;

    private final Activity context;
    private final List<Identity> identities;
    private final ChangeColorListener listener;
    private ViewHolder viewHolder;

    private final String myself;

    private View.OnClickListener onHandshakeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int partnerPosition = ((Integer) v.getTag());
            Identity id = identities.get(partnerPosition);
            Identity myId = PEpUtils.createIdentity(new Address(myself), context);
            id = pEp.updateIdentity(id);
            myId = pEp.myself(myId);

            String trust;
            String myTrust = PEpUtils.getShortTrustWords(pEp, myId);
            String theirTrust = PEpUtils.getShortTrustWords(pEp, id);
            if (myId.fpr.compareTo(id.fpr) > 0) {
                trust = theirTrust + myTrust;
            } else {
                trust = myTrust + theirTrust;
            }
            Log.i("RecipientsAdapter", "onClick " + trust);

            PEpTrustwords.actionRequestHandshake(context, trust, myself, partnerPosition);
        }
    };

    private View.OnClickListener onResetClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = ((Integer) v.getTag());
            Identity id = identities.get(position);
            id = pEp.updateIdentity(id);
            Log.i("RecipientsAdapter", "onResetClick " + id.address);
            pEp.resetTrust(id);
            notifyDataSetChanged();
            listener.colorChanged(Color.pEpRatingReliable);

        }
    };

    public RecipientsAdapter(Activity context,
                             List<Identity> identities,
                             PEpProvider pEp,
                             String myself,
                             ChangeColorListener listener) {
        this.pEp = pEp;
        this.identities = identities;
        this.context = context;
        this.myself = myself;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pep_recipient_row, parent, false);

        viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Identity identity = identities.get(position);
        holder.render(position, identity);
    }


    @Override
    public int getItemCount() {
        return identities.size();
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

        private void renderButton(Color color) {
            if (color.value != Color.pEpRatingRed.value
                    && color.value < Color.pEpRatingYellow.value) {
                handshakeButton.setVisibility(View.GONE);
            } else if (color.value == Color.pEpRatingRed.value
                    || color.value >= Color.pEpRatingGreen.value){
                handshakeButton.setText(context.getString(R.string.pep_reset_trust));
                handshakeButton.setOnClickListener(onResetClick);
            } else if (color.value == Color.pEpRatingYellow.value){
                handshakeButton.setText(context.getString(R.string.pep_handshake));
                handshakeButton.setOnClickListener(onHandshakeClick);
            }
        }

        private void renderColor(Color color) {
            int colorCode = PePUIArtefactCache.getInstance(context).getColor(color);
            container.setBackgroundColor(colorCode);
        }

        private void setPosition(int position) {
            handshakeButton.setTag(position);
            container.setTag(position);
        }

        public void render(int position, Identity identity) {
            Color color = pEp.identityColor(identity);
            renderColor(color);
            renderButton(color);
            setPosition(position);
            renderIdentity(identity, color);
        }

        private void renderIdentity(Identity identity, Color color) {
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
