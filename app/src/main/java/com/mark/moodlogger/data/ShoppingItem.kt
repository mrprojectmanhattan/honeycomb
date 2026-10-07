package com.mark.moodlogger.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One thing on the shopping list. [addedAt] is epoch millis when it went on the
 * list; [purchasedAt] is epoch millis when it was checked off, or null while it's
 * still needed. Age (now - addedAt) drives whether the daily reminder escalates.
 */
@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    @ColumnInfo(defaultValue = "")
    val note: String = "",
    val addedAt: Long = 0L,
    val purchasedAt: Long? = null,
) {
    val outstanding: Boolean get() = purchasedAt == null

    /** Whole days this item has been on the list, as of [now]. */
    fun ageDays(now: Long): Int = ((now - addedAt) / DAY_MS).toInt().coerceAtLeast(0)

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
