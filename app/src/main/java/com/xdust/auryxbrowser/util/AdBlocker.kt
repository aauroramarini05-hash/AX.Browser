package com.xdust.auryxbrowser.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Simple host‑based ad blocker. A list of blocked hostnames is loaded from
 * `res/raw/adblock_list.txt`. Requests whose host ends with any of these
 * patterns will be blocked by returning an empty WebResourceResponse.
 */
object AdBlocker {

    private val adHosts: MutableSet<String> = mutableSetOf()
    private var initialised = false

    /**
     * Loads the block list from the bundled raw resource. Should be called once
     * at application startup.
     */
    fun init(context: Context) {
        if (initialised) return
        initialised = true
        try {
            val resId = context.resources.getIdentifier("adblock_list", "raw", context.packageName)
            if (resId != 0) {
                context.resources.openRawResource(resId).use { input ->
                    BufferedReader(InputStreamReader(input)).useLines { lines ->
                        lines.filter { it.isNotBlank() }.forEach { line ->
                            adHosts.add(line.trim().lowercase(Locale.US))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore exceptions; if list cannot be loaded then no hosts will be blocked.
        }
    }

    /**
     * Returns true if the given URL should be blocked.
     */
    fun isAdUrl(url: String): Boolean {
        val host = try {
            Uri.parse(url).host?.lowercase(Locale.US)
        } catch (e: Exception) {
            null
        } ?: return false
        // Check if host contains typical ad subdomains
        for (pattern in adHosts) {
            if (host == pattern || host.endsWith(".$pattern")) {
                return true
            }
        }
        return false
    }
}