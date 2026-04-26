package ru.itmaster.schedule.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import ru.itmaster.schedule.MainActivity
import ru.itmaster.schedule.data.api.ScheduleResponse
import ru.itmaster.schedule.sessionDataStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

private val scheduledLessonAlarmKeys = stringPreferencesKey("scheduled_lesson_alarm_keys")

object LessonScheduler {

    suspend fun updateFromSchedule(
        context: Context,
        schedule: ScheduleResponse,
        weekMonday: LocalDate,
        minutesBefore: Int,
        enabled: Boolean,
    ) {
        cancelAll(context)

        if (!enabled || minutesBefore <= 0) return

        val store = context.sessionDataStore

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val weekStart = parseWeekStart(schedule.weekStart, weekMonday)

        val keys = mutableListOf<String>()
        val am = context.getSystemService(AlarmManager::class.java) ?: return

        for (entry in schedule.entries) {
            val lessonDay = weekStart.plusDays((entry.weekday - 1).toLong())
            val start = parseStartTime(entry.startTime) ?: continue
            val lessonStart = ZonedDateTime.of(lessonDay, start, zone)
            val trigger = lessonStart.minusMinutes(minutesBefore.toLong())
            if (!trigger.isAfter(now)) continue

            val key = alarmKey(entry.id, weekStart)
            keys.add(key)
            val rc = requestCode(key)

            val body = buildString {
                append(entry.weekdayLabel)
                append(", ")
                append(entry.startTime.take(5))
                entry.subject?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
            }

            val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
                data = alarmUri(entry.id, weekStart)
                putExtra(LessonAlarmReceiver.EXTRA_TITLE, "Скоро пара")
                putExtra(LessonAlarmReceiver.EXTRA_BODY, body)
            }

            val pi = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val show = PendingIntent.getActivity(
                context,
                rc + 1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val clock = AlarmManager.AlarmClockInfo(trigger.toInstant().toEpochMilli(), show)
            am.setAlarmClock(clock, pi)
        }

        if (keys.isNotEmpty()) {
            store.edit { it[scheduledLessonAlarmKeys] = keys.joinToString(",") }
        }
    }

    private fun parseWeekStart(raw: String, fallbackMonday: LocalDate): LocalDate {
        val datePart = raw.trim().take(10)
        return try {
            LocalDate.parse(datePart)
        } catch (_: DateTimeParseException) {
            fallbackMonday
        }
    }

    private fun parseStartTime(raw: String): LocalTime? {
        val parts = raw.trim().split(":")
        if (parts.size < 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return try {
            LocalTime.of(h, m)
        } catch (_: Exception) {
            null
        }
    }

    private fun alarmKey(entryId: Long, weekMonday: LocalDate): String =
        "${entryId}:${weekMonday.toEpochDay()}"

    private fun alarmUri(entryId: Long, weekMonday: LocalDate): Uri =
        Uri.parse("itmaster://lesson/${alarmKey(entryId, weekMonday)}")

    private fun requestCode(key: String): Int = key.hashCode() and 0x7fff_ffff

    suspend fun cancelAll(context: Context) {
        val store = context.sessionDataStore
        val keysStr = store.data.first()[scheduledLessonAlarmKeys]
        if (keysStr.isNullOrBlank()) return

        val am = context.getSystemService(AlarmManager::class.java) ?: return
        for (key in keysStr.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val parts = key.split(':')
            if (parts.size != 2) continue
            val entryId = parts[0].toLongOrNull() ?: continue
            val epoch = parts[1].toLongOrNull() ?: continue
            val weekMonday = LocalDate.ofEpochDay(epoch)
            val intent = Intent(context, LessonAlarmReceiver::class.java).apply {
                data = alarmUri(entryId, weekMonday)
            }
            val rc = requestCode(key)
            val pi = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            pi?.let { am.cancel(it) }
        }
        store.edit { it.remove(scheduledLessonAlarmKeys) }
    }

    fun currentWeekMonday(): LocalDate =
        LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
