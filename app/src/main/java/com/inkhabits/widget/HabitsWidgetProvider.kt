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
import com.inkhabits.data.entity.HabitCompletion
import com.inkhabits.util.Quotes
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class HabitsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetCommon.ACTION_TOGGLE_HABIT) {
            val habitId = intent.getLongExtra(WidgetCommon.EXTRA_ID, -1L)
            if (habitId > 0) {
                val today = LocalDate.now().toString()
                val db = AppDatabase.get(context)
                runBlocking {
                    if (db.habitCompletionDao().isCompleted(habitId, today)) {
                        db.habitCompletionDao().delete(habitId, today)
                    } else {
                        db.habitCompletionDao().insert(HabitCompletion(habitId = habitId, date = today))
                        // Completing from the widget can unlock a reward too.
                        com.inkhabits.util.Rewards.checkAndUnlock(context)
                    }
                }
                notifyData(context)
            }
        }
    }

    private fun notifyData(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, HabitsWidgetProvider::class.java))
        for (id in ids) updateWidget(context, mgr, id)
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.widgetList)
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_habits)
        // Headline perfect-day streak (same number the dashboard shows).
        val streak = runBlocking {
            val db = AppDatabase.get(context)
            val today = LocalDate.now()
            val habits = db.habitDao().getActive()
            val byHabit = habits.associate { h ->
                h.id to (db.habitCompletionDao().getForHabit(h.id).map { it.date }.toSet() +
                    db.streakFreezeDao().getForHabit(h.id).map { it.date }.toSet())
            }
            com.inkhabits.util.Streaks.perfectDayStreak(habits, byHabit, today)
        }
        views.setTextViewText(R.id.widgetStreak, streak.toString())

        // List adapter
        val serviceIntent = Intent(context, HabitsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse("inkhabits://habits/$widgetId")
        }
        views.setRemoteAdapter(R.id.widgetList, serviceIntent)
        views.setEmptyView(R.id.widgetList, R.id.widgetEmpty)

        // Tap a row -> toggle completion (broadcast back to this provider)
        val toggleIntent = Intent(context, HabitsWidgetProvider::class.java).apply {
            action = WidgetCommon.ACTION_TOGGLE_HABIT
            data = Uri.parse("inkhabits://habits/toggle/$widgetId")
        }
        val togglePi = PendingIntent.getBroadcast(
            context, widgetId, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widgetList, togglePi)

        // Tap header -> open app
        val openPi = PendingIntent.getActivity(
            context, widgetId, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetHeader, openPi)

        mgr.updateAppWidget(widgetId, views)
    }
}
