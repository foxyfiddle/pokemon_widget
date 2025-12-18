package com.example.pokemonmatrixwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import android.view.View
import android.graphics.Color




private const val ACTION_TOGGLE_PAGE =
    "com.example.pokemonmatrixwidget.ACTION_TOGGLE_PAGE"

private const val PREFS_NAME = "pokemon_widget_prefs"
private const val KEY_PAGE_PREFIX = "page_"

private fun getCurrentPage(context: Context, appWidgetId: Int): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_PAGE_PREFIX + appWidgetId, 0)
}

private fun setCurrentPage(context: Context, appWidgetId: Int, page: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_PAGE_PREFIX + appWidgetId, page).apply()
}


/**
 * App Widget implementation for the daily Pokémon dot-matrix widget.
 *
 * This provider:
 * - Gets today's Pokémon ID from PokemonOfDay
 * - Downloads its sprite via SpriteDownloader
 * - Turns it into a dot-matrix bitmap via DotMatrixRenderer
 * - Updates the widget layout (widget_pokemon.xml)
 */
class DailyPokemonWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_PAGE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val current = getCurrentPage(context, appWidgetId)
                val next = if (current == 0) 1 else 0
                setCurrentPage(context, appWidgetId, next)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) { }

    override fun onDisabled(context: Context) { }
}


/**
 * Updates a single widget instance.
 */
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_pokemon)

    // 1. Set up "tap to toggle page"
    val intent = Intent(context, DailyPokemonWidget::class.java).apply {
        action = ACTION_TOGGLE_PAGE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Tap anywhere in the main circle to toggle
    views.setOnClickPendingIntent(R.id.page_container, pendingIntent)

    // 2. Get current page
    val page = getCurrentPage(context, appWidgetId)

    GlobalScope.launch(Dispatchers.IO) {
        try {
            val pokemonId = PokemonOfDay.getTodayPokemonId()
            val sprite = SpriteDownloader.fetchPokemonSprite(pokemonId)

            val dotBitmap = sprite?.let { src ->
                DotMatrixRenderer.toDotMatrix(
                    src,
                    gridWidth = 50,
                    gridHeight = 50,
                    cellSize = 5
                )
            }

            // NEW: fetch name + description
            val info = PokemonInfoFetcher.getPokemonInfo(pokemonId)
            val pokemonName = info?.name ?: "Unknown"
            val pokemonDescription = info?.description ?: ""

            withContext(Dispatchers.Main) {
                val page = getCurrentPage(context, appWidgetId)

                if (page == 0) {
                    // Image page
                    views.setViewVisibility(R.id.image_pokemon, View.VISIBLE)
                    views.setViewVisibility(R.id.details_container, View.GONE)

                    if (dotBitmap != null) {
                        views.setImageViewBitmap(R.id.image_pokemon, dotBitmap)
                    }
                    // dots: page 0 active
                    views.setTextColor(R.id.page_dot_1, Color.WHITE)
                    views.setTextColor(R.id.page_dot_2, 0x55FFFFFF.toInt())
                } else {
                    // Details page
                    views.setViewVisibility(R.id.image_pokemon, View.GONE)
                    views.setViewVisibility(R.id.details_container, View.VISIBLE)

                    views.setTextViewText(R.id.text_name, pokemonName)
                    views.setTextViewText(
                        R.id.text_number,
                        "#${pokemonId.toString().padStart(3, '0')}"
                    )
                    views.setTextViewText(R.id.text_description, pokemonDescription)

                    // dots: page 1 active
                    views.setTextColor(R.id.page_dot_1, 0x55FFFFFF.toInt())
                    views.setTextColor(R.id.page_dot_2, Color.WHITE)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}


