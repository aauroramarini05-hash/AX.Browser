package com.xdust.auryxbrowser.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for bookmarks stored in the local database.
 */
@Dao
interface BookmarkDao {

    /**
     * Returns all bookmarks ordered alphabetically by title.
     */
    @Query("SELECT * FROM bookmarks ORDER BY title COLLATE NOCASE ASC")
    fun getAll(): Flow<List<Bookmark>>

    /**
     * Inserts a new bookmark. If another bookmark with the same URL exists it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark)

    /**
     * Deletes a bookmark.
     */
    @Delete
    suspend fun delete(bookmark: Bookmark)

    /**
     * Finds a bookmark by its URL.
     */
    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): Bookmark?

    /**
     * Searches bookmarks for titles or URLs containing the query. This is used to
     * provide search suggestions.
     */
    @Query(
        "SELECT * FROM bookmarks WHERE url LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC LIMIT 15"
    )
    suspend fun searchEntries(query: String): List<Bookmark>
}