package com.memoriabox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.memoriabox.MainActivity
import com.memoriabox.R
import com.memoriabox.data.model.EventType
import com.memoriabox.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_countdown)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_main_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val event = runCatching {
                    AppDatabase.getDatabase(context).eventDao()
                        .getNextUpcomingEvent(System.currentTimeMillis())
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    if (event != null) {
                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                        views.setTextViewText(R.id.widget_next_event, "下一个${widgetTypeLabel(event.type)}")
                        views.setTextViewText(R.id.widget_event_name, event.name)
                        views.setTextViewText(R.id.widget_event_meta, SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date)))
                        views.setTextViewText(R.id.widget_days_left, if (daysLeft == 0L) "今天" else daysLeft.toString())
                        views.setTextViewText(R.id.widget_days_unit, if (daysLeft == 0L) "" else "天")
                    } else {
                        views.setTextViewText(R.id.widget_next_event, "暂无即将到来的日子")
                        views.setTextViewText(R.id.widget_event_name, "添加纪念日")
                        views.setTextViewText(R.id.widget_event_meta, "点击打开 念记")
                        views.setTextViewText(R.id.widget_days_left, "--")
                        views.setTextViewText(R.id.widget_days_unit, "天")
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
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
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)

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

            views.setTextViewText(R.id.widget_today_date, dateText)
            views.setTextViewText(R.id.widget_today_day, dayText)

            CoroutineScope(Dispatchers.IO).launch {
                val count = runCatching {
                    AppDatabase.getDatabase(context).eventDao().getEventCountBetween(start, end)
                }.getOrDefault(0)
                val nextEvent = runCatching {
                    AppDatabase.getDatabase(context).eventDao()
                        .getNextUpcomingEvent(System.currentTimeMillis())
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_event_count, if (count == 0) "今天暂无日程" else "今天 $count 个日程")
                    views.setTextViewText(
                        R.id.widget_next_event_name,
                        nextEvent?.let { "最近：${it.name}" } ?: "暂无即将到来的日子"
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

private fun widgetTypeLabel(type: EventType): String = when (type) {
    EventType.COUNTDOWN -> "倒数日"
    EventType.ANNIVERSARY -> "纪念日"
    EventType.ELAPSED -> "正计时"
    EventType.BIRTHDAY -> "生日"
    EventType.TODO -> "待办"
}
