package com.auralis.player.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Broadcasts a refresh to every Auralis widget. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun update() {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            CompactWidgetProvider::class.java,
            MediumWidgetProvider::class.java,
            LargeWidgetProvider::class.java
        ).forEach { provider ->
            val ids = runCatching {
                manager.getAppWidgetIds(ComponentName(context, provider))
            }.getOrDefault(IntArray(0))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, provider).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        }
    }
}
