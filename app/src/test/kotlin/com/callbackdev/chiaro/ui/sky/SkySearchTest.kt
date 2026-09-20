package com.callbackdev.chiaro.ui.sky

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog's search (Fase 28, widened in 28b).
 *
 * The camera is matched off [com.callbackdev.chiaro.domain.sky.SkyJob.photographic]
 * rather than off a word planted in the nine explanations, so this is where the reader's
 * «foto» is proved to reach them — and where a prefix short enough to match everything
 * is proved not to.
 */
class SkySearchTest {

    private val terms = "foto fotografia fotografare macchina obiettivo scatto"

    private fun match(query: String, photographic: Boolean = false) = matchesSearch(
        query = query,
        name = "Luna piena al crepuscolo",
        explanation = "La sera in cui la luna piena sorge mentre il cielo è ancora colorato.",
        photographic = photographic,
        photoTerms = terms
    )

    @Test
    fun `an empty query keeps every row`() {
        assertTrue(match(""))
        assertTrue(match("   "))
    }

    @Test
    fun `the words the row prints are matched, accents and case aside`() {
        assertTrue(match("luna"))
        assertTrue(match("LUNA"))
        assertTrue(match("crepuscolo"))
        // The explanation counts too: it is the other string the row shows.
        assertTrue(match("colorato"))
        // «è» typed without its accent still finds the line that has one.
        assertTrue(match("e ancora"))
        assertFalse(match("perseidi"))
    }

    @Test
    fun `the camera is found by the flag, not by a word in the prose`() {
        // The explanation above says nothing about photography — on purpose.
        assertFalse("the prose must not carry the word", match("foto", photographic = false))
        assertTrue("a flagged row answers «foto»", match("foto", photographic = true))
        assertTrue("and a prefix of it", match("fot", photographic = true))
        assertTrue(match("macchina", photographic = true))
        assertTrue(match("SCATTO", photographic = true))
    }

    @Test
    fun `a prefix too short to mean anything matches nothing extra`() {
        // «fo» would otherwise pull in every flagged row on two letters, which is the
        // whole catalog's worth of noise for a keystroke.
        assertFalse(match("fo", photographic = true))
        // «ma», the start of «macchina», is two letters and must not reach it either.
        assertFalse(match("ma", photographic = true))
        // And a single letter that IS in the prose still matches the prose, which is
        // the substring rule doing its job and not the camera doing anything: «m» is
        // in «mentre». The short-prefix floor guards the TERMS, not the text.
        assertTrue(match("m", photographic = true))
        assertTrue(match("m", photographic = false))
    }

    @Test
    fun `a word that is not one of the terms does not reach a flagged row`() {
        assertFalse(match("pioggia", photographic = true))
    }
}
