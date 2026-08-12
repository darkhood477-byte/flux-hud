package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

object ActionType {
    const val EMPTY = "EMPTY"
    const val SHORTCUT = "SHORTCUT"
    const val APP = "APP"
    const val DEEP_LINK = "DEEP_LINK"
    const val URL = "URL"
    const val BROADCAST = "BROADCAST"
    const val UTILITY = "UTILITY"
}

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey val id: Int, // Represents the slice index (0, 1, 2, 3)
    val type: String, // "APP", "DEEP_LINK", "URL", "BROADCAST", "UTILITY", "EMPTY"
    val target: String // Package name, URL, intent URI, or broadcast action
)

@Entity(tableName = "clipboard")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
