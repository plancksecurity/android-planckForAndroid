/*
Created by Helm  23/03/16.
*/


package com.fsck.k9.pEp.ui.blacklist;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;

import java.util.Comparator;
import java.util.List;

public class KeysAdapter extends RecyclerView.Adapter<KeysAdapter.ViewHolder> {

    private static final Comparator<KeyListItem> ALPHABETICAL_COMPARATOR = new Comparator<KeyListItem>() {
        @Override
        public int compare(KeyListItem a, KeyListItem b) {
            return a.getFpr().compareTo(b.getFpr());
        }
    };

    private final Context context;
//    private final List<KeyListItem> identities;
    private final SortedList<KeyListItem> dataSet;
    private ViewHolder viewHolder;
    private PEpProvider pEp;
    private Comparator <KeyListItem> comparator;


    public KeysAdapter(Context context,
                       List<KeyListItem> identities) {
//        this.identities = identities;
        this.context = context;
        pEp = ((K9) context.getApplicationContext()).getpEpProvider();
        this.comparator = ALPHABETICAL_COMPARATOR;
        dataSet = new SortedList<KeyListItem>(KeyListItem.class, sortedListCallback);
        dataSet.addAll(identities);
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
        KeyListItem item = dataSet.get(position);
        holder.render(item);
    }


    @Override
    public int getItemCount() {
        return dataSet.size();
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

        private void renderIdentity(final KeyListItem keyItem) {
            final String fpr = keyItem.getFpr();
            String username = keyItem.getGpgUid();
            identityUserName.setText(username);
            String formattedFpr = PEpUtils.formatFpr(fpr);
            identityAddress.setText(formattedFpr);
            isBlacklistedCheckbox.setChecked(keyItem.isSelected());
            isBlacklistedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        pEp.addToBlacklist(fpr);
                        keyItem.setSelected(true);
                    } else {
                        pEp.deleteFromBlacklist(fpr);
                        keyItem.setSelected(false);

                    }
                }
            });


        }
    }



    private final SortedList.Callback<KeyListItem> sortedListCallback = new SortedList.Callback<KeyListItem>() {

        @Override
        public void onInserted(int position, int count) {
            KeysAdapter.this.notifyItemRangeInserted(position, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            KeysAdapter.this.notifyItemRangeRemoved(position, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            KeysAdapter.this.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onChanged(int position, int count) {
            KeysAdapter.this.notifyItemRangeChanged(position, count);
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
            return item1.fpr.equals(item2.getFpr());
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
