package com.example.pokemonmatrixwidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PokemonInfo(
    val id: Int,
    val name: String,
    val description: String
)

object PokemonInfoFetcher {

    fun getPokemonInfo(id: Int): PokemonInfo? {
        // 1) Basic Pokémon data (name, etc.)
        val pokemonJson = httpGet("https://pokeapi.co/api/v2/pokemon/$id") ?: return null

        val pokemonObj = JSONObject(pokemonJson)
        val rawName = pokemonObj.getString("name")
        val name = rawName.replaceFirstChar { it.uppercase() }  // "pikachu" -> "Pikachu"

        // 2) Species data (flavor text / description)
        val speciesJson = httpGet("https://pokeapi.co/api/v2/pokemon-species/$id") ?: return null
        val speciesObj = JSONObject(speciesJson)
        val flavorEntries = speciesObj.getJSONArray("flavor_text_entries")

        var description = ""
        for (i in 0 until flavorEntries.length()) {
            val entry = flavorEntries.getJSONObject(i)
            val language = entry.getJSONObject("language").getString("name")
            if (language == "en") {
                description = entry.getString("flavor_text")
                    .replace('\n', ' ')
                    .replace('\u000c', ' ')
                break
            }
        }

        return PokemonInfo(
            id = id,
            name = name,
            description = description
        )
    }

    private fun httpGet(urlString: String): String? {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
