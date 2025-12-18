package com.example.pokemonmatrixwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget instance is added.
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget instance is removed.
    }
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

    GlobalScope.launch(Dispatchers.IO) {
        try {
            // 1. Get today's Pokémon ID (seeded random)
            val pokemonId = PokemonOfDay.getTodayPokemonId()

            // 2. Download the sprite bitmap
            val sprite = SpriteDownloader.fetchPokemonSprite(pokemonId)

            // 3. Convert to dot-matrix using your tuned renderer
            val dotBitmap = sprite?.let { src ->
                // Keep your grid + cellSize, just scale the final image
                DotMatrixRenderer.toDotMatrixScaled(
                    src,
                    gridWidth = 60,
                    gridHeight = 60,
                    cellSize = 5,
                    scale = 5.0f  // try 0.8f, 0.9f, 1.2f etc.
                )
            }


            // 4. Update the widget views (image only)
            withContext(Dispatchers.Main) {
                if (dotBitmap != null) {
                    views.setImageViewBitmap(R.id.image_pokemon, dotBitmap)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            // If something goes wrong, just update without changing the image
            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}

