package com.fsck.k9.fragment;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mailstore.DatabasePreviewType;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.ui.PEpContactBadge;

import org.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static com.fsck.k9.fragment.MLFProjectionInfo.ANSWERED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.ATTACHMENT_COUNT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.CC_LIST_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.DATE_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FLAGGED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FOLDER_NAME_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FORWARDED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PEP_RATING_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PREVIEW_TYPE_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.READ_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.SUBJECT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.THREAD_COUNT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.TO_LIST_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.UID_COLUMN;


public class MessageListAdapter extends CursorAdapter {

    private List<Integer> selectedMessages = new ArrayList<>();

    public void addSelected(Integer position) {
        selectedMessages.add(position);
    }

    public void removeSelected(Integer position) {
        selectedMessages.remove(position);
    }

    public void clearSelected() {
        selectedMessages.clear();
    }

    private enum Swipe {
        NO_SWIPE, LEFT, RIGHT;
    }

    private final MessageListFragment fragment;
    private Drawable mAttachmentIcon;
    private Drawable mForwardedIcon;
    private Drawable mAnsweredIcon;
    private Drawable mForwardedAnsweredIcon;
    private FontSizes fontSizes = K9.getFontSizes();

    MessageListAdapter(MessageListFragment fragment) {
        super(fragment.getActivity(), null, 0);
        this.fragment = fragment;
        mAttachmentIcon = fragment.getResources().getDrawable(R.drawable.ic_email_attachment_small);
        mAnsweredIcon = fragment.getResources().getDrawable(R.drawable.ic_email_answered_small);
        mForwardedIcon = fragment.getResources().getDrawable(R.drawable.ic_email_forwarded_small);
        mForwardedAnsweredIcon = fragment.getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
    }

