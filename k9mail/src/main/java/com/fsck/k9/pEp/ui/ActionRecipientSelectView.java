package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.fsck.k9.activity.compose.RecipientSelectView;

import org.jetbrains.annotations.NotNull;

import security.pEp.ui.input.utils.InputConnectionProvider;
import security.pEp.ui.input.utils.InputConnectionProviderImpl;

public class ActionRecipientSelectView extends RecipientSelectView {

    private InputConnectionProvider inputConnectionProvider;

    public interface OnCutCopyPasteListener {
        void onCut();

        void onCopy();
    }

    private OnCutCopyPasteListener mOnCutCopyPasteListener;

    public void setOnCutCopyPasteListener(OnCutCopyPasteListener listener) {
        mOnCutCopyPasteListener = listener;
    }

    public ActionRecipientSelectView(Context context) {
        super(context);
        inputConnectionProvider = new InputConnectionProviderImpl(context);
    }

    public ActionRecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inputConnectionProvider = new InputConnectionProviderImpl(context);
    }

    public ActionRecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inputConnectionProvider = new InputConnectionProviderImpl(context);
    }

    /**
     * <p>This is where the "magic" happens.</p>
     * <p>The menu used to cut/copy/paste is a normal ContextMenu, which allows us to
     *  overwrite the consuming method and react on the different events.</p>
     * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3_r1/android/widget/TextView.java#TextView.onTextContextMenuItem%28int%29">Original Implementation</a>
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        // React:
        switch (id) {
            case android.R.id.cut:
                onCut();
                break;
            case android.R.id.copy:
                super.onTextContextMenuItem(id);
                onCopy();
                break;
            case android.R.id.paste:
            case android.R.id.selectAll:
                super.onTextContextMenuItem(id);
                break;
        }
        return true;
    }

    public void onCut() {
        if (mOnCutCopyPasteListener != null)
            mOnCutCopyPasteListener.onCut();
    }

    public void onCopy() {
        if (mOnCutCopyPasteListener != null)
            mOnCutCopyPasteListener.onCopy();
    }


    @Override
    public InputConnection onCreateInputConnection(@NotNull EditorInfo editorInfo) {
        InputConnection ic = super.onCreateInputConnection(editorInfo);
        return inputConnectionProvider.provideInputConnection(ic, editorInfo);
    }
}
