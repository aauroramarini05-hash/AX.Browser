package com.xdust.auryxbrowser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.xdust.auryxbrowser.data.AppDatabase
import com.xdust.auryxbrowser.data.Bookmark
import com.xdust.auryxbrowser.data.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel which mediates between the UI and the underlying Room database. It
 * exposes flows as LiveData for bookmarks and history and provides helper
 * methods for common operations such as adding history entries, toggling
 * bookmarks and generating search suggestions.
 */
class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val historyDao = db.historyDao()
    private val bookmarkDao = db.bookmarkDao()

    /**
     * Live view of all history entries ordered by timestamp descending.
     */
    val history: LiveData<List<HistoryEntry>> = historyDao.getAll().asLiveData()

    /**
     * Live view of all bookmarks ordered alphabetically by title.
     */
    val bookmarks: LiveData<List<Bookmark>> = bookmarkDao.getAll().asLiveData()

    /**
     * Inserts a new history entry asynchronously on the IO dispatcher.
     */
    fun addHistory(url: String, title: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = HistoryEntry(url = url, title = title, timestamp = System.currentTimeMillis())
            historyDao.insert(entry)
        }
    }

    /**
     * Adds the page at [url] to bookmarks if not present, otherwise removes it.
     */
    fun toggleBookmark(url: String, title: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = bookmarkDao.findByUrl(url)
            if (existing == null) {
                bookmarkDao.insert(Bookmark(url = url, title = title ?: url))
            } else {
                bookmarkDao.delete(existing)
            }
        }
    }

    /**
     * Returns true if the URL is currently bookmarked.
     */
    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.findByUrl(url) != null
    }

    /**
     * Returns a list of suggestion strings derived from bookmarks and history
     * matching the given query. Suggestions are unique and limited to 15 items.
     */
    suspend fun searchSuggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val results = LinkedHashSet<String>()
        // Search bookmarks first so they appear before history in suggestions.
        bookmarkDao.searchEntries(query).forEach { results.add(it.url) }
        historyDao.searchEntries(query).forEach { results.add(it.url) }
        return results.take(15)
    }
}