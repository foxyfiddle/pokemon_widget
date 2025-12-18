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

        // 1) Choose a Pokémon ID (25 = Pikachu)
        val pokemonId = 965

        // 2) Run network call on a background thread (IO dispatcher)
        lifecycleScope.launch(Dispatchers.IO) {
            val sprite = SpriteDownloader.fetchPokemonSprite(pokemonId)

            // If we successfully got a sprite, convert it to dot-matrix
            val dotBitmap = sprite?.let {
                DotMatrixRenderer.toDotMatrix(it, gridWidth = 18, gridHeight = 18)
            }

            withContext(Dispatchers.Main) {
                if (dotBitmap != null) {
                    imageView.setImageBitmap(dotBitmap)
                }
            }
        }

    }
}
