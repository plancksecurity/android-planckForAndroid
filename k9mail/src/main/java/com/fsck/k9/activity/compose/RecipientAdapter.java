package com.fsck.k9.activity.compose;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.pEp.infrastructure.modules.ContactLoaderModule;
import com.fsck.k9.ui.contacts.ContactPictureLoader;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;
import com.fsck.k9.view.ThemeUtils;


public class RecipientAdapter extends BaseAdapter implements Filterable {
    private final Context context;
    private List<Recipient> recipients;
    private String highlight;

    private ContactPictureLoader contactPictureLoader;

    public RecipientAdapter(Context context) {
        super();
        this.context = context;
        this.contactPictureLoader = new ContactLoaderModule(context).provideContactPictureLoader();
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
        notifyDataSetChanged();
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    @Override
    public int getCount() {
        return recipients == null ? 0 : recipients.size();
    }

    @Override
    public Recipient getItem(int position) {
        return recipients == null ? null : recipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = newView(parent);
        }

        Recipient recipient = getItem(position);
        bindView(view, recipient);

        return view;
    }

    public View newView(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipient_dropdown_item, parent, false);

        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);

        return view;
    }

    public void bindView(View view, Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();

        holder.name.setText(highlightText(recipient.getDisplayNameOrUnknown(context)));

        String address = recipient.address.getAddress();
        holder.email.setText(highlightText(address));

        contactPictureLoader.setContactPicture(holder.photo, recipient);

        Integer cryptoStatusRes = null, cryptoStatusColor = null;
        RecipientCryptoStatus cryptoStatus = recipient.getCryptoStatus();
        switch (cryptoStatus) {
            case AVAILABLE_TRUSTED: {
                cryptoStatusRes = R.drawable.status_lock_dots_3;
                cryptoStatusColor = ThemeUtils.getStyledColor(context, R.attr.openpgp_green);
                break;
            }
            case AVAILABLE_UNTRUSTED: {
                cryptoStatusRes = R.drawable.status_lock_dots_2;
                cryptoStatusColor = ThemeUtils.getStyledColor(context, R.attr.openpgp_orange);
                break;
            }
            case UNAVAILABLE: {
                cryptoStatusRes = R.drawable.status_lock_disabled_dots_1;
                cryptoStatusColor = ThemeUtils.getStyledColor(context, R.attr.openpgp_red);
                break;
            }
        }

        if (cryptoStatusRes != null) {
            Drawable drawable = ContextCompat.getDrawable(context, cryptoStatusRes);
            DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), cryptoStatusColor);
            holder.cryptoStatusIcon.setImageDrawable(drawable);
            holder.cryptoStatus.setVisibility(View.VISIBLE);
        } else {
            holder.cryptoStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (recipients == null) {
                    return null;
                }

                FilterResults result = new FilterResults();
                result.values = recipients;
                result.count = recipients.size();

                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }


    private static class RecipientTokenHolder {
        public final TextView name;
        public final TextView email;
        public final ImageView photo;
        public final View cryptoStatus;
        public final ImageView cryptoStatusIcon;


        public RecipientTokenHolder(View view) {
            name = (TextView) view.findViewById(R.id.text1);
            email = (TextView) view.findViewById(R.id.text2);
            photo = (ImageView) view.findViewById(R.id.contact_photo);
            cryptoStatus = view.findViewById(R.id.contact_crypto_status);
            cryptoStatusIcon = (ImageView) view.findViewById(R.id.contact_crypto_status_icon);
        }
    }

    public Spannable highlightText(String text) {
        Spannable highlightedSpannable = Spannable.Factory.getInstance().newSpannable(text);

        if (highlight == null) {
            return highlightedSpannable;
        }

        Pattern pattern = Pattern.compile(highlight, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            highlightedSpannable.setSpan(
                    new ForegroundColorSpan(context.getResources().getColor(android.R.color.holo_blue_light)),
                    matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return highlightedSpannable;
    }

}
