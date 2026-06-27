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
        // Roll the widgets over to the new day automatically at midnight.
        com.inkhabits.notify.DayRolloverScheduler.schedule(this)
        // Off the main thread (these do blocking DB reads) so app start stays snappy:
        //  - re-arm per-habit reminder alarms (they don't survive process death/reboot)
        //  - refresh widgets so their tap targets reflect the current build.
        appScope.launch {
            HabitReminderScheduler.rescheduleAll(this@InkHabitsApp)
            // Catch up any missed-day freezes since the app last ran, then refresh widgets.
            com.inkhabits.util.Freezes.reconcile(this@InkHabitsApp)
            com.inkhabits.widget.WidgetCommon.updateAll(this@InkHabitsApp)
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
