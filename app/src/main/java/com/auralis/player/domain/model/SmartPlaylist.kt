package com.auralis.player.domain.model

/**
 * Smart playlist model.
 *
 * Rules are persisted as a JSON string in a single column, so adding an
 * operator or a field never requires a schema migration.
 */

/** A field a rule can test. */
enum class SmartField(val key: String, val label: String, val kind: SmartKind) {
    TITLE("title", "Title", SmartKind.TEXT),
    ARTIST("artist", "Artist", SmartKind.TEXT),
    ALBUM("album", "Album", SmartKind.TEXT),
    ALBUM_ARTIST("albumArtist", "Album artist", SmartKind.TEXT),
    GENRE("genre", "Genre", SmartKind.TEXT),
    COMPOSER("composer", "Composer", SmartKind.TEXT),
    FOLDER("folder", "Folder", SmartKind.TEXT),
    MOOD("mood", "Mood", SmartKind.TEXT),
    YEAR("year", "Year", SmartKind.NUMBER),
    DURATION("duration", "Duration (sec)", SmartKind.NUMBER),
    PLAY_COUNT("playCount", "Play count", SmartKind.NUMBER),
    LAST_PLAYED("lastPlayed", "Last played", SmartKind.DATE),
    DATE_ADDED("dateAdded", "Date added", SmartKind.DATE),
    FAVORITE("favorite", "Favorite", SmartKind.BOOLEAN),
    HAS_LYRICS("hasLyrics", "Has lyrics", SmartKind.BOOLEAN);

    val isText: Boolean get() = kind == SmartKind.TEXT
    val isNumber: Boolean get() = kind == SmartKind.NUMBER
    val isDate: Boolean get() = kind == SmartKind.DATE
    val isBoolean: Boolean get() = kind == SmartKind.BOOLEAN

    /** Operators that make sense for this field. */
    val operators: List<SmartOperator> get() = SmartOperator.forKind(kind)

    companion object {
        fun from(key: String?): SmartField =
            entries.firstOrNull { it.key == key } ?: TITLE
    }
}

enum class SmartKind { TEXT, NUMBER, DATE, BOOLEAN }

enum class SmartOperator(val key: String, val label: String) {
    CONTAINS("contains", "contains"),
    NOT_CONTAINS("notContains", "does not contain"),
    EQUALS("equals", "is"),
    NOT_EQUALS("notEquals", "is not"),
    STARTS_WITH("startsWith", "starts with"),
    ENDS_WITH("endsWith", "ends with"),
    GREATER("greater", "greater than"),
    LESS("less", "less than"),
    BETWEEN("between", "between"),
    IN_LAST("inLast", "in the last (days)"),
    NOT_IN_LAST("notInLast", "not in the last (days)"),
    IS_TRUE("isTrue", "is yes"),
    IS_FALSE("isFalse", "is no");

    companion object {
        fun from(key: String?): SmartOperator =
            entries.firstOrNull { it.key == key } ?: CONTAINS

        fun forKind(kind: SmartKind): List<SmartOperator> = when (kind) {
            SmartKind.TEXT -> listOf(CONTAINS, NOT_CONTAINS, EQUALS, NOT_EQUALS, STARTS_WITH, ENDS_WITH)
            SmartKind.NUMBER -> listOf(EQUALS, NOT_EQUALS, GREATER, LESS, BETWEEN)
            SmartKind.DATE -> listOf(IN_LAST, NOT_IN_LAST)
            SmartKind.BOOLEAN -> listOf(IS_TRUE, IS_FALSE)
        }
    }
}

/**
 * One condition. [value] is always kept as text: it is what the user typed,
 * and the evaluator parses it per field type so bad input can be discarded
 * instead of silently matching nothing.
 */
data class SmartRule(
    val field: SmartField = SmartField.TITLE,
    val operator: SmartOperator = SmartOperator.CONTAINS,
    val value: String = "",
    val valueTo: String = ""
)

