package com.xdust.auryxbrowser.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for browsing history entries.
 */
@Dao
interface HistoryDao {

    /**
     * Returns a flow emitting all history entries ordered from most recent to oldest.
     */
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryEntry>>

    /**
     * Inserts a history entry. If the URL already exists this will create a
     * duplicate entry so that the visit order remains preserved.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: HistoryEntry)

    /**
     * Returns a list of history entries matching the query. The search is
     * case‑insensitive and matches against both title and URL.
     */
    @Query(
        "SELECT * FROM history WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 15"
    )
    suspend fun searchEntries(query: String): List<HistoryEntry>
}