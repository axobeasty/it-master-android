package ru.itmaster.schedule.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        RefreshAlarmsWorker.enqueue(app)
        ServerNotificationsWorker.enqueuePeriodic(app)
        ServerNotificationsWorker.enqueueOnce(app)
    }
}
