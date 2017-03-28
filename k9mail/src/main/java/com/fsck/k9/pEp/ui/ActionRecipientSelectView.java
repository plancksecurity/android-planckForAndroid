package com.fsck.k9.pEp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.fsck.k9.view.RecipientSelectView;

public class ActionRecipientSelectView extends RecipientSelectView {

    public interface OnCutCopyPasteListener {
        void onCut();
        void onCopy();
    }

    private OnCutCopyPasteListener mOnCutCopyPasteListener;

    /**
     * Set a OnCutCopyPasteListener.
     * @param listener
     */
    public void setOnCutCopyPasteListener(OnCutCopyPasteListener listener) {
        mOnCutCopyPasteListener = listener;
    }

    /*
        Just the constructors to create a new EditText...
     */
    public ActionRecipientSelectView(Context context) {
        super(context);
    }

    public ActionRecipientSelectView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActionRecipientSelectView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
        switch (id){
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

    /**
     * Text was cut from this EditText.
     */
    public void onCut(){
        if(mOnCutCopyPasteListener!=null)
            mOnCutCopyPasteListener.onCut();
    }

    /**
     * Text was copied from this EditText.
     */
    public void onCopy(){
        if(mOnCutCopyPasteListener!=null)
            mOnCutCopyPasteListener.onCopy();
    }
}
