package com.zerodev.smsforwarder.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing installed app data with efficient loading and caching.
 * Provides search functionality, pagination, and MRU (Most Recently Used) caching.
 */
@OptIn(FlowPreview::class)
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PAGE_SIZE = 20
        private const val MRU_CACHE_SIZE = 20
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    private val packageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    
    // Comprehensive app cache
    private var _allAppsCache: List<AppInfo>? = null
    private var _cacheTimestamp: Long = 0
    private val cacheLock = kotlinx.coroutines.sync.Mutex()
    
    // MRU cache for recently used apps
    private val _mruApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val mruApps: StateFlow<List<AppInfo>> = _mruApps.asStateFlow()
    
    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Top suggestions based on search query
    val searchSuggestions: Flow<List<AppInfo>> = _searchQuery
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .map { query ->
            if (query.isBlank()) {
                _mruApps.value
            } else {
                getTopSearchSuggestions(query, 5)
            }
        }
    
    /**
     * Initialize the repository by loading MRU apps and preloading cache.
     */
    suspend fun initialize() {
        loadMruApps()
        // Preload cache in background for faster search
        CoroutineScope(Dispatchers.IO).launch {
            getAllAppsCached()
        }
    }
    
    /**
     * Update the search query.
     * 
     * @param query New search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Get a paged flow of installed apps.
     * 
     * @param query Search query to filter apps
     * @return Flow of PagingData containing AppInfo objects
     */
    fun getPagedApps(query: String = ""): Flow<PagingData<AppInfo>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { AppPagingSource(packageManager, query) }
        ).flow
    }
    
    /**
     * Add an app to the MRU cache.
     * 
     * @param appInfo App to add to MRU cache
     */
    fun addToMru(appInfo: AppInfo) {
        val currentMru = _mruApps.value.toMutableList()
        
        // Remove if already exists
        currentMru.removeAll { it.packageName == appInfo.packageName }
        
        // Add to front
        currentMru.add(0, appInfo)
        
        // Limit size
        if (currentMru.size > MRU_CACHE_SIZE) {
            currentMru.removeAt(currentMru.size - 1)
        }
        
        _mruApps.value = currentMru
    }
    
    /**
     * Get app info by package name.
     * 
     * @param packageName Package name to look up
     * @return AppInfo if found, null otherwise
     */
    suspend fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            
            AppInfo(
                packageName = packageName,
                appName = appName,
                icon = icon,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    /**
     * Load MRU apps using UsageStatsManager for recently used apps.
     */
    private suspend fun loadMruApps() {
        val mruList = mutableListOf<AppInfo>()
        
        try {
            // Get usage stats for the last 7 days
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
            
            val usageStats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            
            // Sort by last time used and take top apps
            val recentlyUsedPackages = usageStats
                ?.filter { it.lastTimeUsed > 0 && it.totalTimeInForeground > 0 }
                ?.sortedByDescending { it.lastTimeUsed }
                ?.take(MRU_CACHE_SIZE)
                ?.map { it.packageName }
                ?: emptyList()
            
            // Convert to AppInfo objects
            for (packageName in recentlyUsedPackages) {
                getAppInfo(packageName)?.let { appInfo ->
                    if (!appInfo.isSystemApp) { // Prefer user apps for MRU
                        mruList.add(appInfo)
                    }
                }
            }
            
        } catch (e: Exception) {
            // Fall back to common apps if usage stats unavailable
            val commonPackages = listOf(
                "com.whatsapp",
                "com.facebook.orca", // Facebook Messenger
                "com.google.android.gm", // Gmail
                "org.telegram.messenger", // Telegram
                "com.instagram.android", // Instagram
                "com.facebook.katana", // Facebook
                "com.twitter.android", // Twitter/X
                "com.google.android.apps.messaging", // Google Messages
                "com.slack", // Slack
                "com.discord", // Discord
                "com.skype.raider", // Skype
                "com.viber.voip", // Viber
                "com.google.android.talk", // Google Chat
                "com.microsoft.teams", // Microsoft Teams
                "com.zhiliaoapp.musically", // TikTok
                "com.snapchat.android", // Snapchat
                "com.linkedin.android", // LinkedIn
                "com.pinterest", // Pinterest
                "com.reddit.frontpage" // Reddit
            )
            
            for (packageName in commonPackages) {
                getAppInfo(packageName)?.let { mruList.add(it) }
            }
        }
        
        _mruApps.value = mruList
    }
    
    /**
     * Get cached all apps with expiry handling.
     */
    private suspend fun getAllAppsCached(): List<AppInfo> {
        cacheLock.lock()
        try {
            val currentTime = System.currentTimeMillis()
            
            // Check if cache is valid
            if (_allAppsCache != null && (currentTime - _cacheTimestamp) < CACHE_EXPIRY_MS) {
                return _allAppsCache!!
            }
            
            // Load all apps from package manager with multiple methods for Android 11+ compatibility
            val allApps = mutableListOf<AppInfo>()
            
            try {
                // Method 1: Try to get all apps (works on Android < 11 or with QUERY_ALL_PACKAGES)
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                installedApps.forEach { appInfo ->
                    try {
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(appInfo)
                        
                        allApps.add(AppInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            icon = icon,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        ))
                    } catch (e: Exception) {
                        // Skip apps we can't access
                    }
                }
            } catch (e: Exception) {
                // Fallback for Android 11+ when we can't see all apps
            }
            
            // Method 2: Add apps from launcher intents (visible apps)
            try {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val launcherApps = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                
                launcherApps.forEach { resolveInfo ->
                    val packageName = resolveInfo.activityInfo.packageName
                    if (allApps.none { it.packageName == packageName }) {
                        try {
                            val appInfo = packageManager.getApplicationInfo(packageName, 0)
                            val appName = packageManager.getApplicationLabel(appInfo).toString()
                            val icon = packageManager.getApplicationIcon(appInfo)
                            
                            allApps.add(AppInfo(
                                packageName = packageName,
                                appName = appName,
                                icon = icon,
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            ))
                        } catch (e: Exception) {
                            // Skip if can't access
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue with what we have
            }
            
            // Method 3: Add known popular apps that we've declared in manifest
            val popularApps = listOf(
                "com.whatsapp" to "WhatsApp",
                "com.facebook.orca" to "Messenger",
                "org.telegram.messenger" to "Telegram",
                "com.instagram.android" to "Instagram",
                "com.facebook.katana" to "Facebook",
                "com.twitter.android" to "X (Twitter)",
                "com.google.android.apps.messaging" to "Messages",
                "com.slack" to "Slack",
                "com.discord" to "Discord",
                "com.skype.raider" to "Skype",
                "com.viber.voip" to "Viber",
                "com.google.android.talk" to "Google Chat",
                "com.microsoft.teams" to "Microsoft Teams",
                "com.zhiliaoapp.musically" to "TikTok",
                "com.snapchat.android" to "Snapchat",
                "com.linkedin.android" to "LinkedIn",
                "com.pinterest" to "Pinterest",
                "com.reddit.frontpage" to "Reddit",
                "com.google.android.gm" to "Gmail",
                "com.spotify.music" to "Spotify",
                "com.netflix.mediaclient" to "Netflix",
                "com.amazon.mShop.android.shopping" to "Amazon Shopping",
                "com.paypal.android.p2pmobile" to "PayPal",
                "com.ubercab" to "Uber",
                "com.airbnb.android" to "Airbnb"
            )
            
            popularApps.forEach { (packageName, fallbackName) ->
                if (allApps.none { it.packageName == packageName }) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(appInfo)
                        
                        allApps.add(AppInfo(
                            packageName = packageName,
                            appName = appName,
                            icon = icon,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        ))
                    } catch (e: Exception) {
                        // App not installed, skip
                    }
                }
            }
            
            val sortedApps = allApps.sortedBy { it.appName.lowercase() }
            
            // Update cache
            _allAppsCache = sortedApps
            _cacheTimestamp = currentTime
            
            return sortedApps
        } finally {
            cacheLock.unlock()
        }
    }
    
    /**
     * Fast search through cached apps with comprehensive matching.
     * 
     * @param query Search query
     * @param limit Maximum number of suggestions
     * @return List of AppInfo matching the query
     */
    suspend fun getSearchSuggestions(query: String, limit: Int = 15): List<AppInfo> {
        if (query.isBlank()) return _mruApps.value.take(limit)
        
        val lowerQuery = query.lowercase().trim()
        val allApps = getAllAppsCached()
        
        // Debug logging for troubleshooting
        android.util.Log.d("AppRepository", "Search query: '$query', Total apps available: ${allApps.size}")
        
        val results = mutableListOf<AppInfo>()
        val seen = mutableSetOf<String>()
        
        // Priority 1: Exact app name matches (case insensitive)
        allApps.forEach { app ->
            if (app.appName.lowercase() == lowerQuery && seen.add(app.packageName)) {
                results.add(app)
                android.util.Log.d("AppRepository", "Exact match: ${app.appName} (${app.packageName})")
            }
        }
        
        // Priority 2: App name starts with query
        allApps.forEach { app ->
            if (app.appName.lowercase().startsWith(lowerQuery) && seen.add(app.packageName)) {
                results.add(app)
                android.util.Log.d("AppRepository", "Starts with match: ${app.appName} (${app.packageName})")
                if (results.size >= limit) return@forEach
            }
        }
        
        // Priority 3: App name contains query
        allApps.forEach { app ->
            if (app.appName.lowercase().contains(lowerQuery) && seen.add(app.packageName)) {
                results.add(app)
                android.util.Log.d("AppRepository", "Contains match: ${app.appName} (${app.packageName})")
                if (results.size >= limit) return@forEach
            }
        }
        
        // Priority 4: Package name contains query (for advanced users)
        if (results.size < limit) {
            allApps.forEach { app ->
                if (app.packageName.lowercase().contains(lowerQuery) && seen.add(app.packageName)) {
                    results.add(app)
                    android.util.Log.d("AppRepository", "Package match: ${app.appName} (${app.packageName})")
                    if (results.size >= limit) return@forEach
                }
            }
        }
        
        android.util.Log.d("AppRepository", "Search results for '$query': ${results.size} apps found")
        return results.take(limit)
    }
    
    /**
     * Get top search suggestions for a query (legacy method for Flow).
     */
    private suspend fun getTopSearchSuggestions(query: String, limit: Int): List<AppInfo> {
        return getSearchSuggestions(query, limit)
    }
    
    /**
     * Debug method to check app visibility and permissions.
     */
    suspend fun debugAppVisibility(): String {
        val debugInfo = StringBuilder()
        
        try {
            val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            debugInfo.append("getInstalledApplications: ${allApps.size} apps\n")
        } catch (e: Exception) {
            debugInfo.append("getInstalledApplications failed: ${e.message}\n")
        }
        
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            debugInfo.append("Launcher apps: ${launcherApps.size} apps\n")
        } catch (e: Exception) {
            debugInfo.append("Launcher query failed: ${e.message}\n")
        }
        
        // Check for specific apps
        val testApps = listOf("com.whatsapp", "com.facebook.orca", "org.telegram.messenger")
        testApps.forEach { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                debugInfo.append("✓ $packageName: $appName\n")
            } catch (e: Exception) {
                debugInfo.append("✗ $packageName: Not found or no access\n")
            }
        }
        
        return debugInfo.toString()
    }
}

/**
 * PagingSource for loading apps with pagination support.
 */
class AppPagingSource(
    private val packageManager: PackageManager,
    private val query: String
) : PagingSource<Int, AppInfo>() {
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AppInfo> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            
            val allApps = loadApps()
            val filteredApps = if (query.isBlank()) {
                allApps
            } else {
                allApps.filter { app ->
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                }
            }
            
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, filteredApps.size)
            val apps = if (startIndex < filteredApps.size) {
                filteredApps.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            LoadResult.Page(
                data = apps,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (endIndex >= filteredApps.size) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, AppInfo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
    
    private fun loadApps(): List<AppInfo> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { appInfo ->
                try {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        icon = icon,
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.appName }
    }
}

/**
 * Data class representing app information.
 * 
 * @property packageName Package name of the app
 * @property appName Human-readable app name
 * @property icon App icon drawable
 * @property isSystemApp Whether this is a system app
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isSystemApp: Boolean
)