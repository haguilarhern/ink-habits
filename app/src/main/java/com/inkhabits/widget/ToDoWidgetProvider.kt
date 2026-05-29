package com.inkhabits.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.inkhabits.MainActivity
import com.inkhabits.R
import com.inkhabits.data.AppDatabase
import kotlinx.coroutines.runBlocking

class ToDoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetCommon.ACTION_TOGGLE_TODO) {
            val todoId = intent.getLongExtra(WidgetCommon.EXTRA_ID, -1L)
            if (todoId > 0) {
                val db = AppDatabase.get(context)
                runBlocking {
                    db.toDoDao().getAll().firstOrNull { it.id == todoId }?.let {
                        val newDone = !it.isDone
                        db.toDoDao().update(it.copy(isDone = newDone))
                        // Keep the cumulative "done this year" tally in sync.
                        com.inkhabits.util.YearTally.add(context, if (newDone) 1 else -1)
                    }
                }
                notifyData(context)
            }
        }
    }

    private fun notifyData(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, ToDoWidgetProvider::class.java))
        for (id in ids) updateWidget(context, mgr, id)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetList)
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_todo)
        views.setTextViewText(R.id.widgetCount, com.inkhabits.util.YearTally.get(context).toString())

        val serviceIntent = Intent(context, ToDoWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("inkhabits://todos/$widgetId")
        }
        views.setRemoteAdapter(R.id.widgetList, serviceIntent)
        views.setEmptyView(R.id.widgetList, R.id.widgetEmpty)

        val toggleIntent = Intent(context, ToDoWidgetProvider::class.java).apply {
            action = WidgetCommon.ACTION_TOGGLE_TODO
            data = Uri.parse("inkhabits://todos/toggle/$widgetId")
        }
        val togglePi = PendingIntent.getBroadcast(
            context, widgetId, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widgetList, togglePi)

        // Open the To-Do screen (to add / manage) from the header or the + button.
        val openTodo = PendingIntent.getActivity(
            context, widgetId + 10_000,
            Intent(context, com.inkhabits.ui.todo.ToDoActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetHeader, openTodo)
        views.setOnClickPendingIntent(R.id.widgetAdd, openTodo)

        mgr.updateAppWidget(widgetId, views)
    }
}
