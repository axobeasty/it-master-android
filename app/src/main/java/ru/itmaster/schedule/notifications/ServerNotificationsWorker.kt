package ru.itmaster.schedule.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import ru.itmaster.schedule.ScheduleRepository
import java.util.concurrent.TimeUnit

class ServerNotificationsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = ScheduleRepository(applicationContext)
        if (!repo.getSession().isLoggedIn) return Result.success()
        repo.pollServerNotifications()
        repo.pollAssignedTestsSnapshot()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_PERIODIC = "server_notifications_periodic"
        private const val UNIQUE_ONCE = "server_notifications_once"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = PeriodicWorkRequestBuilder<ServerNotificationsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }

        fun enqueueOnce(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<ServerNotificationsWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_ONCE,
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }

        fun cancelAll(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            wm.cancelUniqueWork(UNIQUE_PERIODIC)
            wm.cancelUniqueWork(UNIQUE_ONCE)
        }
    }
}
