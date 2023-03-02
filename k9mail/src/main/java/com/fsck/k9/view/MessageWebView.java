package com.fsck.k9.view;


import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.util.AttributeSet;
import timber.log.Timber;
import android.view.KeyEvent;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;

import com.fsck.k9.K9;
import com.fsck.k9.message.html.HtmlConverter;
import com.fsck.k9.pEp.ui.tools.Theme;
import com.fsck.k9.R;
import com.fsck.k9.mailstore.AttachmentResolver;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.ThemeManager;
import com.fsck.k9.preferences.Settings;


public class MessageWebView extends RigidWebView {

    private String NEW_BODY_START = "<head><style type=\"text/css\">body{color: " +
            getWebviewTextColor() +
            ";}</style></head><body style=\"overflow-wrap: break-word; word-wrap: break-word;\">";

    private OnHtmlSetListener onHtmlSetListener;

    public MessageWebView(Context context) {
        super(context);
    }

    public MessageWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MessageWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void refreshTheme() {
        if(ThemeManager.isDarkTheme() && WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        }
    }

    /**
     * Configure a web view to load or not load network data. A <b>true</b> setting here means that
     * network data will be blocked.
     * @param shouldBlockNetworkData True if network data should be blocked, false to allow network data.
     */
    public void blockNetworkData(final boolean shouldBlockNetworkData) {
        /*
         * Block network loads.
         *
         * Images with content: URIs will not be blocked, nor
         * will network images that are already in the WebView cache.
         *
         */
        getSettings().setBlockNetworkLoads(shouldBlockNetworkData);
    }


    /**
     * Configure a {@link WebView} to display a Message. This method takes into account a user's
     * preferences when configuring the view. This message is used to view a message and to display a message being
     * replied to.
     */
    public void configure() {
        this.setVerticalScrollBarEnabled(true);
        this.setVerticalScrollbarOverlay(true);
        this.setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        this.setLongClickable(true);

        if (ThemeManager.getMessageViewTheme() == Theme.DARK) {
            // Black theme should get a black webview background
            // we'll set the background of the messages on load
            int color = ThemeManager.getColorFromAttributeResource(getContext(), R.attr.screenDefaultBackgroundColor);
            this.setBackgroundColor(color);
        }

        final WebSettings webSettings = this.getSettings();

        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        clearCache(true);
        /* TODO this might improve rendering smoothness when webview is animated into view
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            webSettings.setOffscreenPreRaster(true);
        }
        */

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        if (K9.autofitWidth()) {
            webSettings.setLoadWithOverviewMode(true);
            setInitialScale((int) getScale());
        }

        disableDisplayZoomControls();

        webSettings.setJavaScriptEnabled(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setRenderPriority(RenderPriority.HIGH);

        // TODO:  Review alternatives.  NARROW_COLUMNS is deprecated on KITKAT
//        webSettings.setLayoutAlgorithm(LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setUseWideViewPort(true);

        setOverScrollMode(OVER_SCROLL_NEVER);

        webSettings.setTextZoom(K9.getFontSizes().getMessageViewContentAsPercent());

        // Disable network images by default.  This is overridden by preferences.
        blockNetworkData(true);
    }

    /**
     * Disable on-screen zoom controls on devices that support zooming via pinch-to-zoom.
     */
    private void disableDisplayZoomControls() {
        PackageManager pm = getContext().getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);

        getSettings().setDisplayZoomControls(!supportsMultiTouch);
    }

    public void displayHtmlContentWithInlineAttachments(
            @NonNull String htmlText,
            @Nullable AttachmentResolver attachmentResolver,
            @Nullable OnPageFinishedListener onPageFinishedListener) {
        setWebViewClient(attachmentResolver, onPageFinishedListener);
        setHtmlContent(htmlText);
    }

    public void setOnHtmlSetListener(OnHtmlSetListener onHtmlSetListener) {
        this.onHtmlSetListener = onHtmlSetListener;
    }

    private void setWebViewClient(@Nullable AttachmentResolver attachmentResolver,
            @Nullable OnPageFinishedListener onPageFinishedListener) {
        K9WebViewClient webViewClient = K9WebViewClient.newInstance(attachmentResolver);
        if (onPageFinishedListener != null) {
            webViewClient.setOnPageFinishedListener(onPageFinishedListener);
        }
        setWebViewClient(webViewClient);
    }

    private String forceBreakWordsHeader(String htmlText) {
        //change body start tag
        return htmlText.replace("<body>", NEW_BODY_START);
    }

    @NonNull
    private String getWebviewTextColor() {
        //There is no straightforward method to just set the text color in a webview
        //as it can be done with the background color.
        //There is a way to replace HEAD Style for making the webview apply a text color
        return ThemeManager.isDarkTheme() ?
                getHexColorByResourceId(R.color.dark_theme_text_color_primary) :
                getHexColorByResourceId(R.color.text_for_white_background);
    }

    @NonNull
    private String getHexColorByResourceId(int color) {
        return Settings.ColorSetting.formatColor(getResources().getColor(color));
    }

    private void setHtmlContent(@NonNull String htmlText) {
        String html = forceBreakWordsHeader(htmlText);
        html = removeAllHttp(html);
        if (onHtmlSetListener != null) {
            String text = HtmlConverter.htmlToText(html);
            onHtmlSetListener.onHtmlSet(text);
        }
        loadDataWithBaseURL("about:blank", html, "text/html", "utf-8", null);
        resumeTimers();
    }

    private String removeAllHttp(String html) {
        return html.replace("http://", "https://");
    }

    /*
     * Emulate the shift key being pressed to trigger the text selection mode
     * of a WebView.
     */
    public void emulateShiftHeld() {
        try {

            KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                                                    KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
            shiftPressEvent.dispatch(this, null, null);
            FeedbackTools.showLongFeedback(getRootView(), getContext().getString(R.string.select_text_now));
        } catch (Exception e) {
            Timber.e(e, "Exception in emulateShiftHeld()");
        }
    }

    public interface OnPageFinishedListener {
        void onPageFinished(WebView webView);
    }

    public interface OnHtmlSetListener {
        void onHtmlSet(String htmlText);
    }
}
