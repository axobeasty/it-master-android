package ru.itmaster.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.itmaster.schedule.MainActivity
import ru.itmaster.schedule.R

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureLessonNotificationChannel(context)
        val nm = NotificationManagerCompat.from(context.applicationContext)
        if (!nm.areNotificationsEnabled()) return

        val lead = intent.getIntExtra(EXTRA_LEAD_MINUTES, 0)
        val title = if (lead > 0) {
            context.getString(R.string.notify_lesson_title_lead, lead)
        } else {
            context.getString(R.string.notify_lesson_title)
        }
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = android.app.PendingIntent.getActivity(
            context,
            0,
            open,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, lessonNotificationChannelId())
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        val id = (intent.data?.toString() ?: title + body).hashCode()
        nm.notify(id, notification)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_LEAD_MINUTES = "lead_minutes"
    }
}
