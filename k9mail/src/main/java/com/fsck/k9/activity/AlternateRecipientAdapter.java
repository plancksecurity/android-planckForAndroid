package com.fsck.k9.activity;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.RatedRecipient;
import com.fsck.k9.activity.compose.Recipient;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.ui.PEpContactBadge;
import com.fsck.k9.pEp.ui.tools.ThemeManager;
import com.fsck.k9.ui.contacts.ContactPictureLoader;
import com.fsck.k9.view.ThemeUtils;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import foundation.pEp.jniadapter.Rating;


public class AlternateRecipientAdapter extends BaseAdapter {
    private static final int NUMBER_OF_FIXED_LIST_ITEMS = 2;
    private static final int POSITION_HEADER_VIEW = 0;
    private static final int POSITION_CURRENT_ADDRESS = 1;


    private final Context context;
    private final ContactPictureLoader contactPictureLoader;
    private AlternateRecipientListener listener;
    private Account account;
    private List<Recipient> recipients;
    private Recipient currentRecipient;
    private final PEpProvider pEp;
    private PePUIArtefactCache uiCache;


    @Inject
    public AlternateRecipientAdapter(@Named("AppContext") Context context,
                                     @Named("MainUI") PEpProvider pEp,
                                     ContactPictureLoader contactPictureLoader
    ) {
        super();
        this.context = context;
        this.pEp = pEp;
        this.contactPictureLoader = contactPictureLoader;
        this.uiCache = PePUIArtefactCache.getInstance(context);
    }

    public void setUp(AlternateRecipientListener listener) {
        this.listener = listener;
    }

    public void setCurrentRecipient(Recipient currentRecipient) {
        this.currentRecipient = currentRecipient;
    }

    public void setAlternateRecipientInfo(List<Recipient> recipients) {
        this.recipients = recipients;
        int indexOfCurrentRecipient = recipients.indexOf(currentRecipient);
        if (indexOfCurrentRecipient >= 0) {
            currentRecipient = recipients.get(indexOfCurrentRecipient);
        }
        recipients.remove(currentRecipient);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (recipients == null) {
            return NUMBER_OF_FIXED_LIST_ITEMS;
        }

        return recipients.size() + NUMBER_OF_FIXED_LIST_ITEMS;
    }

    @Override
    public Recipient getItem(int position) {
        if (position == POSITION_HEADER_VIEW || position == POSITION_CURRENT_ADDRESS) {
            return currentRecipient;
        }

        return recipients == null ? null : getRecipientFromPosition(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private Recipient getRecipientFromPosition(int position) {
        return recipients.get(position - NUMBER_OF_FIXED_LIST_ITEMS);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = newView(parent);
        }
        account = uiCache.getComposingAccount();

        Recipient recipient = getItem(position);

        if (position == POSITION_HEADER_VIEW) {
            bindHeaderView(view, recipient);
        } else {
            bindItemView(view, recipient);
        }

        return view;
    }

    public View newView(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipient_alternate_item, parent, false);

        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != POSITION_HEADER_VIEW;
    }

