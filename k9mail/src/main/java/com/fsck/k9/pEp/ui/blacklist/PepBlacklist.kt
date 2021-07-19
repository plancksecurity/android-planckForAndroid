package com.fsck.k9.pEp.ui.blacklist

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import com.fsck.k9.K9
import com.fsck.k9.R
import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.PepActivity
import com.fsck.k9.pEp.ui.blacklist.PepBlacklist
import com.fsck.k9.pEp.ui.keys.KeyItemAdapter
import com.fsck.k9.pEp.ui.keys.OnKeyClickListener
import com.fsck.k9.pEp.ui.tools.FeedbackTools
import com.fsck.k9.pEp.ui.tools.KeyboardUtils
import com.fsck.k9.pEp.ui.tools.ThemeManager
import kotlinx.coroutines.*
import security.pEp.ui.toolbar.ToolBarCustomizer
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject

class PepBlacklist : PepActivity(), SearchView.OnQueryTextListener {

    private lateinit var recipientsView: RecyclerView
    private lateinit var container: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var clearSearchIcon: View
    private lateinit var searchLayout: View

    private lateinit var recipientsAdapter: KeyItemAdapter
    private lateinit var recipientsLayoutManager: RecyclerView.LayoutManager

    private lateinit var pEp: PEpProvider
    private var keys: List<KeyListItem>? = null

    @Inject
    lateinit var toolbarCustomizer: ToolBarCustomizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pep_blacklist)
        ButterKnife.bind(this@PepBlacklist)
        initializeViews()
        initializeToolbar(true, R.string.pep)
        toolbarCustomizer.setStatusBarPepColor(ThemeManager.getStatusBarColor(this, ThemeManager.ToolbarType.DEFAULT))
        initializeSearchBar()

        pEp = (application as K9).getpEpProvider()
        recipientsLayoutManager = LinearLayoutManager(this)
        (recipientsLayoutManager as LinearLayoutManager).orientation = LinearLayoutManager.VERTICAL
        recipientsView.layoutManager = recipientsLayoutManager
        recipientsView.visibility = View.VISIBLE

        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            keys = getBlackListInfo()
            initializeKeysView()
        }


    }

    override fun inject() {
        getpEpComponent().inject(this)
    }

    private fun initializeViews() {
        recipientsView = findViewById(R.id.my_recycler_view)
        container = findViewById(R.id.pep_blacklist_layout)
    }

    private fun initializeKeysView() {
        recipientsAdapter = KeyItemAdapter(keys, false, OnKeyClickListener { item, checked -> keyChecked(item, checked) })
        recipientsView.adapter = recipientsAdapter
        recipientsAdapter.notifyDataSetChanged()
    }

    private fun keyChecked(item: KeyListItem, checked: Boolean) {
        val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        uiScope.launch {
            when (checked) {
                true -> addToBlacklist(item.fpr)
                false -> deleteFromBlacklist(item.fpr)
            }
        }
    }

    private fun initializeSearchBar() {
        clearSearchIcon = findViewById(R.id.search_clear)
        searchLayout = findViewById(R.id.toolbar_search_container)
        searchInput = findViewById(R.id.search_input)

        searchInput.doOnTextChanged { query, start, count, after ->
            when {
                query.toString().isEmpty() -> {
                    clearSearchIcon.visibility = View.GONE
                    initializeKeysView()
                }
                else -> {
                    clearSearchIcon.visibility = View.VISIBLE
                    onQueryTextSubmit(searchInput.text.toString())
                }
            }
        }

        searchInput.setOnEditorActionListener { v, actionId, event ->
            if (searchInput.text.isNotEmpty()) {
                onQueryTextSubmit(searchInput.text.toString())
            }
            true
        }
        clearSearchIcon.setOnClickListener {
            searchInput.text = null
            hideSearchView()
            KeyboardUtils.hideKeyboard(searchInput)
            initializeKeysView()
        }
    }

    override fun hideSearchView() {
        toolbar.visibility = View.VISIBLE
        searchLayout.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_pep_search, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        val filteredModelList = filter(keys, query)
        recipientsAdapter.replaceAll(filteredModelList)
        recipientsView.scrollToPosition(0)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_search -> {
                showSearchView()
                true
            }
            R.id.action_add_fpr -> {
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView = inflater.inflate(R.layout.fpr_dialog, null)
                dialogBuilder.setView(dialogView)
                val fpr = dialogView.findViewById<View>(R.id.fpr_text) as EditText
                dialogBuilder.setTitle("Add FPR")
                dialogBuilder.setPositiveButton("Done") { dialog: DialogInterface?, whichButton: Int -> addFingerprintToBlacklist(fpr) }
                dialogBuilder.setNegativeButton("Cancel") { dialog: DialogInterface?, whichButton: Int -> }
                val b = dialogBuilder.create()
                b.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun addFingerprintToBlacklist(fpr: EditText) {
        val fingerprint = fpr.text.toString().toUpperCase().replace(" ".toRegex(), "")
        val pattern = Pattern.compile("^[0-9A-F]+$")
        val matcher = pattern.matcher(fingerprint)
        if (matcher.find() && fingerprint.length >= 40) {
            for (key in keys!!) {
                if (key.fpr == fingerprint) {
                    pEp.addToBlacklist(fingerprint)
                }
            }
            keys = pEp.blacklistInfo
            initializeKeysView()
        } else {
            FeedbackTools.showShortFeedback(container, getString(R.string.error_parsing_fingerprint))
        }
    }

    override fun showSearchView() {
        toolbar.visibility = View.GONE
        searchLayout.visibility = View.VISIBLE
        setFocusOnKeyboard()
    }

    private fun setFocusOnKeyboard() {
        searchInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private suspend fun getBlackListInfo(): List<KeyListItem>? = withContext(Dispatchers.IO) {
        pEp.blacklistInfo
    }

    private suspend fun addToBlacklist(fingerprint: String) = withContext(Dispatchers.IO) {
        pEp.addToBlacklist(fingerprint)
    }

    private suspend fun deleteFromBlacklist(fingerprint: String) = withContext(Dispatchers.IO) {
        pEp.deleteFromBlacklist(fingerprint)
    }

    private fun filter(models: List<KeyListItem>?, query: String): List<KeyListItem> {
        val lowerCaseQuery = query.toLowerCase()
        val filteredModelList: MutableList<KeyListItem> = ArrayList()
        for (model in models!!) {
            val text = model.getGpgUid().toLowerCase()
            if (text.contains(lowerCaseQuery)
                    || model.getFpr().toLowerCase().contains(lowerCaseQuery)) {
                filteredModelList.add(model)
            }
        }
        return filteredModelList
    }

    companion object {
        fun actionShowBlacklist(context: Context) {
            val i = Intent(context, PepBlacklist::class.java)
            context.startActivity(i)
        }


    }
}