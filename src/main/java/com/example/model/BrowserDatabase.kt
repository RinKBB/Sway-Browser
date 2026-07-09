package com.example.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val category: String = "Все",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean = false,
    val isActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val historyStackJson: String = "[]",
    val historyIndex: Int = -1,
    val isBrowsing: Boolean = false
)

data class TabHistoryItem(val url: String, val title: String)

fun TabEntity.getHistoryList(): List<TabHistoryItem> {
    if (historyStackJson.isBlank() || historyStackJson == "[]") return emptyList()
    return try {
        val array = org.json.JSONArray(historyStackJson)
        val list = mutableListOf<TabHistoryItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(TabHistoryItem(obj.optString("url", ""), obj.optString("title", "")))
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun TabEntity.withHistoryList(list: List<TabHistoryItem>, index: Int): TabEntity {
    val array = org.json.JSONArray()
    list.forEach {
        val obj = org.json.JSONObject()
        obj.put("url", it.url)
        obj.put("title", it.title)
        array.put(obj)
    }
    return this.copy(historyStackJson = array.toString(), historyIndex = index)
}

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val filename: String,
    val url: String,
    val status: String, // "Готово", "Скачивание", "Ошибка"
    val progress: Float,
    val size: String,
    val localUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BrowserDao {
    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    fun isBookmarkedFlow(url: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url LIMIT 1)")
    suspend fun isBookmarked(url: String): Boolean

    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    // Tabs
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAllTabs()

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, TabEntity::class, DownloadEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: android.content.Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "aurora_browser_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
