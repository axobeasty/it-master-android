package ru.itmaster.schedule

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.itmaster.schedule.notifications.RefreshAlarmsWorker
import ru.itmaster.schedule.notifications.ServerNotificationsWorker
import ru.itmaster.schedule.notifications.ensureLessonNotificationChannel
import ru.itmaster.schedule.notifications.ensureServerNotificationChannel

class ItMasterApp : Application() {
    lateinit var repository: ScheduleRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        ensureLessonNotificationChannel(this)
        ensureServerNotificationChannel(this)
        repository = ScheduleRepository(this)
        appScope.launch(Dispatchers.IO) {
            if (repository.getSession().isLoggedIn) {
                RefreshAlarmsWorker.enqueue(this@ItMasterApp)
                ServerNotificationsWorker.enqueuePeriodic(this@ItMasterApp)
                ServerNotificationsWorker.enqueueOnce(this@ItMasterApp)
            }
        }
    }
}