    private String recipientSigil(boolean toMe, boolean ccMe) {
        if (toMe) {
            return fragment.getString(R.string.messagelist_sent_to_me_sigil);
        } else if (ccMe) {
            return fragment.getString(R.string.messagelist_sent_cc_me_sigil);
        } else {
            return "";
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //COMENTED INTENTIONALLY BY ARTURO IN ORDER TO MAKE EASIER THE NEXT PULL

//            MessageViewHolder holder = new MessageViewHolder();
//            holder.date = (TextView) view.findViewById(R.id.date);
//            holder.chip = view.findViewById(R.id.chip);
//            holder.attachment = view.findViewById(R.id.attachment_icon);
//
//            if (mPreviewLines == 0 && mContactsPictureLoader == null) {
//                view.findViewById(R.id.preview).setVisibility(GONE);
//                holder.preview = (TextView) view.findViewById(R.id.sender_compact);
//                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_center_right);
//                view.findViewById(R.id.flagged_bottom_right).setVisibility(GONE);
//
//
//
//            } else {
//                view.findViewById(R.id.sender_compact).setVisibility(GONE);
//                holder.preview = (TextView) view.findViewById(R.id.preview);
//                holder.flagged = (CheckBox) view.findViewById(R.id.flagged_bottom_right);
//                view.findViewById(R.id.flagged_center_right).setVisibility(GONE);
//
//            }
//
//            PEpContactBadge contactBadge = (PEpContactBadge) view.findViewById(R.id.contact_badge);
//            if (mContactsPictureLoader != null) {
//                holder.contactBadge = contactBadge;
//            } else {
//                contactBadge.setVisibility(GONE);
//            }
//
//            if (mSenderAboveSubject) {
//                holder.from = (TextView) view.findViewById(R.id.subject);
//                mFontSizes.setViewTextSize(holder.from, mFontSizes.getMessageListSender());
//
//            } else {
//                holder.subject = (TextView) view.findViewById(R.id.subject);
//                mFontSizes.setViewTextSize(holder.subject, mFontSizes.getMessageListSubject());
//
//            }
//
//            mFontSizes.setViewTextSize(holder.date, mFontSizes.getMessageListDate());
//
//
//            // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
//            holder.preview.setLines(Math.max(mPreviewLines,1));
//            mFontSizes.setViewTextSize(holder.preview, mFontSizes.getMessageListPreview());
//            holder.threadCount = (TextView) view.findViewById(R.id.thread_count);
//            mFontSizes.setViewTextSize(holder.threadCount, mFontSizes.getMessageListSubject()); // thread count is next to subject
//            view.findViewById(R.id.selected_checkbox_wrapper).setVisibility((mCheckboxes) ? View.VISIBLE : GONE);
//
//            holder.flagged.setVisibility(mStars ? View.VISIBLE : GONE);
//            holder.flagged.setOnClickListener(holder);
//
//
//            holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
//            holder.selected.setOnClickListener(holder);




//            genericView.setTag(holder);

        return LayoutInflater.from(context).inflate(R.layout.generic_message_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Account account = fragment.getAccountFromCursor(cursor);

        String fromList = cursor.getString(SENDER_LIST_COLUMN);
        String toList = cursor.getString(TO_LIST_COLUMN);
        String ccList = cursor.getString(CC_LIST_COLUMN);
        Address[] fromAddrs = Address.unpack(fromList);
        Address[] toAddrs = Address.unpack(toList);
        Address[] ccAddrs = Address.unpack(ccList);
        Rating pEpRating;
        try {
            pEpRating = PEpUtils.stringToRating(cursor.getString(PEP_RATING_COLUMN));
        } catch (IllegalArgumentException ex) {
            pEpRating = PEpUtils.stringToRating(cursor.getString(PEP_RATING_COLUMN));
        }
        boolean fromMe = fragment.messageHelper.toMe(account, fromAddrs);
        boolean toMe = fragment.messageHelper.toMe(account, toAddrs);
        boolean ccMe = fragment.messageHelper.toMe(account, ccAddrs);

        CharSequence displayName = fragment.messageHelper.getDisplayName(account, fromAddrs, toAddrs);
        CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN));

        Address counterpartyAddress = null;
        if (fromMe) {
            if (toAddrs.length > 0) {
                counterpartyAddress = toAddrs[0];
            } else if (ccAddrs.length > 0) {
                counterpartyAddress = ccAddrs[0];
            }
        } else if (fromAddrs.length > 0) {
            counterpartyAddress = fromAddrs[0];
        }

        int threadCount = (fragment.showingThreadedList) ? cursor.getInt(THREAD_COUNT_COLUMN) : 0;

        String subject = cursor.getString(SUBJECT_COLUMN);
        if (subject == null || TextUtils.isEmpty(subject.trim())) {
            subject = fragment.getString(R.string.general_no_subject);
        } else if (threadCount > 1) {
            // If this is a thread, strip the RE/FW from the subject.  "Be like Outlook."
            subject = Utility.stripSubject(subject);
        }

        boolean read = (cursor.getInt(READ_COLUMN) == 1);
        boolean flagged = (cursor.getInt(FLAGGED_COLUMN) == 1);
        boolean answered = (cursor.getInt(ANSWERED_COLUMN) == 1);
        boolean forwarded = (cursor.getInt(FORWARDED_COLUMN) == 1);

        SwipeLayout swipeView = (SwipeLayout) view.findViewById(R.id.swipe_container);
        swipeView.addDrag(SwipeLayout.DragEdge.Left, swipeView.findViewById(R.id.archive_email_container));
        swipeView.addDrag(SwipeLayout.DragEdge.Right, swipeView.findViewById(R.id.delete_email_container));

        final Swipe[] swipe = {Swipe.NO_SWIPE};
        swipeView.findViewById(R.id.delete_email_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipe[0] = Swipe.LEFT;
                swipeView.close();
            }
        });

        swipeView.findViewById(R.id.archive_email_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipe[0] = Swipe.RIGHT;
                swipeView.close();
            }
        });

        swipeView.addSwipeListener(new SwipeLayout.SwipeListener() {
            @Override
            public void onStartOpen(SwipeLayout layout) {
            }

            @Override
            public void onOpen(SwipeLayout layout) {

            }

            @Override
            public void onStartClose(SwipeLayout layout) {

            }

            @Override
            public void onClose(SwipeLayout layout) {
                if (swipe[0].equals(Swipe.LEFT)) {
                    int position = fragment.listView.getPositionForView(layout);
                    MessageReference messageReference = fragment.getMessageAtPosition(position);
                    fragment.onDelete(messageReference);
                    MessageListAdapter.this.notifyDataSetChanged();
                } else if (swipe[0].equals(Swipe.RIGHT)) {
                    int position = fragment.listView.getPositionForView(layout);
                    MessageReference messageReference = fragment.getMessageAtPosition(position);
                    fragment.onArchive(messageReference);
                }
            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

            }
        });

        View readView = view.findViewById(R.id.message_read_container);
        View unreadView = view.findViewById(R.id.message_unread_container);
        View containerBackground = view.findViewById(R.id.container_background);
        if (read) {
            readView.setVisibility(View.VISIBLE);
            unreadView.setVisibility(GONE);
            view = readView;
        } else {
            unreadView.setVisibility(View.VISIBLE);
            readView.setVisibility(GONE);
            view = unreadView;
        }

        MessageViewHolder holder = new MessageViewHolder(fragment);
        holder.date = (TextView) view.findViewById(R.id.date);
        holder.chip = view.findViewById(R.id.chip);
        holder.attachment = view.findViewById(R.id.attachment_icon);

        if (fragment.previewLines == 0 && fragment.contactsPictureLoader == null) {
            view.findViewById(R.id.preview).setVisibility(GONE);
            holder.preview = (TextView) view.findViewById(R.id.sender_compact);
            holder.flagged = (CheckBox) view.findViewById(R.id.flagged_center_right);
            view.findViewById(R.id.flagged_bottom_right).setVisibility(GONE);



        } else {
            view.findViewById(R.id.sender_compact).setVisibility(GONE);
            holder.preview = (TextView) view.findViewById(R.id.preview);
            holder.flagged = (CheckBox) view.findViewById(R.id.flagged_bottom_right);
            view.findViewById(R.id.flagged_center_right).setVisibility(GONE);

        }

        PEpContactBadge contactBadge = (PEpContactBadge) view.findViewById(R.id.contact_badge);
        if (fragment.contactsPictureLoader != null) {
            holder.contactBadge = contactBadge;
        } else {
            contactBadge.setVisibility(GONE);
        }

        if (fragment.senderAboveSubject) {
            holder.from = (TextView) view.findViewById(R.id.subject);
            fontSizes.setViewTextSize(holder.from, fontSizes.getMessageListSender());

        } else {
            holder.subject = (TextView) view.findViewById(R.id.subject);
            fontSizes.setViewTextSize(holder.subject, fontSizes.getMessageListSubject());

        }

        fontSizes.setViewTextSize(holder.date, fontSizes.getMessageListDate());


        // 1 preview line is needed even if it is set to 0, because subject is part of the same text view
        //holder.preview.setLines(Math.max(mPreviewLines,1));
        fontSizes.setViewTextSize(holder.preview, fontSizes.getMessageListPreview());
        holder.threadCount = (TextView) view.findViewById(R.id.thread_count);
        fontSizes.setViewTextSize(holder.threadCount, fontSizes.getMessageListSubject()); // thread count is next to subject
        view.findViewById(R.id.selected_checkbox_wrapper).setVisibility((fragment.checkboxes) ? View.VISIBLE : GONE);

        holder.flagged.setVisibility(fragment.stars ? View.VISIBLE : GONE);
        holder.flagged.setOnClickListener(holder);


        holder.selected = (CheckBox) view.findViewById(R.id.selected_checkbox);
        holder.selected.setOnClickListener(holder);

        boolean hasAttachments = (cursor.getInt(ATTACHMENT_COUNT_COLUMN) > 0);

