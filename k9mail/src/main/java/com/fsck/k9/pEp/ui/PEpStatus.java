package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpStatus extends PepColoredActivity implements ChangeColorListener{

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String MYSELF = "isComposedKey";

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    RecyclerView.Adapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String myself = "";


    public static void actionShowStatus(Context context, Color currentColor, String myself) {
        Intent i = new Intent(context, PEpStatus.class);
        i.setAction(ACTION_SHOW_PEP_STATUS);
        i.putExtra(CURRENT_COLOR, currentColor.toString());
        i.putExtra(MYSELF, myself);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPepColor();
        setContentView(R.layout.pep_status);
        ButterKnife.bind(PEpStatus.this);
        if (getIntent() != null && getIntent().hasExtra(MYSELF)) {
            myself = getIntent().getStringExtra(MYSELF);
        }
        initPep();
        setUpActionBar();
        setUpContactList(myself, getpEp());
        loadPepTexts();

    }


    private void loadPepTexts() {
        pEpTitle.setText(uiCache.getTitle(getpEpColor()));
        pEpSuggestion.setText(uiCache.getExplanation(getpEpColor()));
    }

    private void setUpActionBar() {
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(getString(R.string.title_activity_pep_status));
            colorActionBar();
            colorActionBar();
        }
    }

    private void colorActionBar() {
        ActionBar actionBar = getActionBar() ;
        if (actionBar != null) {
            PEpUtils.colorActionBar(ui, actionBar, m_pEpColor);
        }
    }


    private void setUpContactList(String myself, PEpProvider pEp) {
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsAdapter = new RecipientsAdapter(this, uiCache.getRecipients(), pEp, myself, this);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @Override
    public void colorChanged(Color pEpColor) {
        setpEpColor(pEpColor);
        colorActionBar();
    }

    public class SimpleDividerItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public SimpleDividerItemDecoration(Context context) {
            mDivider = ContextCompat.getDrawable(context, R.drawable.line_divider);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PEpTrustwords.HANDSHAKE_REQUEST) {
            if (resultCode == RESULT_OK) {
               int position = data.getIntExtra(PEpTrustwords.PARTNER_POSITION, PEpTrustwords.DEFAULT_POSITION);
                Identity partner = uiCache.getRecipients().get(position);
                setpEpColor(getpEp().identityColor(partner));
                recipientsAdapter.notifyDataSetChanged();
                colorActionBar();


            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pep_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_explanation:
                showExplanationDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showExplanationDialog() {
         new AlertDialog.Builder(this)
                .setTitle(R.string.pep_explanation)
                .setMessage(uiCache.getExplanation(getpEpColor()))
                .setPositiveButton(R.string.okay_action,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .create()
                 .show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        colorActionBar();
    }


}
