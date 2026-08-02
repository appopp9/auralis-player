package com.auralis.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRuleCodecTest {

    @Test
    fun `round trip preserves every rule field`() {
        val rules = listOf(
            SmartRule(SmartField.ARTIST, SmartOperator.CONTAINS, "محسن چاوشی"),
            SmartRule(SmartField.YEAR, SmartOperator.BETWEEN, "2000", "2010"),
            SmartRule(SmartField.FAVORITE, SmartOperator.IS_TRUE),
            SmartRule(SmartField.LAST_PLAYED, SmartOperator.IN_LAST, "30")
        )

        val decoded = SmartRuleCodec.decode(SmartRuleCodec.encode(rules))

        assertEquals(rules, decoded)
    }

    @Test
    fun `quotes and backslashes survive encoding`() {
        val rules = listOf(SmartRule(SmartField.TITLE, SmartOperator.EQUALS, "say \"hi\" \\ now"))
        assertEquals(rules, SmartRuleCodec.decode(SmartRuleCodec.encode(rules)))
    }

    @Test
    fun `malformed json degrades to an empty rule list instead of crashing`() {
        assertTrue(SmartRuleCodec.decode("not json at all").isEmpty())
        assertTrue(SmartRuleCodec.decode("[{\"field\":").isEmpty())
        assertTrue(SmartRuleCodec.decode(null).isEmpty())
        assertTrue(SmartRuleCodec.decode("").isEmpty())
    }

    @Test
    fun `unknown keys fall back to safe defaults`() {
        assertEquals(SmartField.TITLE, SmartField.from("nope"))
        assertEquals(SmartOperator.CONTAINS, SmartOperator.from(null))
        assertEquals(SmartSort.TITLE, SmartSort.from("nope"))
    }

    @Test
    fun `operators offered always match the field kind`() {
        SmartField.entries.forEach { field ->
            assertTrue("${field.label} has no operators", field.operators.isNotEmpty())
        }
        assertTrue(SmartOperator.IS_TRUE in SmartField.FAVORITE.operators)
        assertTrue(SmartOperator.BETWEEN in SmartField.YEAR.operators)
        assertTrue(SmartOperator.CONTAINS in SmartField.ARTIST.operators)
    }
}
