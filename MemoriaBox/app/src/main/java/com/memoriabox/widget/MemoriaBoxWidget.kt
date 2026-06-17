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
import java.util.Date
import java.util.Locale

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
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_memoria_box)

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            CoroutineScope(Dispatchers.IO).launch {
                val event = runCatching {
                    AppDatabase.getDatabase(context).eventDao()
                        .getNextUpcomingEvent(System.currentTimeMillis())
                }.getOrNull()

                withContext(Dispatchers.Main) {
                    if (event != null) {
                        val daysLeft = ((event.date - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0)
                        views.setTextViewText(R.id.widget_title, event.name)
                        views.setTextViewText(R.id.widget_content, widgetTypeLabel(event.type))
                        views.setTextViewText(R.id.widget_date, SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(Date(event.date)))
                        views.setTextViewText(R.id.widget_days, if (daysLeft == 0L) "今天" else "$daysLeft 天")
                    } else {
                        views.setTextViewText(R.id.widget_title, "念记")
                        views.setTextViewText(R.id.widget_content, "点击添加第一个纪念日")
                        views.setTextViewText(R.id.widget_date, "")
                        views.setTextViewText(R.id.widget_days, "")
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
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
    }
}
