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
        refresh(context, mgr, HabitsWidgetProvider(), HabitsWidgetProvider::class.java)
        refresh(context, mgr, ToDoWidgetProvider(), ToDoWidgetProvider::class.java)
    }

    // Apps can't send the protected ACTION_APPWIDGET_UPDATE broadcast, so call the
    // provider's onUpdate directly (rebuilds the frame) + refresh the list contents.
    private fun refresh(
        context: Context,
        mgr: AppWidgetManager,
        provider: android.appwidget.AppWidgetProvider,
        cls: Class<*>
    ) {
        val ids = mgr.getAppWidgetIds(ComponentName(context, cls))
        if (ids.isEmpty()) return
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetList)
        provider.onUpdate(context, mgr, ids)
    }
}
