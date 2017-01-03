package com.fsck.k9.pEp.ui.adapters;

import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.IdentityClickListener;
import com.fsck.k9.pEp.ui.viewholders.IdentityViewHolder;

import org.pEp.jniadapter.Identity;

import java.util.Comparator;
import java.util.List;

public class IdentitiesAdapter extends RecyclerView.Adapter<IdentityViewHolder> {

    private static final Comparator<Identity> ALPHABETICAL_COMPARATOR = (a, b) -> a.address.compareTo(b.address);

    private final SortedList<Identity> dataSet;
    private final IdentityClickListener identityClickListener;
    private Comparator<Identity> comparator;

    public IdentitiesAdapter(List<Identity> identities, IdentityClickListener identityClickListener) {
        this.comparator = ALPHABETICAL_COMPARATOR;
        dataSet = new SortedList<>(Identity.class, getSortedlistCallback());
        if (identities != null) {
            dataSet.addAll(identities);
        }
        this.identityClickListener = identityClickListener;
    }

    @Override
    public IdentityViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pep_key_row, parent, false);

        return new IdentityViewHolder(v, identityClickListener);
    }

    @Override
    public void onBindViewHolder(IdentityViewHolder holder, final int position) {
        Identity item = dataSet.get(position);
        holder.render(item);
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void add(Identity item) {
        dataSet.add(item);
    }

    public void remove(Identity item) {
        dataSet.remove(item);
    }

    public void add(List<Identity> items) {
        dataSet.addAll(items);
    }

    public void remove(List<Identity> items) {
        dataSet.beginBatchedUpdates();
        for (Identity model : items) {
            dataSet.remove(model);
        }
        dataSet.endBatchedUpdates();
    }

    public void replaceAll(List<Identity> models) {
        dataSet.beginBatchedUpdates();
        for (int i = dataSet.size() - 1; i >= 0; i--) {
            final Identity model = dataSet.get(i);
            if (!models.contains(model)) {
                dataSet.remove(model);
            }
        }
        dataSet.addAll(models);
        dataSet.endBatchedUpdates();
    }

    private SortedList.Callback<Identity> getSortedlistCallback() {
        return new SortedList.Callback<Identity>() {

            @Override
            public void onInserted(int position, int count) {
                IdentitiesAdapter.this.notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                IdentitiesAdapter.this.notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                IdentitiesAdapter.this.notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count) {
                IdentitiesAdapter.this.notifyItemRangeChanged(position, count);
            }

            @Override
            public int compare(Identity a, Identity b) {
                return comparator.compare(a, b);
            }

            @Override
            public boolean areContentsTheSame(Identity oldItem, Identity newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(Identity item1, Identity item2) {
                return item1.fpr.equals(item2.address);
            }
        };
    }

}
