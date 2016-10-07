package com.fsck.k9.pEp.ui.renderers;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.pEp.ui.PEpUIUtils;
import com.fsck.k9.pEp.ui.listeners.OnAccountClickListener;
import com.pedrogomez.renderers.Renderer;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AccountRenderer extends Renderer<Account> {

    @Bind(R.id.account_letter) TextView accountLetter;
    @Bind(R.id.account_email) TextView accountEmail;
    private OnAccountClickListener onAccountClickListener;

    @Override
    protected void setUpView(View rootView) {

    }

    @Override
    protected void hookListeners(View rootView) {

    }

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        View inflatedView = inflater.inflate(R.layout.account_navigation_list_item, parent, false);
        ButterKnife.bind(this, inflatedView);
        return inflatedView;
    }

    @Override
    public void render() {
        Account account = getContent();
        accountLetter.setText(PEpUIUtils.firstLetterOf(account.getName()));
        accountEmail.setText(account.getEmail());
    }

    public void setOnAccountClickListenerListener(OnAccountClickListener onAccountClickListener) {
        this.onAccountClickListener = onAccountClickListener;
    }

    @OnClick(R.id.account_layout) void onAccountClicked() {
        onAccountClickListener.onClick(getContent());
    }
}
