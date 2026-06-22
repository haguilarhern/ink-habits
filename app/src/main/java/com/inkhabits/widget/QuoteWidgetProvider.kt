package com.inkhabits.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.inkhabits.R
import com.inkhabits.util.QuotePrefs
import com.inkhabits.util.Quotes
import com.inkhabits.util.StrokeRenderer
import java.time.LocalDate

/**
 * Home-screen widget that mirrors the app's current inspirational quote — the user's
 * custom one (typed or handwritten) or the rotating daily quote when none is set.
 * A toggle switches between the transcribed and handwritten form (when both exist);
 * tapping the body opens the full in-app editor. (Quotes are edited in the app, not
 * from the launcher.)
 */
class QuoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, mgr, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetCommon.ACTION_TOGGLE_QUOTE_HW) {
            // Flip the typed<->handwritten preference and refresh every quote widget.
            QuotePrefs.setPreferHandwritten(context, !QuotePrefs.preferHandwritten(context))
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, QuoteWidgetProvider::class.java))
            for (id in ids) updateWidget(context, mgr, id)
        }
    }

    private fun updateWidget(context: Context, mgr: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_quote)

        // Same resolution the dashboard uses: typed text by default, handwriting when
        // preferred (or when there's no transcribed text), else the daily quote.
        val text = QuotePrefs.text(context)
        val strokes = QuotePrefs.strokes(context)
        val hasInk = StrokeRenderer.hasInk(strokes)
        val showInk = hasInk && (QuotePrefs.preferHandwritten(context) || text.isBlank())

        if (showInk) {
            views.setViewVisibility(R.id.quoteText, View.GONE)
            views.setViewVisibility(R.id.quoteInk, View.VISIBLE)
            StrokeRenderer.renderToBitmap(strokes, 600, 260, centerHorizontal = true)
                ?.let { views.setImageViewBitmap(R.id.quoteInk, it) }
        } else {
            views.setViewVisibility(R.id.quoteInk, View.GONE)
            views.setViewVisibility(R.id.quoteText, View.VISIBLE)
            views.setTextViewText(
                R.id.quoteText,
                if (text.isNotBlank()) text else Quotes.forToday(LocalDate.now())
            )
        }

        // Typed<->handwritten toggle: shown only when both forms exist (same rule as
        // the dashboard). Label reflects what tapping switches TO.
        if (text.isNotBlank() && hasInk) {
            views.setViewVisibility(R.id.widgetToggle, View.VISIBLE)
            views.setTextViewText(R.id.widgetToggle, if (showInk) "Aa typed" else "✍ handwritten")
            val togglePi = PendingIntent.getBroadcast(
                context, widgetId + 50_000,
                Intent(context, QuoteWidgetProvider::class.java)
                    .setAction(WidgetCommon.ACTION_TOGGLE_QUOTE_HW)
                    .setData(Uri.parse("inkhabits://quote/toggle/$widgetId")),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetToggle, togglePi)
        } else {
            views.setViewVisibility(R.id.widgetToggle, View.GONE)
        }

        // Title / body -> full in-app quote editor.
        val openEditor = PendingIntent.getActivity(
            context, widgetId + 40_000,
            Intent(context, com.inkhabits.ui.quote.QuoteEditActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, openEditor)
        views.setOnClickPendingIntent(R.id.quoteBody, openEditor)

        mgr.updateAppWidget(widgetId, views)
    }
}
