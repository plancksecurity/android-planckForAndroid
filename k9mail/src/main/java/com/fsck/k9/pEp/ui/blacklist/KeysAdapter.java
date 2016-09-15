/*
Created by Helm  23/03/16.
*/


package com.fsck.k9.pEp.ui.blacklist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;

import java.util.List;

class KeysAdapter extends RecyclerView.Adapter<KeysAdapter.ViewHolder> {
    private final Context context;
    private final List<KeyListItem> identities;
    private ViewHolder viewHolder;
    private PEpProvider pEp;


    public KeysAdapter(Context context,
                       List<KeyListItem> identities) {
        this.identities = identities;
        this.context = context;
        pEp = ((K9) context.getApplicationContext()).getpEpProvider();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pep_key_row, parent, false);

        viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        KeyListItem item = identities.get(position);
        holder.render(item);
    }


    @Override
    public int getItemCount() {
        return identities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView identityUserName;
        public TextView identityAddress;

        public CheckBox isBlacklistedCheckbox;
        public View container;
        public Context context;

        public ViewHolder(View view) {
            super(view);
            context = view.getContext();
            identityUserName = ((TextView) view.findViewById(R.id.tvUsername));
            identityAddress = ((TextView) view.findViewById(R.id.tvAddress));
            isBlacklistedCheckbox = ((CheckBox) view.findViewById(R.id.checkboxIsBlacklisted));
            container = view.findViewById(R.id.recipientContainer);
        }

        public void render(KeyListItem identity) {

            renderIdentity(identity);
        }

        private void renderIdentity(KeyListItem keyItem) {
            String fpr = keyItem.getFpr();
            String username = keyItem.getGpgUid();
            identityUserName.setText(username);
            String formattedFpr = PEpUtils.formatFpr(fpr);
            identityAddress.setText(formattedFpr);


        }
    }

}
