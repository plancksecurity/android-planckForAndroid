package com.fsck.k9.activity.compose;


import static com.fsck.k9.activity.compose.RSVUtilsKt.withRatedRecipients;
import static com.fsck.k9.activity.compose.RSVUtilsKt.withRecipientsSortedByRating;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListPopupWindow;
import android.widget.ListView;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.AlternateRecipientAdapter.AlternateRecipientListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import com.fsck.k9.pEp.infrastructure.components.ApplicationComponent;
import com.fsck.k9.ui.contacts.ContactPictureLoader;
import com.tokenautocomplete.CountSpan;
import com.tokenautocomplete.TokenCompleteTextView;

import org.apache.james.mime4j.util.CharsetUtil;
import foundation.pEp.jniadapter.Rating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;


public class RecipientSelectView extends TokenCompleteTextView<Recipient> implements LoaderCallbacks<List<Recipient>>,
        AlternateRecipientListener {

    private static final int MINIMUM_LENGTH_FOR_FILTERING = 1;

    private static final String ARG_QUERY = "query";

    private static final int LOADER_ID_FILTERING = 0;
    private static final int LOADER_ID_ALTERNATES = 1;


    private RecipientAdapter adapter;
    @Nullable
    private String cryptoProvider;
    @Nullable
    private LoaderManager loaderManager;

    private ListPopupWindow alternatesPopup;
    private Recipient alternatesPopupRecipient;
    private TokenListener<Recipient> listener;
    private PEpProvider pEp;
    private Context context;
    private Account account;
    private PePUIArtefactCache uiCache;
    private final List<Recipient> unsecureRecipients = new ArrayList<>();
    private boolean isProcessing;

    @Inject ContactPictureLoader contactPictureLoader;
    @Inject AlternateRecipientAdapter alternatesAdapter;

    public RecipientSelectView(Context context) {
        super(context);
        initView(context);
    }

    public RecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        // TODO: validator?
        this.context = context;
        getApplicationComponent(context).inject(this);

        uiCache = PePUIArtefactCache.getInstance(context);

        alternatesPopup = new ListPopupWindow(context);
        alternatesAdapter.setUp(this);
        alternatesPopup.setAdapter(alternatesAdapter);

        // don't allow duplicates, based on equality of recipient objects, which is e-mail addresses
        allowDuplicates(false);

        // if a token is completed, pick an entry based on best guess.
        // Note that we override performCompletion, so this doesn't actually do anything
        performBestGuess(true);

        adapter = new RecipientAdapter(context, contactPictureLoader);
        setAdapter(adapter);
        pEp = ((K9) context.getApplicationContext()).getpEpProvider();

        setLongClickable(false);
        setTokenListener(new TokenCompleteTextView.TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient token) {
                if (listener != null) {
                    listener.onTokenAdded(token);
                }
            }

            @Override
            public void onTokenRemoved(Recipient token) {
                removeUnsecureRecipient(token);
                if (listener != null) {
                    listener.onTokenRemoved(token);
                }
            }
        });
    }

    @Override
    protected View getViewForObject(Recipient recipient) {
        account = uiCache.getComposingAccount();

        View view = inflateLayout();

        RecipientTokenViewHolder holder =
                new RecipientTokenViewHolder(view, contactPictureLoader, account, cryptoProvider);
        view.setTag(holder);

        bindObjectView(recipient, view);

        return view;
    }

    @SuppressLint("InflateParams")
    private View inflateLayout() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        return layoutInflater.inflate(R.layout.recipient_token_item, null, false);
    }

    private void bindObjectView(Recipient recipient, View view) {
        RecipientTokenViewHolder holder = (RecipientTokenViewHolder) view.getTag();

        holder.bind(recipient);

        pEp.getRating(recipient.getAddress(), new PEpProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                addUnsecureRecipientIfNeeded(
                        recipient,
                        account.ispEpPrivacyProtected()
                                && PEpUtils.isRatingUnsecure(rating)
                );
                if (listener != null) {
                    listener.updateRecipientSecurity(hasUnsecureRecipients());
                }
                setCountColorIfNeeded();
                holder.updateRating(rating);
                postInvalidateDelayed(100);
            }

            @Override
            public void onError(Throwable throwable) {
                addUnsecureRecipientIfNeeded(recipient, account.ispEpPrivacyProtected());
                if (listener != null) {
                    listener.updateRecipientSecurity(true);
                }
                setCountColorIfNeeded();
                holder.updateRating(Rating.pEpRatingUndefined);
                postInvalidateDelayed(100);
            }
        });
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        Editable text = getText();

        if (text != null && action == MotionEvent.ACTION_UP) {
            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (offset != -1) {
                TokenImageSpan[] links = text.getSpans(offset, offset, RecipientTokenSpan.class);
                if (links.length > 0) {
                    showAlternates(links[0].getToken());
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void performCollapse(boolean hasFocus) {
        super.performCollapse(hasFocus);
        if(!hasFocus) {
            truncateFirstDisplayNameAndSetCountColorIfNeeded();
        } else {
            restoreFirstDisplayNameIfNeeded();
        }
    }

    private void restoreFirstDisplayNameIfNeeded() {
        List<Recipient> recipients = getObjects();
        if (!recipients.isEmpty()) {
            if (recipients.get(0).isDisplayedNameTruncated()) {
                for (Recipient recipient : recipients) {
                    removeObject(recipient);
                }
                recipients.get(0).restoreFullDisplayedName();
                for (Recipient recipient : recipients) {
                    addObject(recipient);
                }
            }
        }
    }

    private void truncateFirstDisplayNameAndSetCountColorIfNeeded() {
        Editable editable = getText();
        if (editable != null) {
            CountSpan[] countSpans = editable.getSpans(
                    0, editable.length(), CountSpan.class);
            if (countSpans.length > 0) {
                CountSpan countSpan = countSpans[0];
                if (editable.getSpanStart(countSpan) >= 0) {
                    try {
                        int count = Integer.parseInt(countSpans[0].text.substring(1));
                        if (count == getTokenCount()) {
                            truncateFirstDisplayName(countSpans[0].text);
                        }
                        setCountColor(editable, countSpan, count);
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        }
    }

    private void setCountColor(Editable editable, CountSpan countSpan, int count) {
        int countColor = hasUnsecureRecipients(count)
                ? ContextCompat.getColor(context, R.color.pep_red)
                : getCurrentTextColor();

        CountSpan coloredCountSpan = new CountSpan(
                count,
                getContext(),
                countColor,
                (int) getTextSize(),
                (int) maxTextWidth()
        );
        editable.setSpan(
                coloredCountSpan,
                editable.getSpanStart(countSpan),
                editable.getSpanEnd(countSpan),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        editable.removeSpan(countSpan);
    }

    private void truncateFirstDisplayName(String countText) {
        List<Recipient> recipients = getObjects();
        String textToDisplay = recipients.get(0).getDisplayNameOrAddress();
        Layout lastLayout = getLayout();
        float requiredWidth = lastLayout.getPaint().measureText(textToDisplay + countText);
        for (Recipient recipient : recipients) {
            removeObject(recipient);
        }
        while (maxTextWidth() - requiredWidth < 80) {
            textToDisplay = textToDisplay.substring(0, textToDisplay.length()-1);
            requiredWidth = lastLayout.getPaint().measureText(textToDisplay + countText);
        }

        recipients.get(0).truncateDisplayedName(textToDisplay.length() - 1);
        for (Recipient recipient : recipients) {
            addObject(recipient);
        }
    }

    private void setCountColorIfNeeded() {
        Editable editable = getText();
        if(editable != null) {
            CountSpan[] countSpans = editable.getSpans(0, editable.length(), CountSpan.class);
            if (countSpans.length > 0) {
                CountSpan countSpan = countSpans[0];
                if(editable.getSpanStart(countSpan) >= 0) {
                    try {
                        int count = Integer.parseInt(countSpan.text.substring(1));
                        setCountColor(editable, countSpan, count);
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        }
    }

    private boolean hasUnsecureRecipients() {
        return !unsecureRecipients.isEmpty();
    }

    private boolean hasUnsecureRecipients(int count) {
        List<Recipient> recipients = getObjects();
        for (Recipient recipient : unsecureRecipients) {
            int start = getTokenCount() - count;
            if (recipients.indexOf(recipient) >= start) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Recipient defaultObject(String completionText) {
        Address[] parsedAddresses = Address.parse(completionText);
        if (!CharsetUtil.isASCII(completionText)) {
            setError(getContext().getString(R.string.recipient_error_non_ascii));
            return null;
        }
        if (parsedAddresses.length == 0 || parsedAddresses[0].getAddress() == null) {
            setError(getContext().getString(R.string.recipient_error_parse_failed));
            return null;
        }

        return new Recipient(parsedAddresses[0]);
    }

    public boolean isEmpty() {
        return getObjects().isEmpty();
    }

    public void setLoaderManager(@Nullable LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (loaderManager != null) {
            loaderManager.destroyLoader(LOADER_ID_ALTERNATES);
            loaderManager.destroyLoader(LOADER_ID_FILTERING);
            loaderManager = null;
        }
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (hasFocus) {
            displayKeyboard();
        }
    }

    private void displayKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    public void showDropDown() {
        boolean cursorIsValid = adapter != null;
        if (!cursorIsValid) {
            return;
        }

        super.showDropDown();
    }

    @Override
    public void performCompletion() {
        if (getListSelection() == ListView.INVALID_POSITION && enoughToFilter()) {
            Object recipientText = defaultObject(currentCompletionText());
            if (recipientText != null) {
                replaceText(convertSelectionToString(recipientText));
            }
        } else {
            super.performCompletion();
        }
    }

    private void addUnsecureRecipientIfNeeded(Recipient recipient, boolean unsecure) {
        if(unsecure && !unsecureRecipients.contains(recipient)) {
            unsecureRecipients.add(recipient);
        }
    }

    private void removeUnsecureRecipient(Recipient recipient) {
        unsecureRecipients.remove(recipient);
        if (listener != null) {
            listener.updateRecipientSecurity(hasUnsecureRecipients(getTokenCount()));
        }
    }

    @Override
    protected void performFiltering(@NonNull CharSequence text, int start, int end, int keyCode) {
        if (loaderManager == null) {
            return;
        }

        String query = text.subSequence(start, end).toString();
        if (TextUtils.isEmpty(query) || query.length() < MINIMUM_LENGTH_FOR_FILTERING) {
            loaderManager.destroyLoader(LOADER_ID_FILTERING);
            return;
        }

        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        loaderManager.restartLoader(LOADER_ID_FILTERING, args, this);
    }

    public void setCryptoProvider(@Nullable String cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public void addRecipients(Recipient... recipients) {
        if (recipients.length == 1) {
            addObject(recipients[0]);
        } else {
            withRecipientsSortedByRating(recipients, pEp, ratedRecipients -> {
                for (Recipient recipient : ratedRecipients) {
                    addObject(recipient);
                }
            });
        }
    }

    public Address[] getAddresses() {
        List<Recipient> recipients = getObjects();
        Address[] address = new Address[recipients.size()];
        for (int i = 0; i < address.length; i++) {
            address[i] = recipients.get(i).getAddress();
        }

        return address;
    }

    public void emptyAddresses() {
        for (Recipient recipient : getObjects()) {
            removeObject(recipient);
        }
    }

    public String[] getAddressesText() {
        List<Recipient> recipients = getObjects();
        String[] address = new String[recipients.size()];
        for (int i = 0; i < address.length; i++) {
            address[i] = recipients.get(i).getAddress().toString();
        }

        return address;
    }

    private void showAlternates(Recipient recipient) {
        if (loaderManager == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);

        alternatesPopupRecipient = recipient;
        loaderManager.restartLoader(LOADER_ID_ALTERNATES, null, RecipientSelectView.this);
    }

    public void postShowAlternatesPopup(final List<Recipient> data) {
        // We delay this call so the soft keyboard is gone by the time the popup is layouted
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                withRatedRecipients(
                        data,
                        pEp,
                        recipients -> showAlternatesPopup(recipients)
                );
            }
        });
    }

    public void showAlternatesPopup(List<Recipient> data) {
        if (loaderManager == null) {
            return;
        }

        // Copy anchor settings from the autocomplete dropdown
        View anchorView = getRootView().findViewById(getDropDownAnchor());
        alternatesPopup.setAnchorView(anchorView);
        alternatesPopup.setPromptPosition(ListPopupWindow.POSITION_PROMPT_BELOW);
        if (anchorView != null) {
            alternatesPopup.setVerticalOffset(getAlternatesPopupOffset(anchorView.getHeight()));
        }
        alternatesPopup.setWidth(getDropDownWidth());

        alternatesAdapter.setCurrentRecipient(alternatesPopupRecipient);
        alternatesAdapter.setAlternateRecipientInfo(data);

        // Clear the checked item.
        alternatesPopup.show();
        ListView listView = alternatesPopup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    private int getAlternatesPopupOffset(int anchorViewHeight) {
        View tokenView = getTokenViewForRecipient(alternatesPopupRecipient);
        if (tokenView != null) {
            int tokenViewHeight = tokenView.getHeight();
            if (anchorViewHeight < tokenViewHeight * 2) {
                return 0;
            }
            int size = getTokenCount();
            int index = getObjects().indexOf(alternatesPopupRecipient) + 1;
            return (index - size) * tokenViewHeight;
        }
        return  0;
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        alternatesPopup.dismiss();
        return super.onKeyDown(keyCode, event);
    }

    public void removeRecipient(Recipient recipient) {
        removeUnsecureRecipient(recipient);
        removeObject(recipient);
        post(() -> {
            if (getTokenCount() == 1) {
                requestFocus();
            } else if (getTokenCount() > 1) {
                truncateFirstDisplayNameAndSetCountColorIfNeeded();
            }
        });
    }

    @Override
    public Loader<List<Recipient>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_FILTERING: {
                String query = args != null && args.containsKey(ARG_QUERY) ? args.getString(ARG_QUERY) : "";
                adapter.setHighlight(query);
                return new RecipientLoader(getContext(), cryptoProvider, query);
            }
            case LOADER_ID_ALTERNATES: {
                Uri contactLookupUri = alternatesPopupRecipient.getContactLookupUri();
                if (contactLookupUri != null) {
                    return new RecipientLoader(getContext(), cryptoProvider, contactLookupUri, true);
                } else {
                    return new RecipientLoader(getContext(), cryptoProvider, alternatesPopupRecipient.getAddress());
                }
            }
        }

        throw new IllegalStateException("Unknown Loader ID: " + id);
    }

    @Override
    public void onLoadFinished(Loader<List<Recipient>> loader, List<Recipient> data) {
        if (loaderManager == null) {
            return;
        }

        switch (loader.getId()) {
            case LOADER_ID_FILTERING: {
                adapter.setRecipients(data);
                break;
            }
            case LOADER_ID_ALTERNATES: {
                postShowAlternatesPopup(data);
                loaderManager.destroyLoader(LOADER_ID_ALTERNATES);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Recipient>> loader) {
        if (loader.getId() == LOADER_ID_FILTERING) {
            adapter.setHighlight(null);
            adapter.setRecipients(null);
        }
    }

    public boolean tryPerformCompletion() {
        if (!hasUncompletedText()) {
            return false;
        }
        int previousNumRecipients = getTokenCount();
        performCompletion();
        int numRecipients = getTokenCount();

        return previousNumRecipients != numRecipients;
    }

    private int getTokenCount() {
        return getObjects().size();
    }

    public boolean hasUncompletedText() {
        String currentCompletionText = currentCompletionText();
        return !TextUtils.isEmpty(currentCompletionText) && !isPlaceholderText(currentCompletionText);
    }

    static private boolean isPlaceholderText(String currentCompletionText) {
        // TODO string matching here is sort of a hack, but it's somewhat reliable and the info isn't easily available
        return currentCompletionText.startsWith("+") && currentCompletionText.substring(1).matches("[0-9]+");
    }

    @Override
    public void onRecipientRemove(Recipient currentRecipient) {
        alternatesPopup.dismiss();
        removeRecipient(currentRecipient);
    }

    @Override
    public void onRecipientChange(Recipient recipientToReplace, Recipient alternateAddress) {
        alternatesPopup.dismiss();

        List<Recipient> currentRecipients = getObjects();
        int indexOfRecipient = currentRecipients.indexOf(recipientToReplace);
        if (indexOfRecipient == -1) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }
        Recipient currentRecipient = currentRecipients.get(indexOfRecipient);

        currentRecipient.setAddress(alternateAddress.getAddress());
        currentRecipient.setAddressLabel(alternateAddress.getAddressLabel());
        currentRecipient.setCryptoStatus(alternateAddress.getCryptoStatus());

        View recipientTokenView = getTokenViewForRecipient(currentRecipient);
        if (recipientTokenView == null) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }

        bindObjectView(currentRecipient, recipientTokenView);

        if (listener != null) {
            listener.onTokenChanged(currentRecipient);
        }
    }

    /**
     * This method builds the span given a recipient object. We override it with identical
     * functionality, but using the custom RecipientTokenSpan class which allows us to
     * retrieve the view for redrawing at a later point.
     */
    @Override
    protected TokenImageSpan buildSpanForObject(Recipient obj) {
        if (obj == null) {
            return null;
        }

        View tokenView = getViewForObject(obj);
        return new RecipientTokenSpan(tokenView, obj, (int) maxTextWidth());
    }

    /**
     * Find the token view tied to a given recipient. This method relies on spans to
     * be of the RecipientTokenSpan class, as created by the buildSpanForObject method.
     */
    private View getTokenViewForRecipient(Recipient currentRecipient) {
        Editable text = getText();
        if (text == null) {
            return null;
        }

        RecipientTokenSpan[] recipientSpans = text.getSpans(0, text.length(), RecipientTokenSpan.class);
        for (RecipientTokenSpan recipientSpan : recipientSpans) {
            if (recipientSpan.getToken().equals(currentRecipient)) {
                return recipientSpan.view;
            }
        }

        return null;
    }

    /**
     * We use a specialized version of TokenCompleteTextView.TokenListener as well,
     * adding a callback for onTokenChanged.
     */
    public void setTokenListener(TokenListener<Recipient> listener) {
        this.listener = listener;
    }

    public void notifyDatasetChanged() {
        alternatesAdapter.notifyDataSetChanged();
        adapter.notifyDataSetChanged();
    }

    public enum RecipientCryptoStatus {
        UNDEFINED,
        UNAVAILABLE,
        AVAILABLE_UNTRUSTED,
        AVAILABLE_TRUSTED;

        public boolean isAvailable() {
            return this == AVAILABLE_TRUSTED || this == AVAILABLE_UNTRUSTED;
        }
    }

    public interface TokenListener<T> extends TokenCompleteTextView.TokenListener<T> {
        void onTokenChanged(T token);
        void updateRecipientSecurity(boolean unsecure);
    }

    private class RecipientTokenSpan extends TokenImageSpan {
        private final View view;


        public RecipientTokenSpan(View view, Recipient recipient, int token) {
            super(view, recipient, token);
            this.view = view;
        }
    }

    private ApplicationComponent getApplicationComponent(Context context) {
        return ((K9) context.getApplicationContext()).getComponent();
    }
}
