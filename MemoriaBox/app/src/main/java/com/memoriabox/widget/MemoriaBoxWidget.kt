package com.memoriabox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.memoriabox.MainActivity
import com.memoriabox.R
import com.memoriabox.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MemoriaBoxWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
    }

    override fun onDisabled(context: Context) {
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_memoria_box)
            
            // Set up click intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
            
            val event = runCatching {
                runBlocking(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).eventDao().getNextUpcomingEvent(System.currentTimeMillis())
                }
            }.getOrNull()

            if (event != null) {
                val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                views.setTextViewText(R.id.widget_title, event.name)
                views.setTextViewText(R.id.widget_content, "距离目标还有")
                views.setTextViewText(R.id.widget_days, "$daysLeft 天")
            } else {
                views.setTextViewText(R.id.widget_title, "MemoriaBox")
                views.setTextViewText(R.id.widget_content, "点击添加第一个纪念日")
                views.setTextViewText(R.id.widget_days, "")
            }
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
