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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
            
            val event = runCatching {
                runBlocking(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).eventDao().getNextUpcomingEvent(System.currentTimeMillis())
                }
            }.getOrNull()

            if (event != null) {
                val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                views.setTextViewText(R.id.widget_next_event, "下一个事件")
                views.setTextViewText(R.id.widget_event_name, event.name)
                views.setTextViewText(R.id.widget_days_left, daysLeft.toString())
            } else {
                views.setTextViewText(R.id.widget_next_event, "暂无 upcoming")
                views.setTextViewText(R.id.widget_event_name, "添加纪念日")
                views.setTextViewText(R.id.widget_days_left, "--")
            }
            
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
            
            val calendar = Calendar.getInstance()
            val dateText = SimpleDateFormat("MM月dd日", Locale.getDefault()).format(Date())
            val dayText = calendar.get(Calendar.DAY_OF_MONTH).toString()
            val start = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = start + 86_400_000L - 1
            val count = runCatching {
                runBlocking(Dispatchers.IO) {
                    AppDatabase.getDatabase(context).eventDao().getEventCountBetween(start, end)
                }
            }.getOrDefault(0)

            views.setTextViewText(R.id.widget_today_date, dateText)
            views.setTextViewText(R.id.widget_today_day, dayText)
            views.setTextViewText(R.id.widget_event_count, "$count 个事件")
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
