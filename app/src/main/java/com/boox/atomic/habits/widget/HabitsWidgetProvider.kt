package com.boox.atomic.habits.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.boox.atomic.habits.R

class HabitsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_habits)
            val habitsText = buildHabitsText(context)
            views.setTextViewText(R.id.widgetHabitsList, habitsText)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun buildHabitsText(context: Context): String {
        return "📖 READER\n□ Read 30 min         3\n□ Finish book/mo      1  (weekly)\n\n🏃 ATHLETE\n□ Morning run         7\n□ Stretch             M/W/F"
    }
}