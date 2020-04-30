package com.fsck.k9.activity.folderlist;

import android.widget.Filter;

import com.fsck.k9.activity.FolderInfoHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

/**
 * Filter to search for occurrences of the search-expression in any place of the
 * folder-name instead of doing just a prefix-search.
 *
 * @author Marcus@Wolschon.biz
 */
public class FolderListFilter extends Filter {
    private List<FolderInfoHolder> folders;
    private FolderFilterListener listener;

    FolderListFilter(FolderFilterListener listener) {
        this.listener = listener;
        this.folders = new ArrayList<>();
    }

    public void setFolders(List<FolderInfoHolder> folders) {
      this.folders = folders;
    }

    /**
     * Do the actual search.
     * {@inheritDoc}
     *
     * @see #publishResults(CharSequence, FilterResults)
     */
    @Override
    protected FilterResults performFiltering(CharSequence searchTerm) {
        FilterResults results = new FilterResults();

        Locale locale = Locale.getDefault();
        if ((searchTerm == null) || (searchTerm.length() == 0)) {
            List<FolderInfoHolder> list = new ArrayList<>(folders);
            results.values = list;
            results.count = list.size();
        } else {
            final String searchTermString = searchTerm.toString().toLowerCase(locale);
            final String[] words = searchTermString.split(" ");
            final int wordCount = words.length;

            final List<FolderInfoHolder> newValues = new ArrayList<>();

            for (final FolderInfoHolder value : folders) {
                if (value.displayName == null) {
                    continue;
                }
                final String valueText = value.displayName.toLowerCase(locale);

                for (String word : words) {
                    if (valueText.contains(word)) {
                        newValues.add(value);
                        break;
                    }
                }
            }

            results.values = newValues;
            results.count = newValues.size();
        }

        return results;
    }

    /**
     * Publish the results to the user-interface.
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        List<FolderInfoHolder> values = Collections.unmodifiableList((ArrayList<FolderInfoHolder>) results.values);
        listener.publishResults(values);
    }
}