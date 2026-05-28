package com.inkhabits.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.inkhabits.R
import com.inkhabits.data.AppDatabase
import com.inkhabits.util.StrokeRenderer
import kotlinx.coroutines.runBlocking

class ToDoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        ToDoRemoteViewsFactory(applicationContext)
}

private data class WidgetTodo(
    val id: Long,
    val title: String,
    val strokes: String,
    val done: Boolean
)

class ToDoRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<WidgetTodo> = emptyList()

    override fun onCreate() {}
    override fun onDestroy() { rows = emptyList() }
    override fun getCount(): Int = rows.size
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows[position].id
    override fun hasStableIds(): Boolean = true
    override fun getLoadingView(): RemoteViews? = null

    override fun onDataSetChanged() {
        val db = AppDatabase.get(context)
        rows = runBlocking {
            db.toDoDao().getAll().map {
                WidgetTodo(it.id, it.title, it.titleStrokes, it.isDone)
            }
        }
    }

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_todo_item)
        rv.setImageViewResource(
            R.id.itemCheck,
            if (row.done) R.drawable.ic_widget_check_on else R.drawable.ic_widget_check_off
        )
        if (StrokeRenderer.hasInk(row.strokes)) {
            rv.setViewVisibility(R.id.itemName, android.view.View.GONE)
            rv.setViewVisibility(R.id.itemNameInk, android.view.View.VISIBLE)
            val bmp = StrokeRenderer.renderToBitmap(row.strokes, 360, 70)
            if (bmp != null) rv.setImageViewBitmap(R.id.itemNameInk, bmp)
        } else {
            rv.setViewVisibility(R.id.itemNameInk, android.view.View.GONE)
            rv.setViewVisibility(R.id.itemName, android.view.View.VISIBLE)
            rv.setTextViewText(R.id.itemName, row.title.ifBlank { "To-do" })
        }
        val fillIn = Intent().putExtra(WidgetCommon.EXTRA_ID, row.id)
        rv.setOnClickFillInIntent(R.id.itemRoot, fillIn)
        return rv
    }
}
