package com.example.pokemonmatrixwidget

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imageView = findViewById<ImageView>(R.id.imagePokemon)

        // Use the same "daily Pokémon" as the widget
        val pokemonId = PokemonOfDay.getTodayPokemonId()

        lifecycleScope.launch(Dispatchers.IO) {
            // 1) Download the sprite for today's Pokémon
            val sprite = SpriteDownloader.fetchPokemonSprite(pokemonId)

            // 2) Convert it to a dot–matrix bitmap
            val dotBitmap = sprite?.let {
                DotMatrixRenderer.toDotMatrix(
                    it,
                    gridWidth = 50,
                    gridHeight = 50,
                    cellSize = 5
                )
            }

            // 3) Show it in the ImageView on the main thread
            withContext(Dispatchers.Main) {
                if (dotBitmap != null) {
                    imageView.setImageBitmap(dotBitmap)
                }
            }
        }
    }
}

