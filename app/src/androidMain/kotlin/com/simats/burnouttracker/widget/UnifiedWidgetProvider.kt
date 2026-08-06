package com.simats.burnouttracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.simats.burnouttracker.MainActivity

class UnifiedWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateUnifiedAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == "WIDGET_NEXT" || action == "WIDGET_PREV") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val views = RemoteViews(context.packageName, context.resources.getIdentifier("widget_unified_dashboard", "layout", context.packageName))
                if (action == "WIDGET_NEXT") {
                    views.showNext(context.resources.getIdentifier("widget_flipper", "id", context.packageName))
                } else {
                    views.showPrevious(context.resources.getIdentifier("widget_flipper", "id", context.packageName))
                }
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views)
            }
        }
    }
}

internal fun updateUnifiedAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, context.resources.getIdentifier("widget_unified_dashboard", "layout", context.packageName))

    // Next Intent on the whole widget
    val nextIntent = Intent(context, UnifiedWidgetProvider::class.java).apply {
        action = "WIDGET_NEXT"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val nextPending = PendingIntent.getBroadcast(context, appWidgetId * 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(context.resources.getIdentifier("widget_flipper", "id", context.packageName), nextPending)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
