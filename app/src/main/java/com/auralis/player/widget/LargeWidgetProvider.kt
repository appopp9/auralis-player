package com.auralis.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/** Large 4x2 home-screen widget. See [WidgetRenderer] for the actual rendering. */
class LargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> WidgetRenderer.render(context, appWidgetManager, id, WidgetSize.LARGE) }
    }
}