//            MessageViewHolder holder = (MessageViewHolder) view.getTag();

//            int maybeBoldTypeface = (read) ? Typeface.NORMAL : Typeface.BOLD;

        long uniqueId = cursor.getLong(fragment.uniqueIdColumn);
        boolean selected = fragment.selected.contains(uniqueId);


        if (Preferences.getPreferences(fragment.getActivity().getApplicationContext()).getAccounts().size() > 1) holder.chip.setBackgroundColor(account.getChipColor());

        if (fragment.checkboxes) {
            holder.selected.setChecked(selected);
        }

        if (fragment.stars) {
            holder.flagged.setChecked(flagged);
        }
        holder.position = cursor.getPosition();

        if (holder.contactBadge != null) {
            if (counterpartyAddress != null) {
                Utility.setContactForBadge(holder.contactBadge, counterpartyAddress);
//                    Address from = fromAddrs[0];                            // FIXME: From is an array?!
//                    List<Address> to = Arrays.asList(toAddrs);
//                    List<Address> cc = Arrays.asList(ccAddrs);j
//                    List<Address> bcc = Arrays.asList(new Address[0]);
//                    holder.contactBadge.setpEpRating(((K9) context.getApplicationContext()).getpEpProvider().getPrivacyState(from, to, cc, bcc));
                    /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but QuickContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */

                holder.contactBadge.setPadding(0, 0, 0, 0);
                fragment.contactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
            } else {
                holder.contactBadge.assignContactUri(null);
                holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
            }
            holder.contactBadge.setPepRating(pEpRating, account.ispEpPrivacyProtected());
        }
        if (fragment.activeMessage != null) {
            String uid = cursor.getString(UID_COLUMN);
            String folderName = cursor.getString(FOLDER_NAME_COLUMN);

            if (account.getUuid().equals(fragment.activeMessage.getAccountUuid()) &&
                    folderName.equals(fragment.activeMessage.getFolderName()) &&
                    uid.equals(fragment.activeMessage.getUid())) {
                int res = R.attr.messageListActiveItemBackgroundColor;

                TypedValue outValue = new TypedValue();
                fragment.getActivity().getTheme().resolveAttribute(res, outValue, true);
                view.setBackgroundColor(outValue.data);
            }
        }

        // Thread count
        if (threadCount > 1) {
            holder.threadCount.setText(String.format("%d", threadCount));
            holder.threadCount.setVisibility(View.VISIBLE);
        } else {
            holder.threadCount.setVisibility(GONE);
        }

        CharSequence beforePreviewText = (fragment.senderAboveSubject) ? subject : displayName;

        String sigil = recipientSigil(toMe, ccMe);

        SpannableStringBuilder messageStringBuilder = new SpannableStringBuilder(sigil)
                .append(beforePreviewText);
        messageStringBuilder.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, messageStringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (fragment.previewLines > 0) {
            String preview = getPreview(cursor);
            messageStringBuilder.append(" ").append(preview);
        }

        holder.preview.setText(messageStringBuilder, TextView.BufferType.SPANNABLE);

        Spannable str = (Spannable)holder.preview.getText();

        // Create a span section for the sender, and assign the correct font size and weight
        int fontSize = (fragment.senderAboveSubject) ?
                fontSizes.getMessageListSubject():
                fontSizes.getMessageListSender();

        AbsoluteSizeSpan span = new AbsoluteSizeSpan(fontSize, true);
        str.setSpan(span, 0, beforePreviewText.length() + sigil.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        //TODO: make this part of the theme
        int color = (K9.getK9Theme() == K9.Theme.LIGHT) ?
                Color.rgb(105, 105, 105) :
                Color.rgb(160, 160, 160);

        // Set span (color) for preview message
        str.setSpan(new ForegroundColorSpan(color), beforePreviewText.length() + sigil.length(),
                str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Drawable statusHolder = null;
        if (forwarded && answered) {
            statusHolder = mForwardedAnsweredIcon;
        } else if (answered) {
            statusHolder = mAnsweredIcon;
        } else if (forwarded) {
            statusHolder = mForwardedIcon;
        }

        if (holder.from != null ) {
//                holder.from.setTypeface(Typeface.create(holder.from.getTypeface(), maybeBoldTypeface));
            if (fragment.senderAboveSubject) {
                if (hasAttachments) holder.attachment.setVisibility(View.VISIBLE);
                holder.from.setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        null, // right
                        null); // bottom

                holder.from.setText(displayName);
            } else {
                holder.from.setText(new SpannableStringBuilder(sigil).append(displayName));
            }
        }

        if (holder.subject != null ) {
            if (!fragment.senderAboveSubject) {
                if (hasAttachments) holder.attachment.setVisibility(View.VISIBLE);
                holder.subject.setCompoundDrawablesWithIntrinsicBounds(
                        statusHolder, // left
                        null, // top
                        null, // right
                        null); // bottom
            }
//                holder.subject.setTypeface(Typeface.create(holder.subject.getTypeface(), maybeBoldTypeface));
            holder.subject.setText(subject);
        }

        holder.date.setText(displayDate);
        holder.container = containerBackground;

        if (selectedMessages != null && selectedMessages.contains(holder.position)) {
            holder.container.setBackgroundColor(context.getResources().getColor(R.color.pep_selected_item));
        } else {
            holder.container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }
    }


    private void formatPreviewText(TextView preview, CharSequence beforePreviewText, String sigil) {
        Spannable previewText = (Spannable)preview.getText();
        previewText.setSpan(buildSenderSpan(), 0, beforePreviewText.length() + sigil.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int previewSpanColor = buildPreviewSpanColor();

        // Set span (color) for preview message
        previewText.setSpan(new ForegroundColorSpan(previewSpanColor), beforePreviewText.length() + sigil.length(),
                previewText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Create a span section for the sender, and assign the correct font size and weight
     */
    private AbsoluteSizeSpan buildSenderSpan() {
        int fontSize = (fragment.senderAboveSubject) ?
                fontSizes.getMessageListSubject():
                fontSizes.getMessageListSender();
        return new AbsoluteSizeSpan(fontSize, true);
    }

    private Address fetchCounterPartyAddress(boolean fromMe, Address[] toAddrs, Address[] ccAddrs, Address[] fromAddrs) {
        if (fromMe) {
            if (toAddrs.length > 0) {
                return toAddrs[0];
            } else if (ccAddrs.length > 0) {
                return ccAddrs[0];
            }
        } else if (fromAddrs.length > 0) {
            return fromAddrs[0];
        }
        return null;
    }

    private void updateContactBadge(MessageViewHolder holder, Address counterpartyAddress) {
        if (counterpartyAddress != null) {
            Utility.setContactForBadge(holder.contactBadge, counterpartyAddress);
                    /*
                     * At least in Android 2.2 a different background + padding is used when no
                     * email address is available. ListView reuses the views but ContactBadge
                     * doesn't reset the padding, so we do it ourselves.
                     */
            holder.contactBadge.setPadding(0, 0, 0, 0);
            fragment.contactsPictureLoader.loadContactPicture(counterpartyAddress, holder.contactBadge);
        } else {
            holder.contactBadge.assignContactUri(null);
            holder.contactBadge.setImageResource(R.drawable.ic_contact_picture);
        }
    }

    private void changeBackgroundColorIfActiveMessage(Cursor cursor, Account account, View view) {
        String uid = cursor.getString(UID_COLUMN);
        String folderName = cursor.getString(FOLDER_NAME_COLUMN);

        if (account.getUuid().equals(fragment.activeMessage.getAccountUuid()) &&
                folderName.equals(fragment.activeMessage.getFolderName()) &&
                uid.equals(fragment.activeMessage.getUid())) {
            int res = R.attr.messageListActiveItemBackgroundColor;

            TypedValue outValue = new TypedValue();
            fragment.getActivity().getTheme().resolveAttribute(res, outValue, true);
            view.setBackgroundColor(outValue.data);
        }
    }

    private int buildPreviewSpanColor() {
        //TODO: make this part of the theme
        return (K9.getK9Theme() == K9.Theme.LIGHT) ?
                Color.rgb(105, 105, 105) :
                Color.rgb(160, 160, 160);
    }

    private Drawable buildStatusHolder(boolean forwarded, boolean answered) {
        if (forwarded && answered) {
            return mForwardedAnsweredIcon;
        } else if (answered) {
            return mAnsweredIcon;
        } else if (forwarded) {
            return mForwardedIcon;
        }
        return null;
    }

    private void setBackgroundColor(View view, boolean selected, boolean read) {
        if (selected || K9.useBackgroundAsUnreadIndicator()) {
            int res;
            if (selected) {
                res = R.attr.messageListSelectedBackgroundColor;
            } else if (read) {
                res = R.attr.messageListReadItemBackgroundColor;
            } else {
                res = R.attr.messageListUnreadItemBackgroundColor;
            }

            TypedValue outValue = new TypedValue();
            fragment.getActivity().getTheme().resolveAttribute(res, outValue, true);
            view.setBackgroundColor(outValue.data);
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void updateWithThreadCount(MessageViewHolder holder, int threadCount) {
        if (threadCount > 1) {
            holder.threadCount.setText(String.format("%d", threadCount));
            holder.threadCount.setVisibility(View.VISIBLE);
        } else {
            holder.threadCount.setVisibility(GONE);
        }
    }

    private String getPreview(Cursor cursor) {
        String previewTypeString = cursor.getString(PREVIEW_TYPE_COLUMN);
        DatabasePreviewType previewType = DatabasePreviewType.fromDatabaseValue(previewTypeString);

        switch (previewType) {
            case NONE:
            case ERROR: {
                return "";
            }
            case ENCRYPTED: {
                return fragment.getString(R.string.preview_encrypted);
            }
            case TEXT: {
                return cursor.getString(PREVIEW_COLUMN);
            }
        }

        throw new AssertionError("Unknown preview type: " + previewType);
    }
}
