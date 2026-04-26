package ru.itmaster.schedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.itmaster.schedule.MainActivity
import ru.itmaster.schedule.R

/** Совпадает с текстом на сервере при назначении теста группе. */
const val SERVER_ASSIGN_TEST_TITLE = "Назначен тест"
const val SERVER_TEST_SUBMITTED_TITLE = "Тест группы сдан"
const val EXTRA_OPEN_TEST_ID = "open_test_id"

private const val CHANNEL_ID = "server_messages"

fun ensureServerNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val ch = NotificationChannel(
        CHANNEL_ID,
        "Сообщения от колледжа",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Новые тесты (опрос API) и сообщения с сервера"
        enableVibration(true)
    }
    mgr.createNotificationChannel(ch)
}

fun showServerPushNotification(
    context: Context,
    title: String,
    text: String,
    notificationId: Int,
    openTestId: Long? = null,
) {
    ensureServerNotificationChannel(context.applicationContext)
    val app = context.applicationContext
    val open = Intent(app, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        openTestId?.let { putExtra(EXTRA_OPEN_TEST_ID, it) }
    }
    val pending = PendingIntent.getActivity(
        app,
        notificationId,
        open,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val n = NotificationCompat.Builder(app, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_app_icon)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(pending)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setAutoCancel(true)
        .build()
    val nm = NotificationManagerCompat.from(app)
    if (nm.areNotificationsEnabled()) {
        nm.notify(notificationId, n)
    }
}
