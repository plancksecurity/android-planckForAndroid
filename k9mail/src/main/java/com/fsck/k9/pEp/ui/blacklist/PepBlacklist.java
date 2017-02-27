package com.fsck.k9.pEp.ui.blacklist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.pEp.PEpProvider;
import com.fsck.k9.pEp.ui.tools.FeedbackTools;
import com.fsck.k9.pEp.ui.tools.KeyboardUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by huss on 31/08/16.
 */

public class PepBlacklist extends AppCompatActivity implements SearchView.OnQueryTextListener {

    @Bind(R.id.my_recycler_view)
    RecyclerView recipientsView;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.pep_blacklist_layout)
    LinearLayout container;



    KeysAdapter recipientsAdapter;
    RecyclerView.LayoutManager recipientsLayoutManager;

    PEpProvider pEp;
    private List<KeyListItem> keys;
    private View searchLayout;
    private EditText searchInput;
    private View clearSearchIcon;


    public static void actionShowBlacklist(Context context) {
        Intent i = new Intent(context, PepBlacklist.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pep_blacklist);
        ButterKnife.bind(PepBlacklist.this);
        pEp = ((K9) getApplication()).getpEpProvider();
        recipientsLayoutManager = new LinearLayoutManager(this);
        ((LinearLayoutManager) recipientsLayoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        recipientsView.setLayoutManager(recipientsLayoutManager);
        recipientsView.setVisibility(View.VISIBLE);
        keys = pEp.getAvailableKey();
        initializeKeysView();
        setSupportActionBar(toolbar);
        initializeSearchBar();
    }

    private void initializeKeysView() {
        recipientsAdapter = new KeysAdapter(this, keys);
        recipientsView.setAdapter(recipientsAdapter);
        recipientsAdapter.notifyDataSetChanged();
    }

    private void initializeSearchBar() {
        searchLayout = findViewById(R.id.toolbar_search_container);
        searchInput = (EditText) findViewById(R.id.search_input);
        clearSearchIcon = findViewById(R.id.search_clear);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence query, int start, int before, int count) {
                if (query.toString().isEmpty()) {
                    clearSearchIcon.setVisibility(View.GONE);
                    initializeKeysView();
                } else {
                    clearSearchIcon.setVisibility(View.VISIBLE);
                    onQueryTextSubmit(searchInput.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!searchInput.getText().toString().isEmpty()) {
                    onQueryTextSubmit(searchInput.getText().toString());
                }
                return true;
            }
        });

        clearSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText(null);
                hideSearchView();
                KeyboardUtils.hideKeyboard(searchInput);
                initializeKeysView();
            }
        });
    }

    public void hideSearchView() {
        if (searchLayout != null) {
            toolbar.setVisibility(View.VISIBLE);
            searchLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pep_search, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);

        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        final List<KeyListItem> filteredModelList = filter(keys, query);
        recipientsAdapter.replaceAll(filteredModelList);
        recipientsView.scrollToPosition(0);
        return true;
    }

    private static List<KeyListItem> filter(List<KeyListItem> models, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        final List<KeyListItem> filteredModelList = new ArrayList<>();
        for (KeyListItem model : models) {
            final String text = model.getGpgUid().toLowerCase();
            if (text.contains(lowerCaseQuery)
                    || model.getFpr().toLowerCase().contains(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_search:
                showSearchView();
                return true;
            case R.id.action_add_fpr:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.fpr_dialog, null);
                dialogBuilder.setView(dialogView);

                final EditText fpr = (EditText) dialogView.findViewById(R.id.fpr_text);

                dialogBuilder.setTitle("Add FPR");
                dialogBuilder.setPositiveButton("Done", (dialog, whichButton) -> {
                    addFingerprintToBlacklist(fpr);
                });
                dialogBuilder.setNegativeButton("Cancel", (dialog, whichButton) -> {
                });
                AlertDialog b = dialogBuilder.create();
                b.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addFingerprintToBlacklist(EditText fpr) {
        String fingerprint = fpr.getText().toString().toUpperCase().replaceAll(" ", "");
        Pattern pattern = Pattern.compile("^[0-9A-F]+$");
        Matcher matcher = pattern.matcher(fingerprint);
        if (matcher.find() && fingerprint.length() >= 40) {
            for (KeyListItem key : keys) {
                if (key.fpr.equals(fingerprint)) {
                    pEp.addToBlacklist(fingerprint);
                }
            }
            keys = pEp.getAvailableKey();
            initializeKeysView();
        } else {
            FeedbackTools.showShortFeedback(container, "Wrong FPR");
        }
    }

    public void showSearchView() {
        if (searchLayout != null) {
            toolbar.setVisibility(View.GONE);
            searchLayout.setVisibility(View.VISIBLE);
            setFocusOnKeyboard();
        }
    }

    private void setFocusOnKeyboard() {
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }
}