enum class SmartSort(val key: String, val label: String) {
    TITLE("title", "Title"),
    ARTIST("artist", "Artist"),
    ALBUM("album", "Album"),
    YEAR("year", "Year"),
    DURATION("duration", "Duration"),
    PLAY_COUNT("playCount", "Play count"),
    LAST_PLAYED("lastPlayed", "Last played"),
    DATE_ADDED("dateAdded", "Date added"),
    RANDOM("random", "Random");

    companion object {
        fun from(key: String?): SmartSort =
            entries.firstOrNull { it.key == key } ?: TITLE
    }
}

data class SmartPlaylist(
    val id: Long = 0L,
    val name: String = "",
    val icon: String = "auto",
    val rules: List<SmartRule> = emptyList(),
    val matchAll: Boolean = true,
    /** 0 = unlimited. */
    val limit: Int = 0,
    val sort: SmartSort = SmartSort.TITLE,
    val sortDescending: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * Minimal, dependency-free JSON codec for the rule list.
 *
 * Hand-rolled on purpose: the project has no JSON library on the classpath,
 * `org.json` is unavailable to plain JVM unit tests, and the shape here is
 * fixed (an array of flat string objects), so a full parser would be overkill.
 * Anything unparseable degrades to an empty rule list rather than throwing,
 * because a corrupted row must never crash the library screen.
 */
object SmartRuleCodec {

    fun encode(rules: List<SmartRule>): String {
        val sb = StringBuilder("[")
        rules.forEachIndexed { index, rule ->
            if (index > 0) sb.append(',')
            sb.append('{')
            sb.append("\"field\":").append(quote(rule.field.key)).append(',')
            sb.append("\"op\":").append(quote(rule.operator.key)).append(',')
            sb.append("\"value\":").append(quote(rule.value)).append(',')
            sb.append("\"valueTo\":").append(quote(rule.valueTo))
            sb.append('}')
        }
        return sb.append(']').toString()
    }

    fun decode(json: String?): List<SmartRule> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { parse(json) }.getOrDefault(emptyList())
    }

    private fun parse(json: String): List<SmartRule> {
        val rules = mutableListOf<SmartRule>()
        var i = 0
        fun skipWhitespace() {
            while (i < json.length && json[i].isWhitespace()) i++
        }
        fun readString(): String {
            skipWhitespace()
            if (i >= json.length || json[i] != '"') return ""
            i++
            val sb = StringBuilder()
            while (i < json.length && json[i] != '"') {
                val ch = json[i]
                if (ch == '\\' && i + 1 < json.length) {
                    i++
                    when (val esc = json[i]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            // A truncated or malformed \u escape used to throw,
                            // and the whole rule set was then silently dropped.
                            val end = minOf(i + 5, json.length)
                            val hex = json.substring(i + 1, end)
                            val code = if (hex.length == 4) hex.toIntOrNull(16) else null
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            } else {
                                sb.append(hex)
                                i += hex.length
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(ch)
                }
                i++
            }
            i++ // closing quote
            return sb.toString()
        }

        skipWhitespace()
        if (i < json.length && json[i] == '[') i++
        while (i < json.length) {
            skipWhitespace()
            if (i >= json.length || json[i] == ']') break
            if (json[i] == ',') {
                i++
                continue
            }
            if (json[i] != '{') {
                i++
                continue
            }
            i++ // opening brace
            var field = ""
            var op = ""
            var value = ""
            var valueTo = ""
            while (i < json.length && json[i] != '}') {
                skipWhitespace()
                if (i < json.length && (json[i] == ',' || json[i] == ':')) {
                    i++
                    continue
                }
                if (i >= json.length || json[i] == '}') break
                val key = readString()
                skipWhitespace()
                if (i < json.length && json[i] == ':') i++
                val raw = readString()
                when (key) {
                    "field" -> field = raw
                    "op" -> op = raw
                    "value" -> value = raw
                    "valueTo" -> valueTo = raw
                }
                skipWhitespace()
            }
            i++ // closing brace
            rules += SmartRule(
                field = SmartField.from(field),
                operator = SmartOperator.from(op),
                value = value,
                valueTo = valueTo
            )
        }
        return rules
    }

    private fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') {
                    sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(ch)
                }
            }
        }
        return sb.append('"').toString()
    }
}
