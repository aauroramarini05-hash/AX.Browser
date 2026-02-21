package com.xdust.auryxbrowser.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a single history entry. The [timestamp] is used to sort
 * history entries with the most recent visits appearing first.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String?,
    val timestamp: Long
)