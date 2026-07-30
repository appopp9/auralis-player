package com.auralis.player.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Compact 1x1 home-screen widget. All actual rendering/refresh logic lives in
 * [WidgetRenderer]; each provider only forwards the update broadcast.
 */
class CompactWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> WidgetRenderer.render(context, appWidgetManager, id, WidgetSize.COMPACT) }
    }
}
