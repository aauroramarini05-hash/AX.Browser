package com.xdust.auryxbrowser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.xdust.auryxbrowser.MainActivity
import com.xdust.auryxbrowser.util.AdBlocker
import java.io.ByteArrayInputStream

/**
 * A [Fragment] hosting a single [WebView]. Each instance may be in normal or
 * incognito mode. The WebView is created programmatically to ensure that its
 * context is scoped to the application rather than the activity (required for
 * incognito mode to function properly).
 */
class WebViewFragment : Fragment() {

    private var initialUrl: String? = null
    private var incognito: Boolean = false
    lateinit var webView: WebView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            initialUrl = bundle.getString(ARG_URL)
            incognito = bundle.getBoolean(ARG_INCOGNITO, false)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Create a new WebView using the application context to avoid leaking the activity.
        val context = requireContext().applicationContext
        webView = WebView(context)
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure WebView settings.
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = !incognito
        settings.setSupportZoom(true)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(false)
        settings.saveFormData = !incognito

        if (incognito) {
            // Disable cookies and clear any existing cookies.
            CookieManager.getInstance().setAcceptCookie(false)
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            webView.clearCache(true)
            webView.clearHistory()
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
        }

        // Set up WebView client to handle navigation, history and ad blocking.
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                // Enforce HTTPS when the scheme is HTTP; attempt to redirect to HTTPS for the same host.
                if (url.startsWith("http://")) {
                    val httpsUrl = url.replaceFirst("http://", "https://")
                    view?.loadUrl(httpsUrl)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val urlStr = request.url.toString()
                // Basic ad blocking based on host patterns.
                return if (AdBlocker.isAdUrl(urlStr)) {
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                } else {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Notify the activity that the page has finished loading.
                val activity = activity
                if (activity is MainActivity && url != null) {
                    activity.onPageFinished(this@WebViewFragment, url)
                }
            }
        }

        // Download listener to hook into the download manager.
        webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val hostActivity = activity
            if (hostActivity is MainActivity) {
                hostActivity.onDownloadRequested(url, userAgent, contentDisposition, mimetype)
            }
        })

        // Load the initial URL if provided.
        initialUrl?.let { url ->
            if (url.isNotBlank()) {
                webView.loadUrl(url)
            }
        }
    }

    /**
     * Navigates backward in the history stack if possible.
     */
    fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    /**
     * Navigates forward in the history stack if possible.
     */
    fun goForward() {
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    /**
     * Reloads the current page.
     */
    fun reload() {
        webView.reload()
    }

    /**
     * Loads the specified URL, applying a default https:// prefix if the scheme
     * is missing. The URL must not be null.
     */
    fun loadUrl(url: String) {
        var toLoad = url.trim()
        if (!toLoad.startsWith("http://") && !toLoad.startsWith("https://")) {
            toLoad = "https://$toLoad"
        }
        webView.loadUrl(toLoad)
    }

    companion object {
        private const val ARG_URL = "arg_url"
        private const val ARG_INCOGNITO = "arg_incognito"

        /**
         * Creates a new instance of [WebViewFragment] with the specified URL
         * loaded initially and incognito flag.
         */
        fun newInstance(url: String?, incognito: Boolean): WebViewFragment {
            val fragment = WebViewFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_URL, url)
                putBoolean(ARG_INCOGNITO, incognito)
            }
            return fragment
        }
    }
}