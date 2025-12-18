package com.example.pokemonmatrixwidget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.OkHttpClient
import okhttp3.Request

object SpriteDownloader {

    private val client = OkHttpClient()

    // Returns a Bitmap for the given Pokémon ID, or null if it fails
    fun fetchPokemonSprite(id: Int): Bitmap? {
        // Direct sprite URL, no JSON parsing yet
        val url =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
