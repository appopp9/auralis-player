package com.auralis.player.core

/**
 * Lightweight fuzzy matching utility used by [com.auralis.player.data.repository.MusicRepository]
 * for in-library search.
 *
 * Returns a Float in [0f, 1f]; higher = better match. 0f means "no match".
 *
 * Strategy (cheap and good enough for a local library):
 *   - 1.0 exact match (case-insensitive)
 *   - 0.95 substring match (target contains query)
 *   - 0.9 each char of query appears in order in target (subsequence)
 *   - otherwise weighted Levenshtein-based similarity
 */
object Fuzzy {

    fun score(target: String, query: String): Float {
        if (query.isBlank()) return 0f
        val t = target.trim()
        if (t.isEmpty()) return 0f
        val lowerT = t.lowercase()
        val lowerQ = query.trim().lowercase()

        if (lowerT == lowerQ) return 1f
        if (lowerT.contains(lowerQ)) {
            // Earlier substring position = higher score
            val pos = lowerT.indexOf(lowerQ)
            val posFactor = 1f - (pos.toFloat() / (lowerT.length + 1f)) * 0.3f
            return (0.95f * posFactor).coerceIn(0.6f, 0.95f)
        }
        if (isSubsequence(lowerT, lowerQ)) return 0.75f
        val sim = similarity(lowerT, lowerQ)
        return if (sim >= 0.5f) sim * 0.6f else 0f
    }

    /** True when every character of [query] appears in [target] in the same order (not necessarily contiguous). */
    fun matches(target: String, query: String): Boolean = score(target, query) > 0f

    private fun isSubsequence(target: String, query: String): Boolean {
        var i = 0
        for (j in target.indices) {
            if (i >= query.length) return true
            if (target[j] == query[i]) i++
        }
        return i >= query.length
    }

    /** Normalised Levenshtein similarity in [0,1]. */
    private fun similarity(a: String, b: String): Float {
        if (a.isEmpty() && b.isEmpty()) return 1f
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1f
        return 1f - levenshtein(a, b).toFloat() / maxLen
    }

    private fun levenshtein(a: String, b: String): Int {
        val n = a.length
        val m = b.length
        if (n == 0) return m
        if (m == 0) return n

        var prev = IntArray(m + 1) { it }
        var curr = IntArray(m + 1)

        for (i in 1..n) {
            curr[0] = i
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1,           // deletion
                    curr[j - 1] + 1,       // insertion
                    prev[j - 1] + cost     // substitution
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[m]
    }
}
