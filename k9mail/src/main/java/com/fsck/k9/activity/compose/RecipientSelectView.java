package com.fsck.k9.activity.compose;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.activity.AlternateRecipientAdapter;
import com.fsck.k9.activity.AlternateRecipientAdapter.AlternateRecipientListener;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.planck.PlanckProvider;
import com.fsck.k9.planck.PlanckUIArtefactCache;
import com.fsck.k9.ui.contacts.ContactPictureLoader;
import com.tokenautocomplete.CountSpan;
import com.tokenautocomplete.TokenCompleteTextView;

import org.apache.james.mime4j.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.scopes.ViewScoped;
import foundation.pEp.jniadapter.Rating;
import timber.log.Timber;

@AndroidEntryPoint
@ViewScoped
public class RecipientSelectView extends TokenCompleteTextView<Recipient> implements LoaderCallbacks<List<Recipient>>,
        AlternateRecipientListener, RecipientSelectContract {

    private static final int MINIMUM_LENGTH_FOR_FILTERING = 1;

    private static final String ARG_QUERY = "query";

    private static final int LOADER_ID_FILTERING = 0;
    private static final int LOADER_ID_ALTERNATES = 1;
    private static final long RECIPIENT_HOLDER_RATING_UPDATE_DELAY = 100L;


    private RecipientAdapter adapter;
    @Nullable
    private String cryptoProvider;
    @Nullable
    private LoaderManager loaderManager;

    private ListPopupWindow alternatesPopup;
    private Recipient alternatesPopupRecipient;
    private Context context;
    private Account account;
    private PlanckUIArtefactCache uiCache;
    private boolean alwaysUnsecure;
    private TextView errorTextView;


    @Inject ContactPictureLoader contactPictureLoader;
    @Inject AlternateRecipientAdapter alternatesAdapter;
    @Inject
    RecipientSelectPresenter recipientSelectPresenter;

    public RecipientSelectView(Context context) {
        super(context);
        initView(context, null, 0);
    }

    public RecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs, 0);
    }

    public RecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context, attrs, defStyle);
    }

    private void initView(Context context, AttributeSet attrs, int defStyle) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.RecipientSelectView, defStyle, 0);
            alwaysUnsecure = a.getBoolean(
                    R.styleable.RecipientSelectView_alwaysUnsecure, false);
            a.recycle();
        }
        // TODO: validator?
        this.context = context;

        uiCache = PlanckUIArtefactCache.getInstance(context);

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

        setLongClickable(false);
        recipientSelectPresenter.initialize(this);

        setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == EditorInfo.IME_ACTION_NEXT)) {
                if (hasUncompletedRecipients()) {
                    performCompletion();
                    return true;
                }
            }
            return false;
        });

        setSplitChar(new char[]{',', ';', ' '});

        setTokenListener(new TokenCompleteTextView.TokenListener<Recipient>() {
            @Override
            public void onTokenAdded(Recipient token) {
                recipientSelectPresenter.onRecipientsChanged();
            }

            @Override
            public void onTokenRemoved(Recipient token) {
                recipientSelectPresenter.removeUnsecureAddressChannel(token.getAddress());
                recipientSelectPresenter.handleUnsecureTokenWarning();
                recipientSelectPresenter.onRecipientsChanged();
            }
        });
    }

    public void setErrorTextView(TextView errorTextView) {
        this.errorTextView = errorTextView;
    }

    @Override
    public void showUncompletedError() {
        setError(getContext().getString(R.string.compose_error_incomplete_recipient));
    }

    @Override
    public void showNoRecipientsError() {
        setError(getContext().getString(R.string.message_compose_error_no_recipients));
    }

    @Override
    public void restoreFirstRecipientTruncation() {
        post(() -> {
            if (!hasFocus()) {
                resetFocus();
            }
        });
    }

    private void resetFocus() {
        requestFocus();
        clearFocus();
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

        recipientSelectPresenter.getRecipientRating(
                recipient,
                account.isPlanckPrivacyProtected(),
                new PlanckProvider.ResultCallback<Rating>() {
            @Override
            public void onLoaded(Rating rating) {
                recipientSelectPresenter.handleUnsecureTokenWarning();
                setCountColorIfNeeded();
                updateRecipientHolderRating(holder, rating);
            }

            @Override
            public void onError(Throwable throwable) {
                recipientSelectPresenter.handleUnsecureTokenWarning();
                setCountColorIfNeeded();
                updateRecipientHolderRating(holder, Rating.pEpRatingUndefined);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility") //Comes from library
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        int action = event.getActionMasked();
        Editable text = getText();

        if (text != null && action == MotionEvent.ACTION_UP) {
            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (offset != -1) {
                TokenImageSpan[] links = text.getSpans(offset, offset, RecipientTokenSpan.class);
                if (links.length > 0) {
                    TokenImageSpan span = links[0];
                    Recipient recipient = span.getToken();
                    if (isRemoveRecipientClicked(event, span)) {
                        removeRecipientAndResetView(recipient);
                    } else {
                        showAlternates(recipient);
                    }
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }

    private boolean isRemoveRecipientClicked(
        MotionEvent event,
        TokenImageSpan span
    ) {
        float touchAreaExtension = getResources().getDimension(R.dimen.remove_recipient_button_padding);

        Recipient recipient = span.getToken();
        RecipientTokenViewHolder.ViewLocation viewLocation = getRemoveButtonLocationForRecipient(recipient);

        Layout layout = getLayout();
        Editable text = getText();
        if (viewLocation != null && layout != null && text != null) {
            float spanX = layout.getPrimaryHorizontal(text.getSpanStart(span));
            float absoluteX = spanX + viewLocation.getX();
            float absoluteY = viewLocation.getY();

            if (event.getX() + touchAreaExtension >= absoluteX
                    && event.getX() - touchAreaExtension - absoluteX <= viewLocation.getWidth()
            ) {
                int heightOffset = getHeightOffsetForSpan(span);
                if (heightOffset >= 0) {
                    float realTop = absoluteY + heightOffset;
                    return (event.getY() + touchAreaExtension >= realTop
                            && event.getY() - touchAreaExtension - realTop <= viewLocation.getHeight());
                }
            }
        }
        return false;
    }

    private int getHeightOffsetForSpan(TokenImageSpan span) {
        int lines = getLineCount();
        int spanStart = getText().getSpanStart(span);
        int spanEnd = getText().getSpanEnd(span);
        for (int line = 0; line < lines; line++) {
            if (getLayout().getLineStart(line) <= spanStart
                && getLayout().getLineEnd(line) >= spanEnd) {
                return getLineOffset(getLayout(), line);
            }
        }
        return -1;
    }

    private int realLineHeight(Layout layout, int line) {
        return layout.getLineBottom(line) - layout.getLineTop(line);
    }

    private int getLineOffset(Layout layout, int currentLine) {
        int realLineHeight = 0;
        for (int line = 0; line < currentLine; line++) {
            realLineHeight += realLineHeight(layout, line);
        }
        return realLineHeight;
    }

    /**
     * Find location data of the "x" remove button placed in the view tied to a given recipient.
     * This method relies on spans to be of the RecipientTokenSpan class, as created by the
     * buildSpanForObject method.
     */
    private RecipientTokenViewHolder.ViewLocation getRemoveButtonLocationForRecipient(Recipient currentRecipient) {
        Editable text = getText();
        if (text != null) {
            RecipientTokenSpan[] spans = text.getSpans(
                    0,
                    text.length(), RecipientTokenSpan.class
            );
            for (RecipientTokenSpan span : spans) {
                if (span.getToken() == currentRecipient) {
                    return span.getRemoveButtonLocation();
                }
            }
        }
        return null;
    }

    @Override
    public void performCollapse(boolean hasFocus) {
        if (!hasFocus) {
            truncateFirstDisplayNameIfNeeded();
            super.performCollapse(false);
            setCountColorIfNeeded();
        } else {
            restoreFirstDisplayNameIfNeeded();
            super.performCollapse(true);
        }
    }

    private void restoreFirstDisplayNameIfNeeded() {
        List<Recipient> recipients = getObjects();
        if (!recipients.isEmpty()) {
            restoreRecipientDisplayName(recipients.get(0));
        }
    }

    private void restoreRecipientDisplayName(Recipient recipient) {
        View recipientTokenView = getTokenViewForRecipient(recipient);
        if (recipientTokenView == null) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }
        RecipientTokenViewHolder vh = (RecipientTokenViewHolder) recipientTokenView.getTag();
        vh.restoreNameSize();
    }

    private void truncateFirstDisplayNameIfNeeded() {
        if (getTokenCount() >= 2) {
            Editable text = getText();
            Layout lastLayout = getLayout();
            if (text != null && lastLayout != null) {
                int lastPosition = lastLayout.getLineVisibleEnd(0);
                TokenImageSpan[] tokens = text.getSpans(0, lastPosition, TokenImageSpan.class);
                int count = getTokenCount() - tokens.length;
                if (tokens.length == 1) {
                    CountSpan[] countSpans = text.getSpans(0, lastPosition, CountSpan.class);
                    if (count > 0 && countSpans.length == 0) {
                        truncateFirstDisplayName(lastLayout, count);
                    }
                }
            }
        }
    }

    private void setCountColorIfNeeded() {
        Editable text = getText();
        if(text != null) {
            CountSpan[] countSpans = text.getSpans(0, text.length(), CountSpan.class);
            if (countSpans.length > 0) {
                CountSpan countSpan = countSpans[0];
                if(text.getSpanStart(countSpan) >= 0) {
                    try {
                        int count = Integer.parseInt(countSpan.text.substring(1));
                        setCountColor(text, countSpan, count);
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        }
    }

    private void setCountColor(Editable editable, CountSpan countSpan, int count) {
        boolean unsecure = recipientSelectPresenter.hasHiddenUnsecureAddressChannel(
                getAddresses(),
                count
        );
        int countColor = unsecure
                ? ContextCompat.getColor(
                        context, R.color.compose_unsecure_delivery_warning)
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

    private void truncateFirstDisplayName(Layout lastLayout, int count) {
        Recipient firstRecipient = findFirstVisibleRecipient();
        String countText = "+" + count;
        String textToDisplay = firstRecipient.getDisplayNameOrAddress();
        float requiredWidth = lastLayout.getPaint().measureText(textToDisplay + countText);
        int maxTextWidth = availableTextWidthWithRemoveButton();
        if (maxTextWidth < requiredWidth) {
            do {
                textToDisplay = textToDisplay.substring(0, textToDisplay.length() - 1);
                requiredWidth = lastLayout.getPaint().measureText(textToDisplay + countText);
            } while (maxTextWidth < requiredWidth);
            truncateRecipientDisplayName(firstRecipient, textToDisplay.length() - 1);
        }
    }

    private int availableTextWidthWithRemoveButton() {
        return (int) (maxTextWidth() - getResources().getDimension(
                R.dimen.first_recipient_token_end_reserved_space));
    }

    private Recipient findFirstVisibleRecipient() {
        Editable text = getText();
        RecipientTokenSpan[] spans = getText().getSpans(
                0, getText().length(), RecipientTokenSpan.class);
        int firstSpanStart = spans.length;
        Recipient out = spans[0].getToken();
        for (RecipientTokenSpan span : spans) {
            if (text.getSpanStart(span) < firstSpanStart) {
                firstSpanStart = text.getSpanStart(span);
                out = span.getToken();
            }
        }
        return out;
    }

    private void truncateRecipientDisplayName(Recipient recipient, int limit) {
        View recipientTokenView = getTokenViewForRecipient(recipient);
        if (recipientTokenView == null) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }
        RecipientTokenViewHolder vh = (RecipientTokenViewHolder) recipientTokenView.getTag();
        vh.truncateName(limit);
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

    @Override
    public void setError(CharSequence error) {
        if (errorTextView == null) return;
        if (error == null) {
            errorTextView.setVisibility(View.GONE);
            errorTextView.setText(null);
        } else {
            errorTextView.setText(error);
            errorTextView.setVisibility(View.VISIBLE);
        }
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
            setError(null);
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
        recipientSelectPresenter.addRecipients(recipients);
    }

    public void setPresenter(RecipientPresenter presenter, Message.RecipientType type) {
        recipientSelectPresenter.setPresenter(presenter, type);
    }

    @Override
    public void addRecipient(@NonNull Recipient recipient) {
        addObject(recipient);
    }

    @NonNull
    public Address[] getAddresses() {
        List<Recipient> recipients = getObjects();
        Address[] address = new Address[recipients.size()];
        for (int i = 0; i < address.length; i++) {
            address[i] = recipients.get(i).getAddress();
        }

        return address;
    }

    @NonNull
    @Override
    public List<Recipient> getRecipients() {
        return getObjects();
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
        new Handler().post(() -> recipientSelectPresenter.rateAlternateRecipients(data));
    }

    @Override
    public void showAlternatesPopup(@NonNull List<RatedRecipient> data) {
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

        alternatesAdapter.setCurrentRecipient(
                findCurrentRatedRecipient(data, alternatesPopupRecipient));
        alternatesAdapter.setAlternateRecipientInfo(data);

        // Clear the checked item.
        alternatesPopup.show();
        ListView listView = alternatesPopup.getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    private RatedRecipient findCurrentRatedRecipient(
            List<RatedRecipient> recipients,
            Recipient currentRecipient
    ) {
        for (RatedRecipient recipient : recipients) {
            if (recipient.getBaseRecipient().equals(currentRecipient)) {
                return recipient;
            }
        }
        return null;
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

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (errorTextView != null && errorTextView.getVisibility() != View.GONE) {
            setError(null);
        }
    }

    public void removeRecipientAndResetView(Recipient recipient) {
        removeRecipient(recipient);
        resetCollapsedViewIfNeeded();
    }

    @Override
    public void removeRecipient(@NonNull Recipient recipient) {
        removeObject(recipient);
    }

    @Override
    public void resetCollapsedViewIfNeeded() {
        post(() -> {
            if (!hasFocus()) {
                if (getTokenCount() == 1) {
                    requestFocus();
                } else if (getTokenCount() > 1) {
                    resetFocus();
                }
            }
        });
    }

    @NotNull
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
    public void onLoadFinished(@NotNull Loader<List<Recipient>> loader, List<Recipient> data) {
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

    @Override
    public boolean tryPerformCompletion() {
        if (!hasUncompletedRecipients()) {
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

    @Override
    public boolean hasUncompletedRecipients() {
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
        removeRecipientAndResetView(currentRecipient);
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
        for (Recipient object : getObjects()) {
            if (object.getAddress().equals(alternateAddress.getAddress())) {
                Timber.e("Recipient already present!");
                return;
            }
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

        recipientSelectPresenter.removeUnsecureAddressChannel(recipientToReplace.getAddress());
        bindObjectView(currentRecipient, recipientTokenView);

        recipientSelectPresenter.onRecipientsChanged();
    }

    @Override
    public void updateRecipients(List<RatedRecipient> recipients) {
        for (RatedRecipient recipient : recipients) {
            setCountColorIfNeeded();
            RecipientTokenViewHolder holder = getRecipientHolder(recipient.getBaseRecipient());
            if (holder == null) {
                Timber.e("Tried to refresh invalid view token!");
            } else {
                updateRecipientHolderRating(holder, recipient.getRating());
            }
        }
    }

    private void updateRecipientHolderRating(RecipientTokenViewHolder holder, Rating rating) {
        holder.updateRating(rating);
        postInvalidateDelayed(RECIPIENT_HOLDER_RATING_UPDATE_DELAY);
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
        String email = obj.getAddress().getAddress().toLowerCase();
        String personal = obj.getAddress().getPersonal();
        obj.setAddress(new Address(email, personal));

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

    private RecipientTokenViewHolder getRecipientHolder(Recipient currentRecipient) {
        View view = getTokenViewForRecipient(currentRecipient);
        return view != null
                ? (RecipientTokenViewHolder) view.getTag()
                : null;
    }

    @Override
    public boolean hasRecipient(@NonNull Recipient recipient) {
        return getObjects().contains(recipient);
    }

    @Override
    public boolean isAlwaysUnsecure() {
        return alwaysUnsecure;
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

    private class RecipientTokenSpan extends TokenImageSpan {
        private final View view;


        public RecipientTokenSpan(View view, Recipient recipient, int token) {
            super(view, recipient, token);
            this.view = view;
        }

        public RecipientTokenViewHolder.ViewLocation getRemoveButtonLocation() {
            RecipientTokenViewHolder holder = (RecipientTokenViewHolder) view.getTag();
            return holder.getRemoveButtonLocation();
        }
    }
}
