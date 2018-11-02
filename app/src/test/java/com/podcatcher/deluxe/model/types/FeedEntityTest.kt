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
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.util.*

private class FeedEntityImpl : FeedEntity(null, "") {
    fun parseExplicitTest(value: String): Boolean = super.parseExplicit(value)
    fun parseDateTest(value: String): Date? = super.parseDate(value)
    fun normalizeUrlTest(value: String): String = super.normalizeUrl(value)
}

class FeedEntityTest : StringSpec({
    "Normalize URL" {
        with(FeedEntityImpl()) {
            normalizeUrlTest("") shouldBe ""
            normalizeUrlTest("nothing-serious") shouldBe "nothing-serious"
            normalizeUrlTest("htp://mygreatpodcast.Test.com") shouldBe "htp://mygreatpodcast.Test.com"

            normalizeUrlTest("http://www.npr.org/rss/podcast.php?id=510289") shouldBe "http://www.npr.org/rss/podcast.php?id=510289"
            normalizeUrlTest("http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss") shouldBe "http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss"

            normalizeUrlTest("http://mygreatpodcast.Test.com/?format=rss") shouldBe "http://mygreatpodcast.test.com/?format=rss"
            normalizeUrlTest("http://mygreatpodcast.Test.com/test?format=rss#foo") shouldBe "http://mygreatpodcast.test.com/test?format=rss"

            normalizeUrlTest(" http://feeds.feedburner.com/TheTest ") shouldBe "http://feeds.feedburner.com/TheTest"
            normalizeUrlTest("htTP://feeds2.FeedBurner.com:80/TheTest/") shouldBe "http://feeds2.feedburner.com/TheTest"
            normalizeUrlTest("htTP://feeds2.FeedBurner.com:83/TheTest?format=xml") shouldBe "http://feeds2.feedburner.com:83/TheTest?format=xml"
            normalizeUrlTest("htTPs://feeds2.FeedBurner.com:83/TheTest?format=xml") shouldBe "https://feeds2.feedburner.com:83/TheTest?format=xml"
            normalizeUrlTest("htTPs://feeds2.FeedBurner.com:443/TheTest?format=xml") shouldBe "https://feeds2.feedburner.com/TheTest?format=xml"
        }
    }

    "Parse tag explicit" {
        with(FeedEntityImpl()) {
            parseExplicitTest("") shouldBe false
            parseExplicitTest("no") shouldBe false
            parseExplicitTest("unknown") shouldBe false
            parseExplicitTest(RSS.EXPLICIT_POSITIVE_VALUE) shouldBe true
            parseExplicitTest(" ${RSS.EXPLICIT_POSITIVE_VALUE} ") shouldBe true
            parseExplicitTest(" ${RSS.EXPLICIT_POSITIVE_VALUE.toUpperCase()} ") shouldBe true
            parseExplicitTest(RSS.EXPLICIT) shouldBe false
            parseExplicitTest(" Yes  ") shouldBe true
        }
    }

    "Parse date" {
        with(FeedEntityImpl()) {
            parseDateTest("") shouldBe null
            parseDateTest("random string") shouldBe null

            parseDateTest("Sun, 17 Nov 2013 00:00:00 -0600").okay() shouldBe true
            parseDateTest("Sun, 3 Nov 2013 00:00:00 -0500").okay() shouldBe true
            parseDateTest("Sun, 10 Nov 2013 00:00:00 -0600").okay() shouldBe true
            parseDateTest("18-11-02").okay() shouldBe true
            parseDateTest("2018-11-02").okay() shouldBe true
            parseDateTest("Mon,11 Nov 2013 12:12:12 -0600").okay() shouldBe true
        }
    }
})

private fun Date?.okay(): Boolean {
    return this?.let {
        // No more then 10 years back
        it.after(Date(Date().time - 1000L * 60 * 60 * 24 * 365 * 20)) &&
                // No more then one week into the future
                it.before(Date(Date().time + 1000 * 60 * 60 * 24 * 7))
    } ?: false
}