package ru.itmaster.schedule.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "lesson_reminders"

fun ensureLessonNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val ch = NotificationChannel(
        CHANNEL_ID,
        "Напоминания о парах",
        NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "Уведомление перед началом занятия"
    }
    mgr.createNotificationChannel(ch)
}

fun lessonNotificationChannelId(): String = CHANNEL_ID

fun canPostNotifications(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()
