package com.fsck.k9.pEp.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.PePUIArtefactCache;
import org.pEp.jniadapter.Color;
import org.pEp.jniadapter.Identity;

public class PEpStatus extends K9Activity {

    private static final String ACTION_SHOW_PEP_STATUS = "com.fsck.k9.intent.action.SHOW_PEP_STATUS";
    private static final String CURRENT_COLOR = "current_color";
    private static final String MYSELF = "isComposedKey";
    private Color m_pEpColor = Color.pEpRatingB0rken;
    PePUIArtefactCache ui;

    @Bind(R.id.pEpShortDesc)
    TextView pEpShortDesc;
    @Bind(R.id.pEpLongText)
    TextView pEpLongText;
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
        renderStatusBarPepColor();

    }

    private void initPep() {
        ui = PePUIArtefactCache.getInstance(getApplicationContext());
        pEp = ((K9) getApplication()).getpEpProvider();
    }

    private void loadPepTexts() {
        pEpShortDesc.setText(ui.getTitle(m_pEpColor));
        pEpLongText.setText(ui.getDescription(m_pEpColor));
    }

    private void setUpActionBar() {
        ColorDrawable colorDrawable = new ColorDrawable(ui.getColor(m_pEpColor));
        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();
            actionBar.setBackgroundDrawable(colorDrawable);
            actionBar.setTitle(getString(R.string.title_activity_pep_status));
            actionBar.setSubtitle(ui.getTitle(m_pEpColor));
            fixActionBarTitleColor();
            fixActionBarSubtitleColor();

        }
    }

    private void fixActionBarSubtitleColor() {
        TextView actionBarSubTitle = (TextView) findViewById(getActionBarSubTitleId());
        if (ui.getColor(m_pEpColor) == getResources().getColor(R.color.pep_green)) {
            actionBarSubTitle.setTextColor(actionBarSubTitle.getCurrentTextColor() + 0x00111111);
        }
    }

    private void fixActionBarTitleColor() {
        TextView actionBarTitle = (TextView) findViewById(getActionBarTitleId());
        if (ui.getColor(m_pEpColor) == getResources().getColor(R.color.pep_green)) {
            actionBarTitle.setTextColor(android.graphics.Color.WHITE);
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
        recipientsAdapter = new RecipientsAdapter(this, ui.getRecipients(), pEp, myself);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsView.addItemDecoration(new SimpleDividerItemDecoration(this));

    }

    private int getActionBarTitleId() {
        return getResources().getIdentifier("action_bar_title", "id", "android");
    }

    private int getActionBarSubTitleId() {
        return getResources().getIdentifier("action_bar_subtitle", "id", "android");
    }


    private void renderStatusBarPepColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color, we set the pEpColor in status bar, but 10% darker.
            int color = (ui.getColor(m_pEpColor) & 0x00FFFFFF);
            float[] hsv = new float[3];
            android.graphics.Color.colorToHSV(color, hsv);
            hsv[2] *= 0.9;
            window.setStatusBarColor(android.graphics.Color.HSVToColor(hsv));
        }
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
                Log.i("PEpStatus", "onActivityResult " + pEp.identityColor(partner));
                recipientsAdapter.notifyDataSetChanged();
            }
        }
    }
}
