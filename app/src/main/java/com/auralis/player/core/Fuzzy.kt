package com.auralis.player.core

/**
 * Lightweight fuzzy matcher: exact > prefix > word-prefix > subsequence >
 * edit-distance tolerant > cross-script (Finglish). Returns a score in 0..1
 * (0 = no match).
 *
 * Both sides run through [TextNormalizer] first, so Persian metadata typed with
 * Arabic letters, zero-width joiners or Persian digits still matches a query
 * typed the other way round.
 */
object Fuzzy {

    /**
     * The query is identical for every candidate in a search pass, so its
     * normalized and transliterated forms are computed once per query instead
     * of once per song. This alone removes thousands of string allocations per
     * keystroke on a large library.
     */
    private class PreparedQuery(val raw: String) {
        val normalized: String = TextNormalizer.normalize(raw)
        val skeleton: String =
            TextNormalizer.consonantSkeleton(TextNormalizer.toLatinSkeleton(normalized))
        val tolerance: Int = when {
            normalized.length <= 3 -> 1
            normalized.length <= 6 -> 2
            else -> 3
        }
    }

    @Volatile
    private var lastQuery: PreparedQuery? = null

    private fun prepare(query: String): PreparedQuery {
        val cached = lastQuery
        if (cached != null && cached.raw == query) return cached
        return PreparedQuery(query).also { lastQuery = it }
    }

    fun score(candidate: String, query: String): Float {
        if (query.isBlank()) return 0f
        val prepared = prepare(query)
        val q = prepared.normalized
        if (q.isEmpty()) return 0f
        val c = TextNormalizer.normalize(candidate)
        if (c.isEmpty()) return 0f
        if (c == q) return 1f
        if (c.startsWith(q)) return 0.95f
        if (c.contains(q)) return 0.85f
        if (c.split(' ', '-', '_', '.').any { it.startsWith(q) }) return 0.8f
        if (isSubsequence(c, q)) return 0.65f
        val distance = levenshtein(c.take(q.length + 2), q)
        if (distance <= prepared.tolerance) return 0.6f - distance * 0.08f

        // Last resort: a Latin-keyboard query against Persian metadata
        // ("shadmehr" -> شادمهر). Consonant skeletons only, because Persian
        // does not write short vowels. Cheapest check runs last on purpose.
        val querySkeleton = prepared.skeleton
        if (querySkeleton.isEmpty()) return 0f
        val candidateSkeleton = TextNormalizer.consonantSkeleton(TextNormalizer.toLatinSkeleton(c))
        if (candidateSkeleton.isNotEmpty()) {
            if (candidateSkeleton == querySkeleton) return 0.75f
            if (candidateSkeleton.startsWith(querySkeleton)) return 0.7f
            if (candidateSkeleton.contains(querySkeleton)) return 0.55f
        }
        return 0f
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
