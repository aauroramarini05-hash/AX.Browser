package com.xdust.auryxbrowser

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.xdust.auryxbrowser.databinding.ActivityMainBinding
import com.xdust.auryxbrowser.ui.TabAdapter
import com.xdust.auryxbrowser.ui.WebViewFragment
import com.xdust.auryxbrowser.util.AdBlocker
import com.xdust.auryxbrowser.viewmodel.BrowserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The main activity hosts the tabbed browser UI. It manages a ViewPager2 of
 * [WebViewFragment] instances, an address bar with real‑time suggestions and
 * various toolbar buttons for navigation, tab management and incognito mode.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabAdapter: TabAdapter
    private lateinit var viewModel: BrowserViewModel
    private var incognitoMode: Boolean = false

    // Launcher for requesting runtime permissions required for downloads.
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Trigger any pending download once permissions have been granted.
        pendingDownloadRequest?.let { (url, userAgent, contentDisposition, mimetype) ->
            if (permissions.values.all { it }) {
                startDownload(url, userAgent, contentDisposition, mimetype)
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
            pendingDownloadRequest = null
        }
    }

    // Temporary store for a download request while waiting for permission.
    private var pendingDownloadRequest: Quadruple<String, String, String, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the ad blocker with the application context.
        AdBlocker.init(applicationContext)

        // Obtain the ViewModel.
        viewModel = ViewModelProvider(this)[BrowserViewModel::class.java]

        // Set up the tab adapter and ViewPager.
        tabAdapter = TabAdapter(this)
        binding.viewPager.adapter = tabAdapter

        // Configure TabLayout with ViewPager using a mediator that binds the tab titles and close buttons.
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createTabView(tab, position)
        }.attach()

        // Listen for page selection to update the UI accordingly.
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateAddressBarForCurrentTab()
                updateBookmarkIcon()
            }
        })

        setupAddressBar()
        setupToolbarButtons()
        // Reflect initial incognito mode state in the UI.
        updateIncognitoIcon()

        // Always start with a single tab pointing at a default homepage.
        addNewTab("https://www.google.com")
    }

    /**
     * Builds a custom view for each tab consisting of the page title and a
     * close button. The view's click listeners are set up within this method.
     */
    private fun createTabView(tab: TabLayout.Tab, position: Int): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_tab, null)
        val titleView = view.findViewById<TextView>(R.id.tabTitle)
        val closeButton = view.findViewById<ImageButton>(R.id.btnCloseTab)

        // Set tab title to the current fragment's title or URL.
        val frag = tabAdapter.getFragmentAt(position)
        if (frag is WebViewFragment) {
            val title = frag.webView.title
            titleView.text = if (!title.isNullOrBlank()) title else frag.webView.url ?: getString(R.string.new_tab)
        }

        // Handle tab selection when the title is clicked.
        view.setOnClickListener {
            binding.viewPager.currentItem = position
        }
        // Handle tab closing.
        closeButton.setOnClickListener {
            removeTab(position)
        }
        return view
    }

    private fun setupAddressBar() {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        binding.addressBar.setAdapter(adapter)

        // When the user selects a suggestion, load the URL directly.
        binding.addressBar.setOnItemClickListener { _, _, pos, _ ->
            val url = adapter.getItem(pos)
            url?.let { loadUrlInCurrentTab(it) }
        }

        // Listen for text changes to request new suggestions.
        binding.addressBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: return
                lifecycleScope.launch(Dispatchers.IO) {
                    val suggestions = viewModel.searchSuggestions(query)
                    runOnUiThread {
                        adapter.clear()
                        adapter.addAll(suggestions)
                        adapter.notifyDataSetChanged()
                        if (!binding.addressBar.isPerformingCompletion) {
                            binding.addressBar.showDropDown()
                        }
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle the IME action to load the entered URL.
        binding.addressBar.setOnEditorActionListener { v, _, _ ->
            val url = v.text.toString().trim()
            loadUrlInCurrentTab(url)
            true
        }
    }

    private fun setupToolbarButtons() {
        binding.btnBack.setOnClickListener {
            currentFragment()?.goBack()
        }
        binding.btnForward.setOnClickListener {
            currentFragment()?.goForward()
        }
        binding.btnRefresh.setOnClickListener {
            currentFragment()?.reload()
        }
        binding.btnNewTab.setOnClickListener {
            addNewTab(null)
        }
        binding.btnIncognito.setOnClickListener {
            // Toggle incognito mode for subsequent tabs. This does not change the
            // current tab mode.
            incognitoMode = !incognitoMode
            updateIncognitoIcon()
        }
        binding.btnBookmark.setOnClickListener {
            val fragment = currentFragment() ?: return@setOnClickListener
            val url = fragment.webView.url ?: return@setOnClickListener
            val title = fragment.webView.title
            viewModel.toggleBookmark(url, title)
            updateBookmarkIcon()
        }
    }

    /**
     * Adds a new tab to the ViewPager and navigates to the specified URL. If
     * [url] is null or blank the tab will load a blank page.
     */
    fun addNewTab(url: String? = null) {
        val fragment = WebViewFragment.newInstance(url, incognitoMode)
        tabAdapter.addTab(fragment)
        // Navigate to the newly created tab.
        binding.viewPager.setCurrentItem(tabAdapter.itemCount - 1, true)
    }

    /**
     * Removes the tab at the specified position. If it was the last remaining
     * tab, a new blank tab will be added to keep the browser functional.
     */
    fun removeTab(position: Int) {
        if (tabAdapter.itemCount <= 1) {
            // Always ensure at least one tab exists.
            tabAdapter.removeTab(0)
            addNewTab("https://www.google.com")
            return
        }
        tabAdapter.removeTab(position)
        // Adjust the ViewPager position if needed.
        val newPosition = position.coerceAtMost(tabAdapter.itemCount - 1)
        binding.viewPager.setCurrentItem(newPosition, true)
    }

    /**
     * Loads the provided URL in the currently selected tab.
     */
    fun loadUrlInCurrentTab(url: String?) {
        val fragment = currentFragment() ?: return
        if (url.isNullOrBlank()) return
        fragment.loadUrl(url)
        binding.addressBar.setText(url, false)
        binding.addressBar.setSelection(url.length)
    }

    /**
     * Called by [WebViewFragment] when a page has finished loading. Updates
     * history (when not incognito) and synchronises the address bar and
     * bookmark icon.
     */
    fun onPageFinished(fragment: WebViewFragment, url: String) {
        // Save history only for normal mode.
        if (!fragment.arguments?.getBoolean("arg_incognito", false)!!) {
            viewModel.addHistory(url, fragment.webView.title)
        }
        // Update address bar to show the loaded URL.
        binding.addressBar.setText(url, false)
        binding.addressBar.setSelection(url.length)
        // Update bookmark icon state asynchronously.
        lifecycleScope.launch(Dispatchers.IO) {
            val bookmarked = viewModel.isBookmarked(url)
            runOnUiThread { updateBookmarkIcon(bookmarked) }
        }
        // Update tab title in TabLayout.
        // Rebind the tab title. Because TabAdapter does not expose its internal
        // fragment list we search for the fragment's index manually.
        val index = (0 until tabAdapter.itemCount).firstOrNull {
            tabAdapter.getFragmentAt(it) === fragment
        } ?: -1
        if (index >= 0) {
            val tab = binding.tabLayout.getTabAt(index)
            if (tab != null) {
                tab.customView = createTabView(tab, index)
            }
        }
    }

    /**
     * Returns the currently visible [WebViewFragment], or null if none exists.
     */
    private fun currentFragment(): WebViewFragment? {
        val frag = tabAdapter.getFragmentAt(binding.viewPager.currentItem)
        return frag as? WebViewFragment
    }

    /**
     * Updates the address bar contents to reflect the current tab's URL.
     */
    private fun updateAddressBarForCurrentTab() {
        val frag = currentFragment() ?: return
        val url = frag.webView.url ?: ""
        binding.addressBar.setText(url, false)
        binding.addressBar.setSelection(url.length)
    }

    /**
     * Updates the bookmark button's icon based on whether the current page is
     * bookmarked. If [bookmarked] is provided explicitly then no database
     * lookup will be performed.
     */
    private fun updateBookmarkIcon(bookmarked: Boolean? = null) {
        if (bookmarked == null) {
            val url = currentFragment()?.webView?.url ?: return
            lifecycleScope.launch(Dispatchers.IO) {
                val isBookmarked = viewModel.isBookmarked(url)
                runOnUiThread { updateBookmarkIcon(isBookmarked) }
            }
            return
        }
        val drawableRes = if (bookmarked) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        binding.btnBookmark.setImageResource(drawableRes)
    }

    /**
     * Updates the incognito icon to reflect the current incognito mode state.
     */
    private fun updateIncognitoIcon() {
        // Apply a tinted alpha to indicate active incognito mode.
        binding.btnIncognito.alpha = if (incognitoMode) 1.0f else 0.5f
    }

    /**
     * Handles download events originating from WebView. If the necessary
     * permissions have not yet been granted, a request will be triggered.
     */
    fun onDownloadRequested(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String
    ) {
        // Check if we already have storage permission for external downloads.
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissionsNeeded.isNotEmpty()) {
            // Save download request and ask for permission.
            pendingDownloadRequest = Quadruple(url, userAgent, contentDisposition, mimetype)
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
            return
        }
        startDownload(url, userAgent, contentDisposition, mimetype)
    }

    /**
     * Performs the actual download using the system [DownloadManager].
     */
    private fun startDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimetype: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            request.addRequestHeader("User-Agent", userAgent)
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype)
            request.setDescription(fileName)
            request.setTitle(fileName)
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Simple container for passing download arguments around when awaiting
     * runtime permissions.
     */
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}