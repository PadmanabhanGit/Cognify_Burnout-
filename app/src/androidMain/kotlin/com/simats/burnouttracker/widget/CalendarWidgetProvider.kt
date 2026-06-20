package com.simats.burnouttracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.simats.burnouttracker.R
import java.text.SimpleDateFormat
import java.util.*

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.calendar_widget)
    
    // Add click listener to launch the app
    val intent = android.content.Intent(context, com.simats.burnouttracker.MainActivity::class.java)
    val pendingIntent = android.app.PendingIntent.getActivity(
        context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_month_text, pendingIntent)

    val prefs = context.getSharedPreferences("burnout_history", Context.MODE_PRIVATE)
    val currentMonth = Calendar.getInstance()
    
    val monthFormat = SimpleDateFormat("MMMM, yyyy", Locale.getDefault())
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    views.setTextViewText(R.id.widget_month_text, monthFormat.format(currentMonth.time))
    
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfMonth = Calendar.getInstance().apply {
        time = currentMonth.time
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0)

    val daysList = mutableListOf<Int?>()
    for (i in 0 until firstDayOfMonth) daysList.add(null)
    for (i in 1..daysInMonth) daysList.add(i)
    
    while(daysList.size < 42) {
        daysList.add(null)
    }

    for (i in 0 until 42) {
        val viewId = context.resources.getIdentifier("cell_$i", "id", context.packageName)
        val day = daysList[i]
        
        if (day == null) {
            views.setTextViewText(viewId, "")
            views.setInt(viewId, "setBackgroundResource", 0)
        } else {
            views.setTextViewText(viewId, day.toString())
            
            val cal = Calendar.getInstance().apply {
                time = currentMonth.time
                set(Calendar.DAY_OF_MONTH, day)
            }
            
            val todayCal = Calendar.getInstance()
            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) && 
                          cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                          
            val dateStr = dayFormat.format(cal.time)
            val risk = prefs.getString(dateStr, null)
            
            val bgRes = when {
                isToday -> R.drawable.widget_cell_today
                risk == "LOW" -> R.drawable.widget_cell_low
                risk == "MODERATE" -> R.drawable.widget_cell_moderate
                risk == "HIGH" -> R.drawable.widget_cell_high
                else -> 0
            }
            views.setInt(viewId, "setBackgroundResource", bgRes)
        }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
