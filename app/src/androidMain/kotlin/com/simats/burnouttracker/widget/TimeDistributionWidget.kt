package com.simats.burnouttracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.simats.burnouttracker.MainActivity

class TimeDistributionWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateTimeDistributionAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}

internal fun updateTimeDistributionAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, context.resources.getIdentifier("widget_time_distribution", "layout", context.packageName))

    // Intent to open the main app when clicking the widget
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("NAVIGATE_TO", "productivity")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 
        0, 
        intent, 
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(context.resources.getIdentifier("widget_container", "id", context.packageName), pendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
