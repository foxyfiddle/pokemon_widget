package com.example.pokemonmatrixwidget

import android.graphics.Bitmap
import java.time.LocalDate

object PokemonCache {
    var date: LocalDate? = null
    var id: Int? = null
    var name: String? = null
    var description: String? = null
    var bitmap: Bitmap? = null

    fun isValidFor(today: LocalDate, pokemonId: Int): Boolean {
        return date == today && id == pokemonId && bitmap != null && name != null
    }

    fun set(
        today: LocalDate,
        pokemonId: Int,
        pokemonName: String,
        pokemonDescription: String,
        pokemonBitmap: Bitmap?
    ) {
        date = today
        id = pokemonId
        name = pokemonName
        description = pokemonDescription
        bitmap = pokemonBitmap
    }
}
