package com.auralis.player.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `arabic yeh and kaf fold to persian forms`() {
        // ي / ك (Arabic) must normalize to the same skeleton as ی / ک (Persian)
        assertEquals(
            TextNormalizer.normalize("كتابي"),
            TextNormalizer.normalize("کتابی")
        )
    }

    @Test
    fun `persian and arabic digits fold to latin`() {
        assertEquals("1234", TextNormalizer.normalize("۱۲۳۴"))
        assertEquals("1234", TextNormalizer.normalize("١٢٣٤"))
    }

    @Test
    fun `diacritics and zero width joiners are stripped`() {
        val withMarks = "مَحمَّد"
        assertEquals(TextNormalizer.normalize("محمد"), TextNormalizer.normalize(withMarks))
        assertEquals(
            TextNormalizer.normalize("میخواهم"),
            TextNormalizer.normalize("می\u200cخواهم")
        )
    }

    @Test
    fun `whitespace collapses and case folds`() {
        assertEquals("hello world", TextNormalizer.normalize("  HELLO   World "))
    }

    @Test
    fun `null and blank input are safe`() {
        assertEquals("", TextNormalizer.normalize(null))
        assertEquals("", TextNormalizer.normalize("   "))
    }

    @Test
    fun `loose match survives spacing and script differences`() {
        assertTrue(TextNormalizer.looseMatch("محسن یگانه", "محسنيگانه"))
    }

    @Test
    fun `consonant skeleton drops vowels`() {
        assertEquals(
            TextNormalizer.consonantSkeleton("shadmehr"),
            TextNormalizer.consonantSkeleton("shadmehr")
        )
        assertTrue(TextNormalizer.consonantSkeleton("aeiou").isEmpty())
    }
}
