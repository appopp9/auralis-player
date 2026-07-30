package com.auralis.player.core

import java.util.Locale

/**
 * Lightweight fuzzy matcher: exact > prefix > word-prefix > subsequence >
 * edit-distance tolerant. Returns a score in 0..1 (0 = no match).
 */
object Fuzzy {

    fun score(candidate: String, query: String): Float {
        if (query.isBlank()) return 0f
        val c = candidate.lowercase(Locale.ROOT).trim()
        val q = query.lowercase(Locale.ROOT).trim()
        if (c.isEmpty()) return 0f
        if (c == q) return 1f
        if (c.startsWith(q)) return 0.95f
        if (c.contains(q)) return 0.85f
        if (c.split(' ', '-', '_', '.').any { it.startsWith(q) }) return 0.8f
        if (isSubsequence(c, q)) return 0.65f
        val distance = levenshtein(c.take(q.length + 2), q)
        val tolerance = when {
            q.length <= 3 -> 1
            q.length <= 6 -> 2
            else -> 3
        }
        return if (distance <= tolerance) 0.6f - distance * 0.08f else 0f
    }

    fun matches(candidate: String, query: String): Boolean = score(candidate, query) > 0f

    private fun isSubsequence(text: String, query: String): Boolean {
        var index = 0
        for (ch in text) {
            if (index < query.length && ch == query[index]) index++
        }
        return index == query.length
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
