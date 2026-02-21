package com.xdust.auryxbrowser.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a bookmark. Bookmarks persist across application restarts.
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String
)