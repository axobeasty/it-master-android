package ru.itmaster.schedule

import android.app.Application
import ru.itmaster.schedule.notifications.ensureLessonNotificationChannel
import ru.itmaster.schedule.notifications.ensureServerNotificationChannel

class ItMasterApp : Application() {
    lateinit var repository: ScheduleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        ensureLessonNotificationChannel(this)
        ensureServerNotificationChannel(this)
        repository = ScheduleRepository(this)
    }
}
