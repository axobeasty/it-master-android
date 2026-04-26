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

private const val CHANNEL_ID = "server_messages"

fun ensureServerNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val ch = NotificationChannel(
        CHANNEL_ID,
        "Сообщения от колледжа",
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = "Назначение тестов и другие уведомления"
    }
    mgr.createNotificationChannel(ch)
}

fun showServerPushNotification(context: Context, title: String, text: String, notificationId: Int) {
    ensureServerNotificationChannel(context.applicationContext)
    val app = context.applicationContext
    val open = Intent(app, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(app).notify(notificationId, n)
}
