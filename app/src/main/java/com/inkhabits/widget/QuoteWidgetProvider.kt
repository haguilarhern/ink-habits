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
 * Home-screen widget that shows the current inspirational quote — the user's custom
 * one (typed or handwritten) or the rotating daily quote when none is set. The "✎"
 * button opens the writing pad straight over the home screen to edit it, mirroring
 * the to-do widget's quick-add. Tapping the body opens the full in-app editor.
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

        // Title / body -> full in-app quote editor.
        val openEditor = PendingIntent.getActivity(
            context, widgetId + 40_000,
            Intent(context, com.inkhabits.ui.quote.QuoteEditActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, openEditor)
        views.setOnClickPendingIntent(R.id.quoteBody, openEditor)

        // "✎" -> writing pad over the home screen, saving the quote on Done without
        // ever entering the app (own task via WritingPadActivity's empty taskAffinity).
        val quickEdit = PendingIntent.getActivity(
            context, widgetId + 30_000,
            Intent(context, com.inkhabits.ui.writing.WritingPadActivity::class.java)
                .putExtra(com.inkhabits.ui.writing.WritingPadActivity.EXTRA_SAVE_QUOTE, true)
                .putExtra(com.inkhabits.ui.writing.WritingPadActivity.EXTRA_TITLE, "Your quote")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetEdit, quickEdit)

        mgr.updateAppWidget(widgetId, views)
    }
}
