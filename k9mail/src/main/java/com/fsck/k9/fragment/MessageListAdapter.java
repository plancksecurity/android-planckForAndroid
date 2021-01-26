package com.fsck.k9.fragment;


import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import androidx.core.content.ContextCompat;

import com.daimajia.swipe.SwipeLayout;
import com.fsck.k9.Account;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpUtils;
import foundation.pEp.jniadapter.Rating;

import static android.view.View.GONE;
import static com.fsck.k9.fragment.MLFProjectionInfo.ANSWERED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.CC_LIST_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.DATE_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FLAGGED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.FORWARDED_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.PEP_RATING_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.READ_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.SENDER_LIST_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.SUBJECT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.THREAD_COUNT_COLUMN;
import static com.fsck.k9.fragment.MLFProjectionInfo.TO_LIST_COLUMN;


public class MessageListAdapter extends CursorAdapter {

    private enum Swipe {
        NO_SWIPE, LEFT, RIGHT
    }

    private final MessageListFragment fragment;
    private Drawable mForwardedIcon;
    private Drawable mAnsweredIcon;
    private Drawable mForwardedAnsweredIcon;
    private FontSizes fontSizes = K9.getFontSizes();

    MessageListAdapter(MessageListFragment fragment) {
        super(fragment.getActivity(), null, 0);
        this.fragment = fragment;
        startDrawables();

    }

    private void startDrawables() {
        Context context = fragment.getContext();
        if (context != null) {
            mAnsweredIcon = ContextCompat.getDrawable(context, fragment.getAttributeResource(R.attr.iconActionReply));
            if (mAnsweredIcon != null) {
                mAnsweredIcon.setColorFilter(ContextCompat.getColor(context, R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
            }
            mForwardedIcon = ContextCompat.getDrawable(context, fragment.getAttributeResource(R.attr.iconActionForward));
            if (mForwardedIcon != null) {
                mForwardedIcon.setColorFilter(ContextCompat.getColor(context, R.color.gray), android.graphics.PorterDuff.Mode.SRC_IN);
            }
            mForwardedAnsweredIcon = fragment.getResources().getDrawable(R.drawable.ic_email_forwarded_answered_small);
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

        CharSequence displayName = fragment.messageHelper.getDisplayName(account, fromAddrs, toAddrs);
        CharSequence displayDate = DateUtils.getRelativeTimeSpanString(context, cursor.getLong(DATE_COLUMN));

        Address counterpartyAddress = fetchCounterPartyAddress(fromMe, toAddrs, ccAddrs, fromAddrs);

        int threadCount = (fragment.shouldShowThreadedList()) ? cursor.getInt(THREAD_COUNT_COLUMN) : 0;

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

        setSwipeLayout(view);

        View readView = view.findViewById(R.id.message_read_container);
        View unreadView = view.findViewById(R.id.message_unread_container);
        if (read) {
            readView.setVisibility(View.VISIBLE);
            unreadView.setVisibility(GONE);
            view = readView;
        } else {
            unreadView.setVisibility(View.VISIBLE);
            readView.setVisibility(GONE);
            view = unreadView;
        }

        Drawable statusHolder = buildStatusHolder(forwarded, answered);
        MessageViewHolder holder = new MessageViewHolder(fragment, fontSizes, view, fragment.viewHolderActions);
        holder.bind(cursor,
                counterpartyAddress,
                pEpRating,
                account,
                displayName,
                displayDate,
                flagged,
                fragment.isMessageSelected(cursor),
                read,
                statusHolder,
                subject,
                threadCount
        );
    }

    private void setSwipeLayout(View view) {
        SwipeLayout swipeView = view.findViewById(R.id.swipe_container);
        swipeView.addDrag(SwipeLayout.DragEdge.Left, swipeView.findViewById(R.id.archive_email_container));
        swipeView.addDrag(SwipeLayout.DragEdge.Right, swipeView.findViewById(R.id.delete_email_container));

        final Swipe[] swipe = {Swipe.NO_SWIPE};
        swipeView.findViewById(R.id.delete_email_container).setOnClickListener(v -> {
            swipe[0] = Swipe.LEFT;
            swipeView.close();
        });

        swipeView.findViewById(R.id.archive_email_container).setOnClickListener(v -> {
            swipe[0] = Swipe.RIGHT;
            swipeView.close();
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
            public void onClose(SwipeLayout layout) {

            }

            @Override
            public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

            }

            @Override
            public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

            }
        });
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
}