    public void bindHeaderView(View view, Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(true);

        holder.headerName.setText(recipient.getNameOrUnknown(context));

        colorizeUnsecureRecipientText(
                (RatedRecipient) recipient,
                holder.headerName,
                getHeaderDefaultTextColor(holder.headerName.getContext())
        );

        if (!TextUtils.isEmpty(recipient.getAddressLabel())) {
            holder.headerAddressLabel.setText(recipient.getAddressLabel());
            holder.headerAddressLabel.setVisibility(View.VISIBLE);
        } else {
            holder.headerAddressLabel.setVisibility(View.GONE);
        }

        contactPictureLoader.setContactPicture(holder.headerPhoto, recipient.getAddress());
        holder.headerPhoto.assignContactUri(recipient.getContactLookupUri());
        if (account != null) {
            holder.headerPhoto.setPepRating(pEp.getRating(recipient.getAddress()), account.ispEpPrivacyProtected());
        }

        holder.headerRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRecipientRemove(currentRecipient);
            }
        });
    }

    public void bindItemView(View view, final Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(false);

        String address = recipient.getAddress().getAddress();
        holder.itemAddress.setText(address);

        colorizeUnsecureRecipientText(
                (RatedRecipient) recipient,
                holder.itemAddress,
                getBodyDefaultTextColor(holder.itemAddress.getContext())
        );

        if (!TextUtils.isEmpty(recipient.getAddressLabel())) {
            holder.itemAddressLabel.setText(recipient.getAddressLabel());
            holder.itemAddressLabel.setVisibility(View.VISIBLE);
        } else {
            holder.itemAddressLabel.setVisibility(View.GONE);
        }

        boolean isCurrent = currentRecipient == recipient;
        holder.itemAddress.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        holder.itemAddressLabel.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);

        holder.layoutItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRecipientChange(currentRecipient, recipient);
            }
        });

        configureCryptoStatusView(holder, recipient);
    }

    private void colorizeUnsecureRecipientText(
            RatedRecipient recipient,
            TextView textView,
            @ColorInt int defaultColor
    ) {
        Rating rating = recipient.getRating();
        int textColor = account.ispEpPrivacyProtected() && PEpUtils.isRatingUnsecure(rating)
                ? ContextCompat.getColor(context, R.color.pep_red)
                : defaultColor;
        textView.setTextColor(textColor);
    }

    private int getHeaderDefaultTextColor(Context context) {
        return ThemeManager.getColorFromAttributeResource(
                context, R.attr.textColorPrimaryRecipientDropdown);
    }

    private int getBodyDefaultTextColor(Context context) {
        return ThemeManager.getColorFromAttributeResource(
        context, R.attr.textColorSecondaryRecipientDropdown);
    }

    private void configureCryptoStatusView(RecipientTokenHolder holder, Recipient recipient) {
        switch (recipient.getCryptoStatus()) {
            case AVAILABLE_TRUSTED: {
                setCryptoStatusView(holder, R.drawable.status_lock_dots_3, R.attr.openpgp_green);
                break;
            }
            case AVAILABLE_UNTRUSTED: {
                setCryptoStatusView(holder, R.drawable.status_lock_dots_2, R.attr.openpgp_orange);
                break;
            }
            case UNAVAILABLE: {
                setCryptoStatusView(holder, R.drawable.status_lock_disabled_dots_1, R.attr.openpgp_red);
                break;
            }
            case UNDEFINED: {
                holder.itemCryptoStatus.setVisibility(View.GONE);
                break;
            }
        }
    }

    private void setCryptoStatusView(RecipientTokenHolder holder, @DrawableRes int cryptoStatusRes,
            @AttrRes int cryptoStatusColorAttr) {
        Resources resources = context.getResources();

        Drawable drawable = resources.getDrawable(cryptoStatusRes);
        // noinspection ConstantConditions, we know the resource exists!
        drawable.mutate();

        int cryptoStatusColor = ThemeUtils.getStyledColor(context, cryptoStatusColorAttr);
        drawable.setColorFilter(cryptoStatusColor, Mode.SRC_ATOP);

        holder.itemCryptoStatusIcon.setImageDrawable(drawable);
        holder.itemCryptoStatus.setVisibility(View.VISIBLE);
    }


    private static class RecipientTokenHolder {
        public final View layoutHeader, layoutItem;
        public final TextView headerName;
        public final TextView headerAddressLabel;
        public final PEpContactBadge headerPhoto;
        public final View headerRemove;
        public final TextView itemAddress;
        public final TextView itemAddressLabel;
        public final View itemCryptoStatus;
        public final ImageView itemCryptoStatusIcon;


        public RecipientTokenHolder(View view) {
            layoutHeader = view.findViewById(R.id.alternate_container_header);
            layoutItem = view.findViewById(R.id.alternate_container_item);

            headerName = (TextView) view.findViewById(R.id.alternate_header_name);
            headerAddressLabel = (TextView) view.findViewById(R.id.alternate_header_label);
            headerPhoto = (PEpContactBadge) view.findViewById(R.id.alternate_contact_photo);
            headerRemove = view.findViewById(R.id.alternate_remove);

            itemAddress = (TextView) view.findViewById(R.id.alternate_address);
            itemAddressLabel = (TextView) view.findViewById(R.id.alternate_address_label);
            itemCryptoStatus = view.findViewById(R.id.alternate_crypto_status);
            itemCryptoStatusIcon = (ImageView) view.findViewById(R.id.alternate_crypto_status_icon);
        }

        public void setShowAsHeader(boolean isHeader) {
            layoutHeader.setVisibility(isHeader ? View.VISIBLE : View.GONE);
            layoutItem.setVisibility(isHeader ? View.GONE : View.VISIBLE);
        }
    }

    public interface AlternateRecipientListener {
        void onRecipientRemove(Recipient currentRecipient);
        void onRecipientChange(Recipient currentRecipient, Recipient alternateRecipient);
    }
}
