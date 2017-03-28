/*
Created by Helm  23/03/16.
*/


package com.fsck.k9.pEp.ui.keys;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.blacklist.KeyListItem;

import java.util.Comparator;
import java.util.List;

public class KeyItemAdapter extends RecyclerView.Adapter<KeyItemAdapter.ViewHolder> {

    private static final Comparator<KeyListItem> ALPHABETICAL_COMPARATOR = new Comparator<KeyListItem>() {
        @Override
        public int compare(KeyListItem a, KeyListItem b) {
            return a.getFpr().compareTo(b.getFpr());
        }
    };

    private final SortedList<KeyListItem> dataSet;
    private final OnKeyClickListener onKeyClickListener;
    private Comparator <KeyListItem> comparator;


    public KeyItemAdapter(List<KeyListItem> identities, OnKeyClickListener onKeyClickListener) {
        this.comparator = ALPHABETICAL_COMPARATOR;
        dataSet = new SortedList<>(KeyListItem.class, sortedListCallback);
        if (identities != null) {
            dataSet.addAll(identities);
        }
        this.onKeyClickListener = onKeyClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,
                                         int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.pep_key_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        KeyListItem item = dataSet.get(position);
        holder.render(item);
    }


    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView identityUserName;
        TextView identityAddress;

        CheckBox isBlacklistedCheckbox;
        View container;
        Context context;

        ViewHolder(View view) {
            super(view);
            context = view.getContext();
            identityUserName = ((TextView) view.findViewById(R.id.tvUsername));
            identityAddress = ((TextView) view.findViewById(R.id.tvAddress));
            isBlacklistedCheckbox = ((CheckBox) view.findViewById(R.id.checkboxIsBlacklisted));
            container = view.findViewById(R.id.recipientContainer);


        }

        void render(KeyListItem identity) {
            renderIdentity(identity);
        }

        private void renderIdentity(final KeyListItem keyItem) {
            final String fpr = keyItem.getFpr();
            String username = keyItem.getGpgUid();
            identityUserName.setText(username);
            String formattedFpr = PEpUtils.formatFpr(fpr);
            identityAddress.setText(formattedFpr);
            isBlacklistedCheckbox.setChecked(keyItem.isSelected());
            isBlacklistedCheckbox.setOnClickListener(v -> {
                boolean checked = ((CheckBox) v).isChecked();
                keyItem.setSelected(checked);
                int position = dataSet.indexOf(keyItem);
                dataSet.get(position).setSelected(checked);
                onKeyClickListener.onClick(keyItem, checked);
            });
        }
    }



    private final SortedList.Callback<KeyListItem> sortedListCallback = new SortedList.Callback<KeyListItem>() {

        @Override
        public void onInserted(int position, int count) {
            KeyItemAdapter.this.notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            KeyItemAdapter.this.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            KeyItemAdapter.this.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count) {
            KeyItemAdapter.this.notifyItemRangeChanged(position, count);
        }

        @Override
        public int compare(KeyListItem a, KeyListItem b) {
            return comparator.compare(a, b);
        }

        @Override
        public boolean areContentsTheSame(KeyListItem oldItem, KeyListItem newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(KeyListItem item1, KeyListItem item2) {
            return item1.getFpr().equals(item2.getFpr());
        }
    };

    public void add(KeyListItem item) {
        dataSet.add(item);
    }

    public void remove(KeyListItem item) {
        dataSet.remove(item);
    }

    public void add(List<KeyListItem> items) {
        dataSet.addAll(items);
    }

    public void remove(List<KeyListItem> items) {
        dataSet.beginBatchedUpdates();
        for (KeyListItem model : items) {
            dataSet.remove(model);
        }
        dataSet.endBatchedUpdates();
    }

    public void replaceAll(List<KeyListItem> models) {
        dataSet.beginBatchedUpdates();
        for (int i = dataSet.size() - 1; i >= 0; i--) {
            final KeyListItem model = dataSet.get(i);
            if (!models.contains(model)) {
                dataSet.remove(model);
            }
        }
        dataSet.addAll(models);
        dataSet.endBatchedUpdates();
    }

}
