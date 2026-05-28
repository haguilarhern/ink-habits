package com.inkhabits

import android.app.Application
import android.os.Build
import com.inkhabits.notify.NotificationHelper
import com.inkhabits.notify.ReminderScheduler
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
    }
}
