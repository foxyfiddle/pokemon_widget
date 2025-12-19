package com.example.pokemonmatrixwidget

import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

object PokemonOfDay {

    private const val MAX_ID = 1025  // highest Pokédex number you want to include

    fun getTodayPokemonId(): Int {
        val today = LocalDate.now(ZoneId.systemDefault())

        // Long: days since 1970-01-01 (e.g., ~20k)
        val daysSinceEpoch = today.toEpochDay()

        // Mix the day index into a "random-looking" seed.
        // Constants are chosen to stay well within Long range.
        val mixed = daysSinceEpoch * 1_103_515_245L + 12_345_678_901L

        // Deterministic RNG seeded by the mixed value
        val random = Random(mixed)

        // Burn a few values to decorrelate nearby days a bit more
        repeat(5) { random.nextInt() }

        // Pick a Pokémon ID between 1 and MAX_ID (inclusive)
        return random.nextInt(1, MAX_ID + 1)
    }
}

