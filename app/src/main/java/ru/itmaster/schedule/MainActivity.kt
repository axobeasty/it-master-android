package ru.itmaster.schedule

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import ru.itmaster.schedule.notifications.RefreshAlarmsWorker
import ru.itmaster.schedule.notifications.EXTRA_OPEN_TEST_ID
import ru.itmaster.schedule.ui.AppNav
import ru.itmaster.schedule.ui.theme.ItMasterTheme
import ru.itmaster.schedule.updates.AppUpdateManager
import ru.itmaster.schedule.updates.UpdateInfo

class MainActivity : ComponentActivity() {
    private val pendingOpenTestId = MutableStateFlow<Long?>(null)
    private var updatePromptShown = false
    private companion object {
        const val FOREGROUND_POLL_INTERVAL_MS = 60_000L
    }

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* при отказе пользователь может включить уведомления в настройках системы */ }

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val pending = AppUpdateManager.getPendingDownloadId(this@MainActivity)
            if (id > 0L && id == pending) {
                AppUpdateManager.clearPendingDownload(this@MainActivity)
                promptInstallDownloadedUpdate(id)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentForTestOpen(intent)
        requestPostNotificationsIfNeeded()
        enableEdgeToEdge()

        val app = application as ItMasterApp
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    if (app.repository.getSession().isLoggedIn) {
                        app.repository.pollServerNotifications()
                        app.repository.pollAssignedTestsSnapshot()
                        RefreshAlarmsWorker.enqueue(this@MainActivity)
                    }
                    delay(FOREGROUND_POLL_INTERVAL_MS)
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            maybeCheckForAppUpdate()
        }

        setContent {
            val openTestIdFromNotification by pendingOpenTestId.collectAsState()
            ItMasterTheme {
                AppNav(
                    repository = app.repository,
                    modifier = Modifier.fillMaxSize(),
                    openTestIdFromNotification = openTestIdFromNotification,
                    onOpenTestNotificationHandled = { pendingOpenTestId.value = null },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadCompleteReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadCompleteReceiver, filter)
        }
    }

    override fun onStop() {
        runCatching { unregisterReceiver(downloadCompleteReceiver) }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        val pendingId = AppUpdateManager.getPendingDownloadId(this)
        if (pendingId > 0L) {
            val status = AppUpdateManager.getDownloadStatus(this, pendingId)
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                AppUpdateManager.clearPendingDownload(this)
                promptInstallDownloadedUpdate(pendingId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentForTestOpen(intent)
    }

    private fun handleIntentForTestOpen(intent: Intent?) {
        val openId = intent?.getLongExtra(EXTRA_OPEN_TEST_ID, 0L) ?: 0L
        if (openId > 0L) {
            pendingOpenTestId.value = openId
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestNotifPermission.launch(permission)
        }
    }

    private suspend fun maybeCheckForAppUpdate() {
        checkForAppUpdate(manual = false)
    }

    fun checkForUpdatesManually() {
        lifecycleScope.launch(Dispatchers.IO) {
            checkForAppUpdate(manual = true)
        }
    }

    private suspend fun checkForAppUpdate(manual: Boolean) {
        if (updatePromptShown && !manual) return
        val update = AppUpdateManager.checkForUpdate().getOrNull()
        if (update == null) {
            if (manual) {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Обновлений не найдено или GitHub недоступен",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            return
        }
        if (!manual && AppUpdateManager.isSkipped(this, update.latestVersionName)) return
        updatePromptShown = true
        lifecycleScope.launch(Dispatchers.Main) {
            showUpdateDialog(update)
        }
    }

    private fun showUpdateDialog(update: UpdateInfo) {
        val notes = update.releaseNotes
            .lines()
            .take(8)
            .joinToString("\n")
            .ifBlank { "Доступно обновление приложения." }
        val message = buildString {
            append("Новая версия: ")
            append(update.latestVersionName)
            append("\n\n")
            append(notes)
        }
        AlertDialog.Builder(this)
            .setTitle("Доступно обновление")
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Скачать") { _, _ ->
                AppUpdateManager.clearSkippedVersion(this)
                AppUpdateManager.enqueueUpdateDownload(this, update)
                Toast.makeText(this, "Загрузка обновления началась", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Пропустить версию") { _, _ ->
                AppUpdateManager.skipVersion(this, update.latestVersionName)
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    private fun promptInstallDownloadedUpdate(downloadId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Обновление загружено")
            .setMessage("Установить новую версию сейчас?")
            .setPositiveButton("Установить") { _, _ ->
                if (!AppUpdateManager.canInstallPackages(this)) {
                    Toast.makeText(
                        this,
                        "Разрешите установку из этого источника и повторите установку.",
                        Toast.LENGTH_LONG,
                    ).show()
                    AppUpdateManager.openInstallPermissionSettings(this)
                    return@setPositiveButton
                }
                val ok = runCatching { AppUpdateManager.installDownloadedApk(this, downloadId) }.getOrDefault(false)
                if (!ok) {
                    Toast.makeText(this, "Не удалось открыть установщик APK", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Позже", null)
            .show()
    }
}
