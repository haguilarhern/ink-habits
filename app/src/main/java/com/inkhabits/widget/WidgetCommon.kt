package com.inkhabits.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.inkhabits.R

object WidgetCommon {
    const val ACTION_TOGGLE_HABIT = "com.inkhabits.action.TOGGLE_HABIT"
    const val ACTION_TOGGLE_TODO = "com.inkhabits.action.TOGGLE_TODO"
    const val EXTRA_ID = "com.inkhabits.extra.ID"

    /** Rebuilds and refreshes all of the app's home-screen widgets. */
    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        notify(context, mgr, HabitsWidgetProvider::class.java)
        notify(context, mgr, ToDoWidgetProvider::class.java)
    }

    private fun notify(context: Context, mgr: AppWidgetManager, cls: Class<*>) {
        val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
        if (ids.isEmpty()) return
        val intent = android.content.Intent(context, cls).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetList)
    }
}
