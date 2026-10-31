package com.example.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages VPS Sync configuration and persisted state in SharedPreferences
 */
object VpsSyncPreferences {

    private const val PREFS_NAME = "vps_sync_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_AUTO_SYNC = "auto_sync"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"
    private const val KEY_LAST_SYNC_STATUS = "last_sync_status"
    private const val KEY_LAST_SYNC_SUCCESS = "last_sync_success"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, "") ?: ""
    }

    fun setServerUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, url.trim().removeSuffix("/")).apply()
    }

    fun getApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_API_KEY, "") ?: ""
    }

    fun setApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_SYNC, false)
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
    }

    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    fun setLastSyncTime(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }

    fun getLastSyncStatus(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_SYNC_STATUS, "لم تتم المزامنة بعد") ?: "لم تتم المزامنة بعد"
    }

    fun setLastSyncStatus(context: Context, status: String, isSuccess: Boolean) {
        getPrefs(context).edit()
            .putString(KEY_LAST_SYNC_STATUS, status)
            .putBoolean(KEY_LAST_SYNC_SUCCESS, isSuccess)
            .apply()
    }

    fun isLastSyncSuccessful(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LAST_SYNC_SUCCESS, false)
    }
}
