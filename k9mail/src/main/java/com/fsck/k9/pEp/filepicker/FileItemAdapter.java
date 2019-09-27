package com.fsck.k9.pEp.filepicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;

/**
 * A simple adapter which also inserts a header item ".." to handle going up to the parent folder.
 * @param <T> the type which is used, for example a normal java File object.
 */
public class FileItemAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final LogicHandler<T> mLogic;
    protected SortedList<T> mList = null;

    public FileItemAdapter(@NonNull LogicHandler<T> logic) {
        this.mLogic = logic;
    }

    public void setList(@Nullable SortedList<T> list) {
        mList = list;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mLogic.onCreateViewHolder(parent, viewType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int headerPosition) {
        if (headerPosition != mList.size()) {
            mLogic.onBindViewHolder((AbstractFilePickerFragment<T>.DirViewHolder) viewHolder, headerPosition, mList.get(headerPosition));
        }
    }

    @Override
    public int getItemViewType(int headerPosition) {
        int pos = headerPosition - 1;
        if (pos >= 0) {
            return mLogic.getItemViewType(pos, mList.get(pos));
        } else {
            if (mList.size() == 0) {
                return 1;
            }
            return mLogic.getItemViewType(headerPosition, mList.get(headerPosition));
        }
    }

    @Override
    public int getItemCount() {
        if (mList == null) {
            return 0;
        }

        // header + count
        return 1 + mList.size();
    }

    /**
     * Get the item at the designated position in the adapter.
     *
     * @param position of item in adapter
     * @return null if position is zero (that means it's the ".." header), the item otherwise.
     */
    protected @Nullable T getItem(int position) {
        if (position == 0) {
            return null;
        }
        return mList.get(position - 1);
    }
}
