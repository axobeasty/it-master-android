package ru.itmaster.schedule.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import ru.itmaster.schedule.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val latestVersionName: String,
    val releaseNotes: String,
    val assetUrl: String,
    val assetFileName: String,
)

object AppUpdateManager {
    private const val PREFS = "app_updates"
    private const val KEY_PENDING_DOWNLOAD_ID = "pending_download_id"
    private const val KEY_PENDING_FILE_NAME = "pending_file_name"
    private const val KEY_SKIP_VERSION = "skip_version"

    fun checkForUpdate(): Result<UpdateInfo?> = runCatching {
        val endpoint = BuildConfig.GITHUB_RELEASES_LATEST_API
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "IT-Master-Android/${BuildConfig.VERSION_NAME}")
        }
        conn.connect()
        val code = conn.responseCode
        if (code !in 200..299) {
            return@runCatching null
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        parseLatestRelease(body)
    }

    fun isSkipped(context: Context, versionName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SKIP_VERSION, null) == versionName
    }

    fun skipVersion(context: Context, versionName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIP_VERSION, versionName)
            .apply()
    }

    fun clearSkippedVersion(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SKIP_VERSION)
            .apply()
    }

    fun enqueueUpdateDownload(context: Context, update: UpdateInfo): Long {
        val fileName = "it-master-${update.latestVersionName}.apk"
        val req = DownloadManager.Request(Uri.parse(update.assetUrl))
            .setTitle("Обновление IT-Master")
            .setDescription("Скачивание версии ${update.latestVersionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(req)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PENDING_DOWNLOAD_ID, id)
            .putString(KEY_PENDING_FILE_NAME, fileName)
            .apply()
        return id
    }

    fun getPendingDownloadId(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_PENDING_DOWNLOAD_ID, -1L)

    fun clearPendingDownload(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_DOWNLOAD_ID)
            .remove(KEY_PENDING_FILE_NAME)
            .apply()
    }

    fun getDownloadStatus(context: Context, downloadId: Long): Int? {
        if (downloadId <= 0L) return null
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val q = DownloadManager.Query().setFilterById(downloadId)
        dm.query(q).use { c ->
            if (!c.moveToFirst()) return null
            val idx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (idx < 0) return null
            return c.getInt(idx)
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val i = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(i)
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun installDownloadedApk(context: Context, downloadId: Long): Boolean {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId) ?: return false
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(i)
        return true
    }

    private fun parseLatestRelease(json: String): UpdateInfo? {
        val root = JSONObject(json)
        val tag = root.optString("tag_name").trim()
        val version = normalizeVersion(tag)
        if (version.isBlank()) return null
        if (!isVersionNewer(version, BuildConfig.VERSION_NAME)) return null

        val assets = root.optJSONArray("assets") ?: JSONArray()
        val chosen = chooseApkAsset(assets) ?: return null
        val url = chosen.optString("browser_download_url")
        if (url.isBlank()) return null
        return UpdateInfo(
            latestVersionName = version,
            releaseNotes = root.optString("body").orEmpty(),
            assetUrl = url,
            assetFileName = chosen.optString("name").ifBlank { "update.apk" },
        )
    }

    private fun chooseApkAsset(assets: JSONArray): JSONObject? {
        var fallback: JSONObject? = null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val name = a.optString("name").lowercase()
            if (!name.endsWith(".apk")) continue
            if ("unsigned" !in name && "release" in name) return a
            if (fallback == null) fallback = a
        }
        return fallback
    }

    private fun normalizeVersion(raw: String): String {
        var v = raw.trim()
        if (v.startsWith("v", ignoreCase = true)) v = v.substring(1)
        return v
    }

    private fun isVersionNewer(candidate: String, current: String): Boolean {
        val a = candidate.split('.').mapNotNull { it.toIntOrNull() }
        val b = normalizeVersion(current).split('.').mapNotNull { it.toIntOrNull() }
        val max = maxOf(a.size, b.size)
        for (i in 0 until max) {
            val av = a.getOrElse(i) { 0 }
            val bv = b.getOrElse(i) { 0 }
            if (av > bv) return true
            if (av < bv) return false
        }
        return false
    }
}
