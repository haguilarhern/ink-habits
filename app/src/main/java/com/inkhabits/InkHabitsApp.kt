package com.inkhabits

import android.app.Application
import android.os.Build
import com.inkhabits.notify.HabitReminderScheduler
import com.inkhabits.notify.NotificationHelper
import com.inkhabits.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass

class InkHabitsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The Onyx pen SDK reflects into hidden android.onyx.* APIs; exempt them
        // so raw drawing can reach the EPD controller instead of hanging.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (_: Throwable) {
            }
        }
        NotificationHelper.ensureChannel(this)
        ReminderScheduler.schedule(this)
        // Re-arm per-habit reminder alarms (they don't survive process death / reboot).
        appScope.launch { HabitReminderScheduler.rescheduleAll(this@InkHabitsApp) }
        // Refresh widgets so their tap targets (e.g. the to-do "+" quick-add pad)
        // always reflect the current build's PendingIntents.
        com.inkhabits.widget.WidgetCommon.updateAll(this)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
