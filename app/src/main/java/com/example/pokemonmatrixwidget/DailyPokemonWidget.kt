package com.example.pokemonmatrixwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private const val ACTION_PAGE_NEXT =
    "com.example.pokemonmatrixwidget.ACTION_PAGE_NEXT"
private const val ACTION_PAGE_PREV =
    "com.example.pokemonmatrixwidget.ACTION_PAGE_PREV"


private const val PREFS_NAME = "pokemon_widget_prefs"
private const val KEY_PAGE_PREFIX = "page_"
private const val KEY_ID = "pokemon_id"
private const val KEY_NAME = "pokemon_name"
private const val KEY_DESC = "pokemon_description"

private fun getCurrentPage(context: Context, appWidgetId: Int): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_PAGE_PREFIX + appWidgetId, 0)
}

private fun setCurrentPage(context: Context, appWidgetId: Int, page: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_PAGE_PREFIX + appWidgetId, page).apply()
}

private fun savePokemonInfo(
    context: Context,
    id: Int,
    name: String,
    description: String
) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putInt(KEY_ID, id)
        .putString(KEY_NAME, name)
        .putString(KEY_DESC, description)
        .apply()
}

private fun loadPokemonInfo(context: Context): Triple<Int?, String?, String?> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val id = if (prefs.contains(KEY_ID)) prefs.getInt(KEY_ID, 0) else null
    val name = prefs.getString(KEY_NAME, null)
    val desc = prefs.getString(KEY_DESC, null)
    return Triple(id, name, desc)
}

/**
 * App Widget implementation for the daily Pokémon dot-matrix widget.
 */
class DailyPokemonWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        if (action != ACTION_PAGE_NEXT && action != ACTION_PAGE_PREV) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.widget_pokemon)

        val today = LocalDate.now(ZoneId.systemDefault())
        val todayId = PokemonOfDay.getTodayPokemonId()

        // Try memory cache
        val bitmap = if (PokemonCache.isValidFor(today, todayId)) {
            PokemonCache.bitmap
        } else null

        // Load text from prefs as fallback
        val (savedId, savedName, savedDesc) = loadPokemonInfo(context)
        val idToUse = savedId ?: todayId
        val nameToUse = savedName ?: "Unknown"
        val descToUse = savedDesc ?: ""

        // Current + direction
        val current = getCurrentPage(context, appWidgetId)
        val maxPage = 2  // 0,1,2
        val next = when (action) {
            ACTION_PAGE_NEXT -> (current + 1) % (maxPage + 1)
            ACTION_PAGE_PREV -> (current - 1 + (maxPage + 1)) % (maxPage + 1)
            else -> current
        }
        setCurrentPage(context, appWidgetId, next)

        // Now show the right page using the same when(...) you already have:
        when (next) {
            0 -> {
                // Image page
                views.setViewVisibility(R.id.image_pokemon, View.VISIBLE)
                views.setViewVisibility(R.id.name_container, View.GONE)
                views.setViewVisibility(R.id.description_container, View.GONE)

                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.image_pokemon, bitmap)
                }

                views.setTextColor(R.id.page_dot_1, Color.WHITE)
                views.setTextColor(R.id.page_dot_2, 0x55FFFFFF.toInt())
                views.setTextColor(R.id.page_dot_3, 0x55FFFFFF.toInt())
            }
            1 -> {
                // Name + number
                views.setViewVisibility(R.id.image_pokemon, View.GONE)
                views.setViewVisibility(R.id.name_container, View.VISIBLE)
                views.setViewVisibility(R.id.description_container, View.GONE)

                views.setTextViewText(R.id.text_name, nameToUse.uppercase())
                views.setTextViewText(
                    R.id.text_number,
                    "#${idToUse.toString().padStart(3, '0')}"
                )

                views.setTextColor(R.id.page_dot_1, 0x55FFFFFF.toInt())
                views.setTextColor(R.id.page_dot_2, Color.WHITE)
                views.setTextColor(R.id.page_dot_3, 0x55FFFFFF.toInt())
            }
            2 -> {
                // Description
                views.setViewVisibility(R.id.image_pokemon, View.GONE)
                views.setViewVisibility(R.id.name_container, View.GONE)
                views.setViewVisibility(R.id.description_container, View.VISIBLE)

                views.setTextViewText(R.id.text_description, descToUse)

                views.setTextColor(R.id.page_dot_1, 0x55FFFFFF.toInt())
                views.setTextColor(R.id.page_dot_2, 0x55FFFFFF.toInt())
                views.setTextColor(R.id.page_dot_3, Color.WHITE)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
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
 * Updates a single widget instance (full update: does network/cache work).
 */
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.widget_pokemon)

    // NEXT (bottom tap)
    val nextIntent = Intent(context, DailyPokemonWidget::class.java).apply {
        action = ACTION_PAGE_NEXT
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val nextPending = PendingIntent.getBroadcast(
        context,
        appWidgetId * 10 + 1,  // just to keep requestCodes distinct
        nextIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.tap_bottom, nextPending)

// PREV (top tap)
    val prevIntent = Intent(context, DailyPokemonWidget::class.java).apply {
        action = ACTION_PAGE_PREV
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val prevPending = PendingIntent.getBroadcast(
        context,
        appWidgetId * 10 + 2,
        prevIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.tap_top, prevPending)


    GlobalScope.launch(Dispatchers.IO) {
        try {
            val today = LocalDate.now(ZoneId.systemDefault())
            val pokemonId = PokemonOfDay.getTodayPokemonId()

            // Try to reuse cache first
            val cachedOk = PokemonCache.isValidFor(today, pokemonId)

            val (info, dotBitmap) =
                if (cachedOk) {
                    val cachedName = PokemonCache.name ?: "Unknown"
                    val cachedDesc = PokemonCache.description ?: ""
                    val cachedBitmap = PokemonCache.bitmap
                    PokemonInfo(pokemonId, cachedName, cachedDesc) to cachedBitmap
                } else {
                    // Full fetch only when needed (once per day / per process)
                    val sprite = SpriteDownloader.fetchPokemonSprite(pokemonId)
                    val dot = sprite?.let { src ->
                        DotMatrixRenderer.toDotMatrix(
                            src,
                            gridWidth = 50,
                            gridHeight = 50,
                            cellSize = 5
                        )
                    }

                    val infoFetched = PokemonInfoFetcher.getPokemonInfo(pokemonId)
                    val name = infoFetched?.name ?: "Unknown"
                    val desc = infoFetched?.description ?: ""

                    // Store into memory cache
                    PokemonCache.set(today, pokemonId, name, desc, dot)

                    // Also persist text so we have it across restarts
                    savePokemonInfo(context, pokemonId, name, desc)

                    PokemonInfo(pokemonId, name, desc) to dot
                }

            val pokemonInfo = info
            val pokemonName = pokemonInfo.name
            val pokemonDescription = pokemonInfo.description
            val bitmap = dotBitmap

            withContext(Dispatchers.Main) {
                val page = getCurrentPage(context, appWidgetId)

                when (page) {
                    0 -> {
                        // Page 0: image
                        views.setViewVisibility(R.id.image_pokemon, View.VISIBLE)
                        views.setViewVisibility(R.id.name_container, View.GONE)
                        views.setViewVisibility(R.id.description_container, View.GONE)

                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.image_pokemon, bitmap)
                        }

                        views.setTextColor(R.id.page_dot_1, Color.WHITE)
                        views.setTextColor(R.id.page_dot_2, 0x55FFFFFF.toInt())
                        views.setTextColor(R.id.page_dot_3, 0x55FFFFFF.toInt())
                    }
                    1 -> {
                        // Page 1: name + number
                        views.setViewVisibility(R.id.image_pokemon, View.GONE)
                        views.setViewVisibility(R.id.name_container, View.VISIBLE)
                        views.setViewVisibility(R.id.description_container, View.GONE)

                        views.setTextViewText(R.id.text_name, pokemonName.uppercase())
                        views.setTextViewText(
                            R.id.text_number,
                            "#${pokemonInfo.id.toString().padStart(3, '0')}"
                        )

                        views.setTextColor(R.id.page_dot_1, 0x55FFFFFF.toInt())
                        views.setTextColor(R.id.page_dot_2, Color.WHITE)
                        views.setTextColor(R.id.page_dot_3, 0x55FFFFFF.toInt())
                    }
                    2 -> {
                        // Page 2: description
                        views.setViewVisibility(R.id.image_pokemon, View.GONE)
                        views.setViewVisibility(R.id.name_container, View.GONE)
                        views.setViewVisibility(R.id.description_container, View.VISIBLE)

                        views.setTextViewText(R.id.text_description, pokemonDescription)

                        views.setTextColor(R.id.page_dot_1, 0x55FFFFFF.toInt())
                        views.setTextColor(R.id.page_dot_2, 0x55FFFFFF.toInt())
                        views.setTextColor(R.id.page_dot_3, Color.WHITE)
                    }
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




