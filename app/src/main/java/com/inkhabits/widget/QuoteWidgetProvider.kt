package com.inkhabits.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
 * Just the quote; tapping it opens the in-app editor. The typed<->handwritten choice
 * is made in the app (its preference drives what this widget shows).
 */
class QuoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) updateWidget(context, mgr, id)
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

        // Tap the quote -> full in-app quote editor.
        val openEditor = PendingIntent.getActivity(
            context, widgetId + 40_000,
            Intent(context, com.inkhabits.ui.quote.QuoteEditActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.quoteBody, openEditor)

        mgr.updateAppWidget(widgetId, views)
    }
}
