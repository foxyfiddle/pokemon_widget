package com.example.pokemonmatrixwidget

object PokemonOfDay {

    private const val TOTAL_POKEMON = 1025  // adjust if you want

    fun getTodayPokemonId(): Int {
        // Number of days since Jan 1, 1970 (roughly)
        val daysSinceEpoch = System.currentTimeMillis() / (24L * 60L * 60L * 1000L)

        // Wrap it into the range 1..TOTAL_POKEMON
        val id = (daysSinceEpoch % TOTAL_POKEMON) + 1

        return id.toInt()
    }
}
