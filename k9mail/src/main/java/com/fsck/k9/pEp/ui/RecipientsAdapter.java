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
    private ViewHolder viewHolder;

    private final String myself;

    private View.OnClickListener onHandshakeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = ((Integer) v.getTag());
            Identity id = identities.get(position);
            Identity myId = PEpUtils.createIdentity(new Address(myself), context);
            id = pEp.updateIdentity(id);
            myId = pEp.updateIdentity(myId);

            String trust;
            pEp.myself(myId);
            String myTrust = PEpUtils.getShortTrustWords(pEp, myId);
            String theirTrust = PEpUtils.getShortTrustWords(pEp, id);if (myId.fpr.compareTo(id.fpr) > 0) {
                trust = theirTrust + myTrust;
            } else {
                trust = myTrust + theirTrust;
            }
            Log.i("RecipientsAdapter", "onClick " + trust);

            PEpTrustwords.actionRequestHandshake(context, trust, position);
        }
    };

    public RecipientsAdapter(Activity context,
                             List<Identity> identities,
                             PEpProvider pEp,
                             String myself) {
        this.pEp = pEp;
        this.identities = identities;
        this.context = context;
        this.myself = myself;
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
        Color color = pEp.identityColor(identity);
        holder.render(color, position, getShownName(identity));
    }

    private String getShownName(Identity identity) {
        if (identity.username == null || identity.username.equals("")) {
            return identity.address;
        } else {
            return identity.username;
        }
    }


    @Override
    public int getItemCount() {
        return identities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView contactEmail;
        public Button handshakeButton;
        public View container;
        public Context context;

        public ViewHolder(View view) {
            super(view);
            context = view.getContext();
            contactEmail = ((TextView) view.findViewById(R.id.tvEmail));
            handshakeButton = ((Button) view.findViewById(R.id.buttonHandshake));
            container = view.findViewById(R.id.recipientContainer);
        }

        private void renderButton(Color color) {
            if (color.value != Color.pEpRatingYellow.value) {
                handshakeButton.setVisibility(View.GONE);
            } else {
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

        public void render(Color color, int position, String shownName) {
            renderColor(color);
            renderButton(color);
            setPosition(position);
            renderShownName(shownName, color);
        }

        private void renderShownName(String shownName, Color color) {
            contactEmail.setText(shownName);
            if (color.value >= Color.pEpRatingGreen.value) {
                contactEmail.setTextColor(android.graphics.Color.WHITE);
            }
        }
    }

}
