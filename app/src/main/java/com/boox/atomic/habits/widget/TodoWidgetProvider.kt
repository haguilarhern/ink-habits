package com.boox.atomic.habits.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.boox.atomic.habits.R

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo)
            val todosText = buildTodosText(context)
            views.setTextViewText(R.id.widgetTodoList, todosText)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun buildTodosText(context: Context): String {
        return "□ Pick up groceries\n□ Call dentist"
    }
}