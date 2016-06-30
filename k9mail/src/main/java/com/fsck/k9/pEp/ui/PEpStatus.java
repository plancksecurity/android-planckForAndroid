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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PEpUtils;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpStatus extends K9Activity implements ChangeColorListener{

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String CURRENT_COLOR = "current_color";
    private static final String MYSELF = "isComposedKey";
    private Color m_pEpColor = Color.pEpRatingB0rken;
    PePUIArtefactCache ui;

    @Bind(R.id.pEpTitle)
    TextView pEpTitle;
    @Bind(R.id.pEpSuggestion)
    TextView pEpSuggestion;
    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;


    RecyclerView.Adapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    String myself = "";
    private PEpProvider pEp;


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
        setUpContactList(myself, pEp);
        loadPepTexts();

    }

    private void initPep() {
        ui = PePUIArtefactCache.getInstance(getApplicationContext());
        pEp = ((K9) getApplication()).getpEpProvider();
    }

    private void loadPepTexts() {
        pEpTitle.setText(ui.getTitle(m_pEpColor));
        pEpSuggestion.setText(ui.getExplanation(m_pEpColor));
    }

    private void setUpActionBar() {
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(getString(R.string.title_activity_pep_status));
            colorActionBar();
        }
    }

    private void colorActionBar() {
        ActionBar actionBar = getActionBar() ;
        if (actionBar != null) {
            PEpUtils.colorActionBar(ui, actionBar, m_pEpColor);
        }
    }

    private void loadPepColor() {
        final Intent intent = getIntent();
        String colorString;
        if (intent.getExtras() != null) {
            colorString = intent.getStringExtra(CURRENT_COLOR);
            Log.d(K9.LOG_TAG, "Got color:" + colorString);
            m_pEpColor = Color.valueOf(colorString);
        } else {
            throw new RuntimeException("Cannot retrieve pEpColor");
        }
    }

    private void setUpContactList(String myself, PEpProvider pEp) {
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        recipientsAdapter = new RecipientsAdapter(this, ui.getRecipients(), pEp, myself, this);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    @Override
    public void colorChanged(Color pEpColor) {
        m_pEpColor = pEpColor;
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
                Identity partner = ui.getRecipients().get(position);
                m_pEpColor = pEp.identityColor(partner);
                Log.i("PEpStatus", "onActivityResult " + m_pEpColor);
                recipientsAdapter.notifyDataSetChanged();
                colorActionBar();


            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pep_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_explanation:
                showExplanationDialog();
                return true;
        }
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    private void showExplanationDialog() {
         new AlertDialog.Builder(this)
                .setTitle(R.string.pep_explanation)
                .setMessage(ui.getExplanation(m_pEpColor))
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
