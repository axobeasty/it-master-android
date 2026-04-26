package ru.itmaster.schedule.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.itmaster.schedule.ScheduleRepository

class RefreshAlarmsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = ScheduleRepository(applicationContext)
        if (!repo.getSession().isLoggedIn) return Result.success()

        val week = LessonScheduler.currentWeekMonday()
        val sch = repo.loadSchedule(week.toString()).getOrNull() ?: return Result.retry()

        LessonScheduler.updateFromSchedule(
            applicationContext,
            sch,
            week,
            repo.getNotifyBeforeMinutes(),
            repo.areNotificationsEnabled(),
        )
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "refresh_lesson_alarms"

        fun enqueue(context: Context) {
            val req = OneTimeWorkRequestBuilder<RefreshAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE,
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }
    }
}
