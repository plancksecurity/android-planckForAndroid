package com.fsck.k9.pEp.ui.handshake;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.pEp.models.PEpIdentity;
import com.fsck.k9.pEp.ui.listeners.HandshakeListener;
import com.thoughtbot.expandablerecyclerview.ExpandableRecyclerViewAdapter;
import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;

import org.pEp.jniadapter.Identity;

import java.util.List;

public class TrustwordsAdapter extends ExpandableRecyclerViewAdapter<PEpIdentityViewHolder, HandshakeViewHolder> {

    private final Identity myself;
    private final HandshakeListener handshakeListener;
    private final View.OnClickListener onResetGreenClickListener;
    private final View.OnClickListener onResetRedClickListener;
    private final View.OnClickListener onHandshakeClickListener;
    private final List<Account> accounts;

    public TrustwordsAdapter(Identity myself, List<ExpandablePEpIdentity> groups, HandshakeListener handshakeListener,
                             View.OnClickListener onResetGreenClickListener, View.OnClickListener onResetRedClickListener,
                             View.OnClickListener onHandshakeClickListener, List<Account> accounts) {
        super(groups);
        this.myself = myself;
        this.handshakeListener = handshakeListener;
        this.onResetGreenClickListener = onResetGreenClickListener;
        this.onResetRedClickListener = onResetRedClickListener;
        this.onHandshakeClickListener = onHandshakeClickListener;
        this.accounts = accounts;
    }

    @Override
    public PEpIdentityViewHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pep_recipient_row, parent, false);
        return new PEpIdentityViewHolder(view, accounts);
    }

    @Override
    public HandshakeViewHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.handshake_item_view, parent, false);
        return new HandshakeViewHolder(view);
    }

    @Override
    public void onBindChildViewHolder(HandshakeViewHolder holder, int flatPosition, ExpandableGroup group, int childIndex) {
        final PEpIdentity identity = ((ExpandablePEpIdentity) group).getItems().get(childIndex);
        holder.render(myself, identity, handshakeListener);
    }

    @Override
    public void onBindGroupViewHolder(PEpIdentityViewHolder holder, int flatPosition, ExpandableGroup group) {
        PEpIdentity identity = ((ExpandablePEpIdentity) group).getItems().get(flatPosition);
        holder.render(flatPosition, identity, onResetGreenClickListener, onResetRedClickListener, onHandshakeClickListener);
    }
}
