package com.memoriabox.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateFormatTest {
    @Test
    fun normalizesVersionPrefix() {
        assertEquals("3.5.0", UpdateFormat.normalizeVersion("v3.5.0"))
    }

    @Test
    fun comparesSemanticVersions() {
        assertTrue(UpdateFormat.isNewer("3.5.0", "3.4.9"))
        assertTrue(UpdateFormat.isNewer("v4.0", "3.99.99"))
        assertFalse(UpdateFormat.isNewer("3.4.0", "3.4.0"))
        assertFalse(UpdateFormat.isNewer("3.3.9", "3.4.0"))
    }

    @Test
    fun parsesSha256File() {
        val hash = "a".repeat(64)
        assertEquals(hash, UpdateFormat.parseSha256("$hash  app-release.apk\n"))
        assertNull(UpdateFormat.parseSha256("invalid"))
    }

    @Test
    fun buildsMirrorsWithTheOriginalUrlFirst() {
        val originalUrl = "https://github.com/org/repo/releases/download/v1/app.apk"
        val urls = UpdateFormat.mirrorUrls(originalUrl)

        assertEquals(originalUrl, urls.first())
        assertTrue(urls.size > 1)
        assertTrue(urls.all { it.endsWith("/org/repo/releases/download/v1/app.apk") })
        assertEquals(urls.size, urls.distinct().size)
    }
}
