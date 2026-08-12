package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val shortcutDao: ShortcutDao,
    private val clipboardDao: ClipboardDao
) {
    val shortcuts: Flow<List<ShortcutEntity>> = shortcutDao.getAllShortcuts()
    val clipboardHistory: Flow<List<ClipboardEntity>> = clipboardDao.getClipboardHistory()

    suspend fun saveShortcut(id: Int, type: String, target: String) {
        shortcutDao.insertShortcut(ShortcutEntity(id, type, target))
    }

    suspend fun getShortcut(id: Int): ShortcutEntity? {
        return shortcutDao.getShortcut(id)
    }

    suspend fun addClipboardItem(text: String) {
        clipboardDao.insertClipboardItem(ClipboardEntity(text = text))
    }
}
