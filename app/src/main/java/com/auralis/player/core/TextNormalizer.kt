package com.auralis.player.core

/**
 * Text folding for search and rule matching.
 *
 * Persian text in music metadata is wildly inconsistent: tags are typed with
 * Arabic ي and ك instead of Persian ی and ک, with or without zero-width
 * non-joiners, with Arabic-Indic digits, and with optional diacritics. Two
 * spellings of the same artist name therefore compare as different strings.
 * Everything here collapses those variants onto one canonical form so a single
 * query finds them all.
 */
object TextNormalizer {

    /** Characters that carry no meaning for matching and are simply dropped. */
    private val STRIPPED = setOf(
        '\u200C', // zero-width non-joiner
        '\u200D', // zero-width joiner
        '\u200E', // left-to-right mark
        '\u200F', // right-to-left mark
        '\u0640', // tatweel / kashida
        '\u064B', '\u064C', '\u064D', // fathatan, dammatan, kasratan
        '\u064E', '\u064F', '\u0650', // fatha, damma, kasra
        '\u0651', '\u0652', '\u0653', // shadda, sukun, maddah
        '\u0654', '\u0655', '\u0670'
    )

    /**
     * Small bounded memo. Search re-normalizes the *same* library strings on
     * every keystroke, so without this the whole library is folded character by
     * character 4× per song per typed letter. Access-ordered, so hot strings
     * (the visible library) stay resident and the rest evicts.
     */
    private const val CACHE_LIMIT = 4096

    private val normalizeCache = object : LinkedHashMap<String, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_LIMIT
    }

    private val skeletonCache = object : LinkedHashMap<String, String>(512, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > CACHE_LIMIT
    }

    /**
     * Canonical form: unified letters, no diacritics, Latin digits, lower case,
     * collapsed whitespace.
     *
     * Deliberately allocation-light: one pass, no regular expressions and no
     * intermediate strings, because this runs on every candidate of every
     * keystroke.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        synchronized(normalizeCache) { normalizeCache[input] }?.let { return it }

        val sb = StringBuilder(input.length)
        var pendingSpace = false
        for (raw in input) {
            if (raw in STRIPPED) continue
            val ch = fold(raw)
            if (ch.isWhitespace()) {
                // Leading whitespace is dropped, inner runs collapse to one
                // space, trailing whitespace never gets flushed.
                if (sb.isNotEmpty()) pendingSpace = true
                continue
            }
            if (pendingSpace) {
                sb.append(' ')
                pendingSpace = false
            }
            sb.append(ch.lowercaseChar())
        }
        val result = sb.toString()
        synchronized(normalizeCache) { normalizeCache[input] = result }
        return result
    }

    private fun fold(ch: Char): Char = when (ch) {
        // Alef family -> bare alef
        '\u0622', '\u0623', '\u0625', '\u0671' -> '\u0627'
        // Arabic yeh / alef maksura -> Persian yeh
        '\u064A', '\u0649', '\u06CD' -> '\u06CC'
        // Arabic kaf variants -> Persian kaf
        '\u0643', '\u06AA' -> '\u06A9'
        // Teh marbuta -> heh
        '\u0629' -> '\u0647'
        // Heh variants -> heh
        '\u06C0', '\u06D5' -> '\u0647'
        // Waw variants -> waw
        '\u0624', '\u06C4', '\u06C5', '\u06C6', '\u06C7', '\u06C8' -> '\u0648'
        // Persian and Arabic-Indic digits -> ASCII
        in '\u06F0'..'\u06F9' -> ('0' + (ch - '\u06F0'))
        in '\u0660'..'\u0669' -> ('0' + (ch - '\u0660'))
        // Arabic punctuation -> ASCII equivalents
        '\u060C' -> ','
        '\u061B' -> ';'
        '\u061F' -> '?'
        else -> ch
    }

    /**
     * Rough Persian-to-Latin transliteration, used only to let a Latin-keyboard
     * query match Persian metadata ("shadmehr" finding شادمهر).
     *
     * This is intentionally lossy: short vowels are not written in Persian at
     * all, so an exact transliteration is impossible. Consonants are what carry
     * the signal, so vowels are dropped from both sides before comparing and the
     * result is only ever used for fuzzy candidate matching, never for display.
     */
    fun toLatinSkeleton(input: String?): String {
        val normalized = normalize(input)
        if (normalized.isEmpty()) return ""
        synchronized(skeletonCache) { skeletonCache[normalized] }?.let { return it }
        val sb = StringBuilder(normalized.length + 8)
        for (ch in normalized) {
            when (ch) {
                '\u0627' -> {}                    // alef: vowel carrier, dropped
                '\u0628' -> sb.append('b')        // be
                '\u067E' -> sb.append('p')        // pe
                '\u062A', '\u0637' -> sb.append('t')   // te, ta
                '\u062B', '\u0633', '\u0635' -> sb.append('s') // se, sin, sad
                '\u062C' -> sb.append('j')        // jim
                '\u0686' -> sb.append("ch")       // che
                '\u062D', '\u0647' -> sb.append('h')   // he jimi, he
                '\u062E' -> sb.append("kh")       // khe
                '\u062F' -> sb.append('d')        // dal
                '\u0630', '\u0632', '\u0636', '\u0638' -> sb.append('z')
                '\u0631' -> sb.append('r')        // re
                '\u0698' -> sb.append('j')        // zhe
                '\u0634' -> sb.append("sh")       // shin
                '\u0639', '\u0621', '\u0626' -> {}     // eyn / hamza
                '\u063A', '\u0642' -> sb.append('q')   // ghain, qaf
                '\u0641' -> sb.append('f')        // fe
                '\u06A9' -> sb.append('k')        // kaf
                '\u06AF' -> sb.append('g')        // gaf
                '\u0644' -> sb.append('l')        // lam
                '\u0645' -> sb.append('m')        // mim
                '\u0646' -> sb.append('n')        // nun
                '\u0648' -> sb.append('v')        // vav
                '\u06CC' -> sb.append('y')        // ye
                else -> if (ch.isLetterOrDigit() || ch == ' ') sb.append(ch)
            }
        }
        val result = sb.toString().replace(WHITESPACE, " ").trim()
        synchronized(skeletonCache) { skeletonCache[normalized] = result }
        return result
    }

    private val WHITESPACE = Regex("\\s+")

    /** Latin vowels removed, so "shadmehr" and "shdmhr" compare equal. */
    fun consonantSkeleton(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (raw in input) {
            val ch = raw.lowercaseChar()
            if (ch == 'a' || ch == 'e' || ch == 'i' || ch == 'o' || ch == 'u') continue
            if (ch == '\'' || ch == '’' || ch.isWhitespace()) continue
            sb.append(ch)
        }
        return sb.toString()
    }

    /**
     * True when [query] plausibly refers to [candidate], across scripts.
     * Cheap checks run first so the common case never pays for transliteration.
     */
    fun looseMatch(candidate: String?, query: String?): Boolean {
        val c = normalize(candidate)
        val q = normalize(query)
        if (q.isEmpty()) return true
        if (c.contains(q)) return true
        val cSkeleton = consonantSkeleton(toLatinSkeleton(c))
        val qSkeleton = consonantSkeleton(toLatinSkeleton(q))
        if (qSkeleton.isEmpty() || cSkeleton.isEmpty()) return false
        return cSkeleton.contains(qSkeleton)
    }
}
