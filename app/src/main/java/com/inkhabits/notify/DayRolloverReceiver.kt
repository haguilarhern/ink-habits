package com.inkhabits.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.inkhabits.widget.WidgetCommon

/**
 * Refreshes all home-screen widgets at the day boundary, then re-arms the midnight
 * alarm for the next day (exact alarms are one-shot). Also re-arms on boot, since
 * alarms don't survive a reboot.
 */
class DayRolloverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ROLLOVER, Intent.ACTION_BOOT_COMPLETED -> {
                // Widget rebuilds do blocking DB reads — keep them off the main thread.
                val pending = goAsync()
                Thread {
                    try {
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
