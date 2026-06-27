package com.inkhabits.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inkhabits.util.Freezes
import com.inkhabits.widget.WidgetCommon
import kotlinx.coroutines.runBlocking

/**
 * At the day boundary: consume protective totems for any streak broken by yesterday's
 * miss ([Freezes.reconcile]), refresh all home-screen widgets, then re-arm the midnight
 * alarm for the next day (exact alarms are one-shot). Also runs on boot, since alarms
 * don't survive a reboot.
 */
class DayRolloverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ROLLOVER, Intent.ACTION_BOOT_COMPLETED -> {
                // Freeze reconcile + widget rebuilds do blocking DB work — off the main thread.
                val pending = goAsync()
                Thread {
                    try {
                        runBlocking { Freezes.reconcile(context) }
                        WidgetCommon.updateAll(context)
                        DayRolloverScheduler.schedule(context)
                    } finally { pending.finish() }
                }.start()
            }
        }
    }

    companion object {
        const val ACTION_ROLLOVER = "com.inkhabits.action.DAY_ROLLOVER"
    }
}
