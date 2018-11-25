/*
 * Copyright 2018 Kevin Hausmann
 *
 * This file is part of Podcatcher Deluxe.
 *
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe.model.types

import com.podcatcher.deluxe.model.tags.RSS
import org.junit.Assert.*
import org.junit.Test
import java.util.*

private class FeedEntityImpl : FeedEntity(null, "") {
    fun parseExplicitTest(value: String): Boolean = super.parseExplicit(value)
    fun parseDateTest(value: String): Date? = super.parseDate(value)
    fun normalizeUrlTest(value: String): String = super.normalizeUrl(value)
}

class FeedEntityTest {

    @Test
    fun normalizeUrl() {
        with(FeedEntityImpl()) {
            assertEquals("", normalizeUrlTest(""))
            assertEquals("nothing-serious", normalizeUrlTest("nothing-serious"))
            assertEquals("htp://mygreatpodcast.Test.com", normalizeUrlTest("htp://mygreatpodcast.Test.com"))

            assertEquals("http://www.npr.org/rss/podcast.php", normalizeUrlTest("http://www.npr.org/rss/podcast.php?"))
            assertEquals("http://www.npr.org/rss/podcast.php?id=510289", normalizeUrlTest("http://www.npr.org/rss/podcast.php?id=510289"))
            assertEquals("http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss", normalizeUrlTest("http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss"))

            assertEquals("http://mygreatpodcast.test.com/?format=rss", normalizeUrlTest("http://mygreatpodcast.Test.com/?format=rss"))
            assertEquals("http://mygreatpodcast.test.com/test?format=rss", normalizeUrlTest("http://mygreatpodcast.Test.com/test?format=rss#foo"))

            assertEquals("http://feeds.feedburner.com/TheTest", normalizeUrlTest(" http://feeds.feedburner.com/TheTest "))
            assertEquals("http://feeds2.feedburner.com/TheTest", normalizeUrlTest("htTP://feeds2.FeedBurner.com:80/TheTest/"))
            assertEquals("http://feeds2.feedburner.com:83/TheTest?format=xml", normalizeUrlTest("htTP://feeds2.FeedBurner.com:83/TheTest?format=xml"))
            assertEquals("https://feeds2.feedburner.com:83/TheTest?format=xml", normalizeUrlTest("htTPs://feeds2.FeedBurner.com:83/TheTest?format=xml"))
            assertEquals("https://feeds2.feedburner.com/TheTest?format=xml", normalizeUrlTest("htTPs://feeds2.FeedBurner.com:443/TheTest?format=xml"))
        }
    }

    @Test
    fun parseExplicit() {
        with(FeedEntityImpl()) {
            assertFalse(parseExplicitTest(""))
            assertFalse(parseExplicitTest("no"))
            assertFalse(parseExplicitTest("unknown"))
            assertTrue(parseExplicitTest(RSS.EXPLICIT_POSITIVE_VALUE))
            assertTrue(parseExplicitTest(" ${RSS.EXPLICIT_POSITIVE_VALUE} "))
            assertTrue(parseExplicitTest(" ${RSS.EXPLICIT_POSITIVE_VALUE.toUpperCase()} "))
            assertFalse(parseExplicitTest(RSS.EXPLICIT))
            assertTrue(parseExplicitTest(" Yes  "))
        }
    }

    @Test
    fun parseDate() {
        with(FeedEntityImpl()) {
            assertNull(parseDateTest(""))
            assertNull(parseDateTest("random string"))

            assertTrue(parseDateTest("Sun, 17 Nov 2013 00:00:00 -0600").okay())
            assertTrue(parseDateTest("Sun, 3 Nov 2013 00:00:00 -0500").okay())
            assertTrue(parseDateTest("Sun, 10 Nov 2013 00:00:00 -0600").okay())
            assertTrue(parseDateTest("18-11-02").okay())
            assertTrue(parseDateTest("2018-11-02").okay())
            assertTrue(parseDateTest("Mon,11 Nov 2013 12:12:12 -0600").okay())
        }
    }

}

private fun Date?.okay(): Boolean {
    return this?.let {
        // No more then 20 years back
        it.after(Date(Date().time - 1000L * 60 * 60 * 24 * 365 * 20)) &&
                // No more then one week into the future
                it.before(Date(Date().time + 1000 * 60 * 60 * 24 * 7))
    } ?: false
}