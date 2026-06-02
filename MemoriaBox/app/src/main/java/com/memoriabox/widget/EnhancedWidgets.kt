package com.memoriabox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.memoriabox.MainActivity
import com.memoriabox.R
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.toArgb

class CountdownWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)
            
            // Click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_main_container, pendingIntent)
            
            // TODO: Fetch next upcoming event from database
            // For now, show placeholder
            views.setTextViewText(R.id.widget_next_event, "下一个事件")
            views.setTextViewText(R.id.widget_event_name, "点击配置")
            views.setTextViewText(R.id.widget_days_left, "--")
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class CalendarWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            
            // Click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_calendar_container, pendingIntent)
            
            // TODO: Show today's events
            views.setTextViewText(R.id.widget_today_date, "今天")
            views.setTextViewText(R.id.widget_event_count, "0 个事件")
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
