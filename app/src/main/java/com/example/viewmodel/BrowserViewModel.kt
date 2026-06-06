package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.downloader.DownloadManagerHelper
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

sealed class DownloadProgressState {
    object Idle : DownloadProgressState()
    data class Loading(
        val itemIndex: Int,
        val totalItems: Int,
        val progressFraction: Float,
        val currentFileName: String,
        val description: String
    ) : DownloadProgressState()
    data class Completed(val message: String, val savedUri: Uri?) : DownloadProgressState()
    data class Error(val errorMessage: String) : DownloadProgressState()
}

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadHelper = DownloadManagerHelper(application)
    private val database = BrowserDatabase.getDatabase(application)
    private val dao = database.browserDao()
    
    private val prefs = application.getSharedPreferences("aurora_browser_prefs", android.content.Context.MODE_PRIVATE)

    // Current settings
    val searchEngine = MutableStateFlow(prefs.getString("search_engine", "Google") ?: "Google")
    val userAgentMode = MutableStateFlow(prefs.getString("user_agent_mode", "Mobile") ?: "Mobile")
    val homePageUrl = MutableStateFlow(prefs.getString("homepage_url", "https://images.google.com") ?: "https://images.google.com")
    val isTrackerBlockingEnabled = MutableStateFlow(prefs.getBoolean("tracker_blocking", true))
    val isClearHistoryOnExitEnabled = MutableStateFlow(prefs.getBoolean("clear_history_on_exit", false))
    val appLanguage = MutableStateFlow(prefs.getString("app_language", "ru") ?: "ru")
    val adBlockMode = MutableStateFlow(prefs.getString("ad_block_mode", "standard") ?: "standard") // "off", "standard", "aggressive"
    val blockedAdsCount = MutableStateFlow(0)
    val isFastRenderingEnabled = MutableStateFlow(prefs.getBoolean("fast_rendering", true))
    val isAutoCookieEnabled = MutableStateFlow(prefs.getBoolean("auto_cookie", true))

    fun toggleFastRendering(enabled: Boolean) {
        isFastRenderingEnabled.value = enabled
        prefs.edit().putBoolean("fast_rendering", enabled).apply()
    }

    fun toggleAutoCookie(enabled: Boolean) {
        isAutoCookieEnabled.value = enabled
        prefs.edit().putBoolean("auto_cookie", enabled).apply()
    }

    fun selectAdBlockMode(mode: String) {
        adBlockMode.value = mode
        prefs.edit().putString("ad_block_mode", mode).apply()
    }

    fun incrementBlockedAdsCount() {
        blockedAdsCount.value = blockedAdsCount.value + 1
    }

    fun resetBlockedAdsCount() {
        blockedAdsCount.value = 0
    }

    fun selectLanguage(langCode: String) {
        appLanguage.value = langCode
        prefs.edit().putString("app_language", langCode).apply()
    }

    fun selectSearchEngine(engine: String) {
        searchEngine.value = engine
        prefs.edit().putString("search_engine", engine).apply()
    }

    fun selectUserAgentMode(mode: String) {
        userAgentMode.value = mode
        prefs.edit().putString("user_agent_mode", mode).apply()
    }

    fun setHomePageUrl(url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        homePageUrl.value = cleanUrl
        prefs.edit().putString("homepage_url", cleanUrl).apply()
    }

    fun toggleTrackerBlocking(enabled: Boolean) {
        isTrackerBlockingEnabled.value = enabled
        prefs.edit().putBoolean("tracker_blocking", enabled).apply()
    }

    fun toggleClearHistoryOnExit(enabled: Boolean) {
        isClearHistoryOnExitEnabled.value = enabled
        prefs.edit().putBoolean("clear_history_on_exit", enabled).apply()
    }

    fun getSearchUrl(query: String): String {
        val encodedQuery = Uri.encode(query)
        return when (searchEngine.value) {
            "Yandex" -> "https://yandex.com/search/?text=$encodedQuery"
            "Bing" -> "https://www.bing.com/search?q=$encodedQuery"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$encodedQuery"
            else -> "https://www.google.com/search?q=$encodedQuery" // Google
        }
    }

    fun getUserAgent(): String {
        return if (userAgentMode.value == "Desktop") {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36"
        } else {
            "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        }
    }

    fun clearBookmarks() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dao.getAllBookmarks().first()
            list.forEach { dao.deleteBookmark(it) }
        }
    }

    fun clearDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dao.getAllDownloads().first()
            list.forEach { dao.deleteDownload(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isClearHistoryOnExitEnabled.value) {
            clearHistory()
        }
    }

    // Dynamic database streams
    val bookmarks: StateFlow<List<BookmarkEntity>> = dao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = dao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabs: StateFlow<List<TabEntity>> = dao.getAllTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = dao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Web Browser State
    private val _currentUrl = MutableStateFlow(prefs.getString("homepage_url", "https://images.google.com") ?: "https://images.google.com")
    val currentUrl: StateFlow<String> = _currentUrl

    private val _webTitle = MutableStateFlow("Google Images")
    val webTitle: StateFlow<String> = _webTitle

    private val _webLoadProgress = MutableStateFlow(0)
    val webLoadProgress: StateFlow<Int> = _webLoadProgress

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId

    // Browser User Agent
    var browserUserAgent: String = ""

    // Discovered Media Elements
    private val _rawMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val rawMediaItems: StateFlow<List<MediaItem>> = _rawMediaItems

    // UI Configuration / Selection state
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

    private val _filterType = MutableStateFlow("All") // "All", "image", "video", "other"
    val filterType: StateFlow<String> = _filterType

    private val _sortOption = MutableStateFlow("None") // "None", "Name", "Type", "Size"
    val sortOption: StateFlow<String> = _sortOption

    private val _mediaSearchQuery = MutableStateFlow("")
    val mediaSearchQuery: StateFlow<String> = _mediaSearchQuery

    // Active downloading progress dialog state
    private val _downloadProgress = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadProgress: StateFlow<DownloadProgressState> = _downloadProgress

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val savedHome = prefs.getString("homepage_url", "https://images.google.com") ?: "https://images.google.com"
            dao.getAllTabs().first().let { currentTabs ->
                if (currentTabs.isEmpty()) {
                    val defaultTabs = listOf(
                        TabEntity("1", "Sway Browser — Главная", savedHome, isIncognito = false, isActive = true),
                        TabEntity("2", "Material 3", "https://m3.material.io", isIncognito = false, isActive = false),
                        TabEntity("3", "GitHub", "https://github.com", isIncognito = false, isActive = false)
                    )
                    defaultTabs.forEach { dao.insertTab(it) }
                    _activeTabId.value = "1"
                    _currentUrl.value = savedHome
                } else {
                    val active = currentTabs.find { it.isActive } ?: currentTabs.first()
                    _activeTabId.value = active.id
                    _currentUrl.value = active.url
                    _webTitle.value = active.title
                }
            }
        }
    }

    // Tab Operations
    fun selectTab(tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTabs = dao.getAllTabs().first()
            val tabToActivate = currentTabs.find { it.id == tabId } ?: return@launch
            
            _activeTabId.value = tabId
            _currentUrl.value = tabToActivate.url
            _webTitle.value = tabToActivate.title
            
            currentTabs.forEach {
                val updated = it.copy(isActive = (it.id == tabId))
                dao.updateTab(updated)
            }
        }
    }

    fun addTab(title: String, url: String, isIncognito: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val tabId = java.util.UUID.randomUUID().toString()
            val newTab = TabEntity(
                id = tabId,
                title = title,
                url = url,
                isIncognito = isIncognito,
                isActive = true
            )
            val currentTabs = dao.getAllTabs().first()
            currentTabs.forEach {
                if (it.isActive) {
                    dao.updateTab(it.copy(isActive = false))
                }
            }
            dao.insertTab(newTab)
            
            _activeTabId.value = tabId
            _currentUrl.value = url
            _webTitle.value = title
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentTabs = dao.getAllTabs().first()
            val tabToClose = currentTabs.find { it.id == tabId } ?: return@launch
            
            dao.deleteTabById(tabId)
            
            if (tabToClose.isActive) {
                val remaining = currentTabs.filter { it.id != tabId }
                if (remaining.isNotEmpty()) {
                    val nextToActivate = remaining.first()
                    dao.updateTab(nextToActivate.copy(isActive = true))
                    _activeTabId.value = nextToActivate.id
                    _currentUrl.value = nextToActivate.url
                    _webTitle.value = nextToActivate.title
                } else {
                    val fallbackId = java.util.UUID.randomUUID().toString()
                    val fallback = TabEntity(
                        id = fallbackId,
                        title = "Sway Browser — Главная",
                        url = "https://images.google.com",
                        isIncognito = false,
                        isActive = true
                    )
                    dao.insertTab(fallback)
                    _activeTabId.value = fallbackId
                    _currentUrl.value = fallback.url
                    _webTitle.value = fallback.title
                }
            }
        }
    }

    // Bookmark Operations
    fun toggleBookmark(title: String, url: String, category: String = "Все") {
        if (url.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.isBookmarked(url)) {
                dao.deleteBookmarkByUrl(url)
            } else {
                val name = if (title.isBlank()) url else title
                dao.insertBookmark(BookmarkEntity(title = name, url = url, category = category))
            }
        }
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteBookmark(bookmark)
        }
    }

    fun isBookmarkedFlow(url: String): Flow<Boolean> {
        return dao.isBookmarkedFlow(url)
    }

    // History Operations
    fun addToHistory(title: String, url: String) {
        if (url.isBlank() || url == "about:blank" || url.startsWith("data:")) return
        viewModelScope.launch(Dispatchers.IO) {
            val name = if (title.isBlank()) url else title
            dao.insertHistory(HistoryEntity(title = name, url = url))
        }
    }

    fun deleteHistoryItem(item: HistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHistory(item)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllHistory()
        }
    }

    // Filtered and Sorted list combined
    val filteredAndSortedMedia: StateFlow<List<MediaItem>> = combine(
        _rawMediaItems,
        _filterType,
        _sortOption,
        _mediaSearchQuery
    ) { items, filter, sort, search ->
        var list = items.asSequence()

        // 1. Filter Type
        if (filter != "All") {
            list = list.filter { it.type.equals(filter, ignoreCase = true) }
        }

        // 2. Search Text
        if (search.isNotBlank()) {
            list = list.filter { it.filename.contains(search, ignoreCase = true) }
        }

        // 3. Sorting
        list = when (sort) {
            "Name" -> list.sortedBy { it.filename }
            "Type" -> list.sortedBy { it.type }
            "Size" -> list.sortedByDescending { it.sizeBytes }
            else -> list
        }

        list.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var sizeQueryJob: Job? = null

    fun updateUrl(url: String) {
        _currentUrl.value = url
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = _activeTabId.value
            if (activeId != null) {
                val tab = dao.getAllTabs().first().find { it.id == activeId }
                if (tab != null && tab.url != url) {
                    dao.updateTab(tab.copy(url = url))
                }
            }
        }
    }

    fun updateWebProgress(progress: Int) {
        _webLoadProgress.value = progress
    }

    fun updateWebNavigation(title: String, canGoBack: Boolean, canGoForward: Boolean) {
        _webTitle.value = title
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward

        if (title.isNotEmpty()) {
            addToHistory(title, _currentUrl.value)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val activeId = _activeTabId.value
            if (activeId != null) {
                val tab = dao.getAllTabs().first().find { it.id == activeId }
                if (tab != null && (tab.title != title || tab.url != _currentUrl.value)) {
                    dao.updateTab(tab.copy(title = title, url = _currentUrl.value))
                }
            }
        }
    }

    fun updateFilter(type: String) {
        _filterType.value = type
    }

    fun updateSort(option: String) {
        _sortOption.value = option
    }

    fun updateSearch(query: String) {
        _mediaSearchQuery.value = query
    }

    fun clearDiscoveredMedia() {
        sizeQueryJob?.cancel()
        _rawMediaItems.value = emptyList()
        _selectedIds.value = emptySet()
    }

    fun toggleSelection(itemId: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _selectedIds.value = current
    }

    fun selectAll() {
        val currentFiltered = filteredAndSortedMedia.value
        val allIds = currentFiltered.map { it.id }.toSet()
        _selectedIds.value = allIds
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    private fun getSimilarityKey(urlStr: String): String {
        try {
            var clean = urlStr.lowercase().trim()
            
            // Handle Pinterest specifically
            if (clean.contains("pinimg.com")) {
                clean = clean.replace(Regex("/(136x136|236x|474x|564x|736x|originals)/"), "/{size}/")
            }
            
            // Remove common sizing / tracking query parameters
            val uri = Uri.parse(clean)
            val builder = uri.buildUpon().clearQuery()
            
            uri.queryParameterNames.forEach { param ->
                if (param != "w" && param != "h" && param != "width" && param != "height" && 
                    param != "size" && param != "resize" && param != "quality" && param != "q" && 
                    param != "fit" && param != "crop" && param != "thumb") {
                    builder.appendQueryParameter(param, uri.getQueryParameter(param))
                }
            }
            
            var base = builder.build().toString()
            
            // Remove common filename size decorators
            base = base.replace(Regex("[-_]\\d+x\\d+(?=\\.[a-z0-9]+)"), "")
            base = base.replace(Regex("[-_](thumb|thumbnail|small|preview|mini)(?=\\.[a-z0-9]+)"), "")
            
            return base
        } catch (e: Exception) {
            return urlStr.lowercase()
        }
    }

    private fun getQualityScore(item: MediaItem): Double {
        var score = 0.0
        
        // Image dimensions
        if (item.width > 0 && item.height > 0) {
            score += (item.width * item.height).toDouble() / 1000.0
        }
        
        // File size bytes weight
        if (item.sizeBytes > 0) {
            score += item.sizeBytes.toDouble() / 1024.0
        }
        
        // Quality indicators in path
        val urlLower = item.url.lowercase()
        if (urlLower.contains("/originals/")) {
            score += 2000000.0
        } else if (urlLower.contains("/736x/")) {
            score += 1000000.0
        } else if (urlLower.contains("/564x/")) {
            score += 500000.0
        } else if (urlLower.contains("/236x/")) {
            score -= 500000.0
        }
        
        if (urlLower.contains("original") || urlLower.contains("highres") || urlLower.contains("large") || urlLower.contains("full")) {
            score += 300000.0
        }
        if (urlLower.contains("thumb") || urlLower.contains("mini") || urlLower.contains("avatar") || urlLower.contains("preview")) {
            score -= 200000.0
        }
        
        try {
            val uri = Uri.parse(item.url)
            val wParam = uri.getQueryParameter("w") ?: uri.getQueryParameter("width")
            val hParam = uri.getQueryParameter("h") ?: uri.getQueryParameter("height")
            if (wParam != null && hParam != null) {
                val w = wParam.toIntOrNull() ?: 0
                val h = hParam.toIntOrNull() ?: 0
                if (w > 0 && h > 0) {
                    score += (w * h).toDouble() / 1000.0
                }
            } else if (wParam != null) {
                val w = wParam.toIntOrNull() ?: 0
                if (w > 0) score += w.toDouble()
            }
        } catch (e: Exception) {}
        
        return score
    }

    private fun deduplicateAndSortMedia(items: List<MediaItem>): List<MediaItem> {
        val grouped = items.groupBy { getSimilarityKey(it.url) }
        return grouped.map { (_, groupList) ->
            groupList.maxByOrNull { getQualityScore(it) }!!
        }
    }

    fun parseAndAddDiscoveredMedia(jsonStr: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val list = mutableListOf<MediaItem>()
            try {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val url = obj.optString("url", "")
                    if (url.isBlank()) continue

                    val type = obj.optString("type", "other")
                    val ext = obj.optString("ext", "bin")
                    val filename = obj.optString("filename", "file_$i")
                    val tagName = obj.optString("tagName", "IMG")
                    val width = obj.optInt("width", -1)
                    val height = obj.optInt("height", -1)

                    // Generate clean md5 identifier
                    val md = MessageDigest.getInstance("MD5")
                    val hash = md.digest(url.toByteArray()).joinToString("") { "%02x".format(it) }

                    list.add(
                        MediaItem(
                            id = hash,
                            url = url,
                            type = type,
                            ext = ext,
                            filename = filename,
                            tagName = tagName,
                            width = width,
                            height = height,
                            sizeBytes = -1L
                        )
                    )

                    // Proactive high-quality upgrades for Pinterest pin images
                    if (url.contains("pinimg.com")) {
                        val highQualityUrl = url.replace(Regex("/(136x136|236x|474x|564x|736x)/"), "/originals/")
                        if (highQualityUrl != url) {
                            val originalMd = MessageDigest.getInstance("MD5")
                            val originalHash = originalMd.digest(highQualityUrl.toByteArray()).joinToString("") { "%02x".format(it) }
                            val originalFilename = if (filename.contains(Regex("[-_](136x136|236x|474x|564x|736x)"))) {
                                filename.replace(Regex("[-_](136x136|236x|474x|564x|736x)"), "_highres")
                            } else {
                                "${filename}_highres"
                            }
                            list.add(
                                MediaItem(
                                    id = originalHash,
                                    url = highQualityUrl,
                                    type = type,
                                    ext = ext,
                                    filename = originalFilename,
                                    tagName = "${tagName}_UPGRADED",
                                    width = -1,
                                    height = -1,
                                    sizeBytes = -1L
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BrowserViewModel", "JSON Parsing failure", e)
            }

            if (list.isEmpty()) return@launch

            val currentItems = _rawMediaItems.value
            val deduplicatedCombined = deduplicateAndSortMedia(currentItems + list)

            if (deduplicatedCombined.size != currentItems.size || 
                deduplicatedCombined.map { it.url }.toSet() != currentItems.map { it.url }.toSet()) {
                _rawMediaItems.value = deduplicatedCombined
                startAsyncSizeQueries()
            }
        }
    }

    private fun startAsyncSizeQueries() {
        sizeQueryJob?.cancel()
        sizeQueryJob = viewModelScope.launch(Dispatchers.IO) {
            // Find all elements with undetermined sizes
            val unmeasured = _rawMediaItems.value.filter { it.sizeBytes == -1L }
            unmeasured.forEach { item ->
                val size = downloadHelper.queryContentSize(item.url, browserUserAgent, _currentUrl.value)
                if (size > 0) {
                    if (item.type == "image" && size < 1800) {
                        // Filter out tiny layout files or tracking pixels
                        val filtered = _rawMediaItems.value.filter { it.id != item.id }
                        _rawMediaItems.value = deduplicateAndSortMedia(filtered)
                    } else {
                        val updated = _rawMediaItems.value.map {
                            if (it.id == item.id) it.copy(sizeBytes = size) else it
                        }
                        _rawMediaItems.value = deduplicateAndSortMedia(updated)
                    }
                }
            }
        }
    }

    // Download Operations
    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteDownload(download)
        }
    }

    /**
     * Download individual items selected in the list
     */
    fun downloadSelectedFilesIndividually() {
        val selected = _rawMediaItems.value.filter { _selectedIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        viewModelScope.launch(Dispatchers.Main) {
            _downloadProgress.value = DownloadProgressState.Loading(
                itemIndex = 0,
                totalItems = selected.size,
                progressFraction = 0.0f,
                currentFileName = "",
                description = "Начало загрузки..."
            )

            var successCount = 0
            selected.forEachIndexed { index, mediaItem ->
                val sizeStr = if (mediaItem.sizeBytes > 0) {
                    val kb = mediaItem.sizeBytes / 1024
                    if (kb > 1024) "%.2f MB".format(kb.toFloat() / 1024f) else "$kb KB"
                } else "Размер неизвестен"

                val downloadEntity = DownloadEntity(
                    id = mediaItem.id,
                    filename = mediaItem.filename,
                    url = mediaItem.url,
                    status = "Скачивание",
                    progress = 0.0f,
                    size = sizeStr,
                    timestamp = System.currentTimeMillis()
                )

                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity)
                }

                _downloadProgress.value = DownloadProgressState.Loading(
                    itemIndex = index,
                    totalItems = selected.size,
                    progressFraction = 0.1f,
                    currentFileName = mediaItem.filename,
                    description = "Файл ${index + 1} из ${selected.size}"
                )

                val resultUri = downloadHelper.downloadFile(mediaItem, browserUserAgent, _currentUrl.value) { currentBytes, totalBytes ->
                    val frac = if (totalBytes > 0) currentBytes.toFloat() / totalBytes else 0.5f
                    _downloadProgress.value = DownloadProgressState.Loading(
                        itemIndex = index,
                        totalItems = selected.size,
                        progressFraction = frac,
                        currentFileName = mediaItem.filename,
                        description = "Файл ${index + 1} из ${selected.size}"
                    )
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertDownload(downloadEntity.copy(progress = frac))
                    }
                }

                if (resultUri != null) {
                    successCount++
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertDownload(downloadEntity.copy(status = "Готово", progress = 1.0f, localUri = resultUri.toString()))
                    }
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        dao.insertDownload(downloadEntity.copy(status = "Ошибка", progress = 0.0f))
                    }
                }
            }

            _downloadProgress.value = DownloadProgressState.Completed(
                message = "Успешно сохранено $successCount из ${selected.size} файлов в Галерею / Загрузки.",
                savedUri = null
            )
        }
    }

    /**
     * Download a single media item and log it to database
     */
    fun downloadSingleMediaFile(mediaItem: MediaItem) {
        viewModelScope.launch(Dispatchers.Main) {
            val sizeStr = if (mediaItem.sizeBytes > 0) {
                val kb = mediaItem.sizeBytes / 1024
                if (kb > 1024) "%.2f MB".format(kb.toFloat() / 1024f) else "$kb KB"
            } else "Размер неизвестен"

            val downloadEntity = DownloadEntity(
                id = mediaItem.id,
                filename = mediaItem.filename,
                url = mediaItem.url,
                status = "Скачивание",
                progress = 0.0f,
                size = sizeStr,
                timestamp = System.currentTimeMillis()
            )

            viewModelScope.launch(Dispatchers.IO) {
                dao.insertDownload(downloadEntity)
            }

            _downloadProgress.value = DownloadProgressState.Loading(
                itemIndex = 0,
                totalItems = 1,
                progressFraction = 0.1f,
                currentFileName = mediaItem.filename,
                description = "Скачивание..."
            )

            val resultUri = downloadHelper.downloadFile(mediaItem, browserUserAgent, _currentUrl.value) { currentBytes, totalBytes ->
                val frac = if (totalBytes > 0) currentBytes.toFloat() / totalBytes else 0.5f
                _downloadProgress.value = DownloadProgressState.Loading(
                    itemIndex = 0,
                    totalItems = 1,
                    progressFraction = frac,
                    currentFileName = mediaItem.filename,
                    description = "Скачивание..."
                )
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(progress = frac))
                }
            }

            if (resultUri != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(status = "Готово", progress = 1.0f, localUri = resultUri.toString()))
                }
                _downloadProgress.value = DownloadProgressState.Completed(
                    message = "Файл '${mediaItem.filename}' сохранен в Галерею.",
                    savedUri = resultUri
                )
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(status = "Ошибка", progress = 0.0f))
                }
                _downloadProgress.value = DownloadProgressState.Error("Не удалось скачать файл.")
            }
        }
    }

    /**
     * Compression-assisted batch ZIP download
     */
    fun downloadSelectedAsZip(zipNameInput: String = "") {
        val selected = _rawMediaItems.value.filter { _selectedIds.value.contains(it.id) }
        if (selected.isEmpty()) return

        val targetName = zipNameInput.trim().let {
            if (it.isEmpty()) "media_bundle_${System.currentTimeMillis()}.zip"
            else if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip"
        }

        viewModelScope.launch(Dispatchers.Main) {
            val downloadEntity = DownloadEntity(
                id = targetName.hashCode().toString(),
                filename = targetName,
                url = "Local Zip Bundle",
                status = "Скачивание",
                progress = 0.0f,
                size = "${selected.size} файлов",
                timestamp = System.currentTimeMillis()
            )

            viewModelScope.launch(Dispatchers.IO) {
                dao.insertDownload(downloadEntity)
            }

            _downloadProgress.value = DownloadProgressState.Loading(
                itemIndex = 0,
                totalItems = selected.size,
                progressFraction = 0.0f,
                currentFileName = "",
                description = "Создание архива ZIP..."
            )

            val apiUri = downloadHelper.downloadZip(selected, targetName, browserUserAgent, _currentUrl.value) { index, total, frac, name ->
                _downloadProgress.value = DownloadProgressState.Loading(
                    itemIndex = index,
                    totalItems = total,
                    progressFraction = frac,
                    currentFileName = name,
                    description = "Упаковка файла ${index + 1} из $total"
                )
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(progress = (index.toFloat() / total)))
                }
            }

            if (apiUri != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(status = "Готово", progress = 1.0f, localUri = apiUri.toString()))
                }
                _downloadProgress.value = DownloadProgressState.Completed(
                    message = "ZIP архив '$targetName' сохранен в Downloads/MediaDownloader",
                    savedUri = apiUri
                )
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    dao.insertDownload(downloadEntity.copy(status = "Ошибка", progress = 0.0f))
                }
                _downloadProgress.value = DownloadProgressState.Error("Не удалось создать ZIP архив.")
            }
        }
    }

    /**
     * Download everything as single collective ZIP
     */
    fun downloadAllAsZip() {
        val allItems = filteredAndSortedMedia.value
        if (allItems.isEmpty()) return

        // Select all items
        _selectedIds.value = allItems.map { it.id }.toSet()
        downloadSelectedAsZip("all_media_archive.zip")
    }

    fun dismissProgress() {
        _downloadProgress.value = DownloadProgressState.Idle
    }

    suspend fun downloadFileDirectly(mediaItem: MediaItem): Uri? {
        return downloadHelper.downloadFile(mediaItem, browserUserAgent, _currentUrl.value) { _, _ -> }
    }
}
