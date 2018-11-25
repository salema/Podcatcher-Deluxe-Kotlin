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

import android.util.Base64
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.*
import org.junit.Test
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.util.*

class PodcastTest {

    @Test
    fun init() {
        with(Podcast(null, "")) {
            assertNull(name)
            assertEquals("", url)
            assertEquals("", feed)
            assertEquals("", guid)
        }
        with(Podcast(" This American Life ", "hTTp://feeds.thisAmericanlife.org/talpodcast")) {
            assertEquals("This American Life", name)
            assertEquals("http://feeds.thisamericanlife.org/talpodcast", url)
            assertEquals("http://feeds.thisamericanlife.org/talpodcast", feed)
        }
    }

    @Test
    fun testHashCode() {
        assertTrue(Podcast(null, "").hashCode() != 0)
    }

    @Test
    fun testCompareTo() {
        assertTrue(Podcast(null, "").compareTo(Podcast(null, "")) == 0)
    }

    @Test
    fun testGetName() {
        var name: String? = null
        var podcast = Podcast(name, "")
        assertEquals(name, podcast.name)

        name = ""
        podcast = Podcast(name, "")
        assertEquals(name, podcast.name)

        name = "Test"
        podcast = Podcast(name, "")
        assertEquals(name, podcast.name)

        val tal = Podcast(null, "http://feeds.thisamericanlife.org/talpodcast")
        assertNull(tal.name)
        tal.loadAndParse()
        assertTrue(tal.name!!.length > 10)

        val tal2 = Podcast("", "http://feeds.thisamericanlife.org/talpodcast")
        tal2.loadAndParse()
        assertEquals(tal2.name, tal.name)

        val tal3 = Podcast("    ", "http://feeds.thisamericanlife.org/talpodcast")
        tal3.loadAndParse()
        assertEquals(tal3.name, tal.name)
    }

    @Test
    fun testToString() {
        val name: String? = null
        val podcast = Podcast(name, "")
        assertNotNull(podcast.toString())
    }

    @Test
    fun testGetLogoUrl() {
        assertNull(Podcast(null, "").logoUrl)

        val tal = Podcast("TAL", "http://feeds.thisamericanlife.org/talpodcast")
        assertNull(tal.logoUrl)
        tal.loadAndParse()
        assertNotNull(tal.logoUrl)
    }

    @Test
    fun testLastLoaded() {
        assertNull(Podcast(null, "").lastSuccessfulLoad)

        val tal = Podcast("TAL", "http://feeds.thisamericanlife.org/talpodcast")
        assertNull(tal.lastSuccessfulLoad)
        tal.loadAndParse()
        assertNotNull(tal.lastSuccessfulLoad)
    }

    @Test
    fun testIsExplicit() {
        assertFalse(Podcast(null, "").explicit)

        val explicit = Podcast("NoSleep", "http://nosleeppodcast.libsyn.com/rss")
        assertFalse(explicit.explicit)
        explicit.loadAndParse()
        assertTrue(explicit.explicit)

        val clean = Podcast("SN", "https://feeds.twit.tv/sn.xml")
        assertFalse(clean.explicit)
        clean.loadAndParse()
        assertFalse(clean.explicit)
    }

    fun Podcast.loadAndParse() = runBlocking {
        launch {
            val client = OkHttpClient()
            val request = Request.Builder().url(feed).build()
            val response = client.newCall(request).execute()
            val feedContent = response.body()?.bytes()

            if (feedContent != null) {
                val parserFactory = XmlPullParserFactory.newInstance()
                parserFactory.isNamespaceAware = true

                val parser = parserFactory.newPullParser()
                parser.setInput(ByteArrayInputStream(feedContent), null)
                parse(parser)

                fileSize = feedContent.size.toLong()
                lastSuccessfulLoad = Date()
                status = Podcast.Status.READY
            }
        }
    }

    @Test
    fun testGetAuth() {
        val podcast = Podcast(null, "")
        assertNull(podcast.authorization)
        podcast.username = "kevin"
        assertNull(podcast.authorization)

        podcast.username = null
        podcast.password = "monkey"
        assertNull(podcast.authorization)

        podcast.username = "kevin"
        assertNotNull(podcast.authorization)
        assertEquals(podcast.authorization,
                "Basic " + Base64.encodeToString("kevin:monkey".toByteArray(), Base64.NO_WRAP)
        )
    }

    @Test
    fun testNormalizeUrl() {
        assertEquals("", Podcast(null, "").url)
        assertEquals("nothing-serious", Podcast(null, "nothing-serious").url)
        assertEquals("htp://mygreatpodcast.Test.com", Podcast(null, "htp://mygreatpodcast.Test.com").url)
        assertEquals("http://www.npr.org/rss/podcast.php?id=510289",
                Podcast(null, "http://www.npr.org/rss/podcast.php?id=510289").url)
        assertEquals("http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss",
                Podcast(null, "http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss").url)

        assertEquals("http://mygreatpodcast.test.com/",
                Podcast(null, "http://mygreatpodcast.Test.com").url)
        assertEquals("http://mygreatpodcast.test.com/",
                Podcast(null, "feed://mygreatpodcast.Test.com").url)
        assertEquals("http://mygreatpodcast.test.com/",
                Podcast(null, "itPC://mygreatpodcast.Test.com").url)
        assertEquals("http://mygreatpodcast.test.com/",
                Podcast(null, "Itms://mygreatpodcast.Test.com").url)
        assertEquals("http://mygreatpodcast.test.com/?format=rss",
                Podcast(null, "http://mygreatpodcast.Test.com/?format=rss").url)
        assertEquals("http://mygreatpodcast.test.com/test?format=rss",
                Podcast(null, "http://mygreatpodcast.Test.com/test?format=rss#foo").url)
        assertEquals("http://feeds.feedburner.com/TheTest",
                Podcast(null, " http://feeds.feedburner.com/TheTest ").url)
        assertEquals("http://feeds.feedburner.com/TheTest/",
                Podcast(null, "htTP://feeds2.FeedBurner.com:80/TheTest/").url)
        assertEquals("http://feeds.feedburner.com:83/TheTest",
                Podcast(null, "htTP://feeds2.FeedBurner.com:83/TheTest?format=xml").url)
        assertEquals("https://feeds.feedburner.com:83/TheTest",
                Podcast(null, "htTPs://feeds2.FeedBurner.com:83/TheTest?format=xml").url)
        assertEquals("https://feeds.feedburner.com/TheTest",
                Podcast(null, "htTPs://feeds2.FeedBurner.com:443/TheTest?format=xml").url)

        assertEquals("http://feeds.feedburner.com/TestPodcast",
                Podcast(null, "FB:TestPodcast?format=xml").url)

        var test = Podcast(null, "http://kevin@feeds.feedburner.com/TestPodcast")
        assertEquals("kevin", test.username)
        assertNull(test.password)
        test = Podcast(null, "http://kevin:@feeds.feedburner.com/TestPodcast")
        assertEquals("kevin", test.username)
        assertNull(test.password)
        test = Podcast(null, "http://feeds.feedburner.com/TestPodcast")
        assertNull(test.username)
        assertNull(test.password)
        val test2 = Podcast(null, "http://kevin:test@feeds.feedburner.com/TestPodcast")
        assertEquals("kevin", test2.username)
        assertEquals("test", test2.password)
        assertEquals(test, test2)
    }

    @Test
    fun testCreateGuid() {
        assertEquals("", Podcast(null, "").guid)
        assertEquals("nothing-serious", Podcast(null, "nothing-serious").guid)
        assertEquals("htp://mygreatpodcast.Test.com", Podcast(null, "htp://mygreatpodcast.Test.com").guid)
        assertEquals("npr.org/rss/podcast.php?id=510289",
                Podcast(null, "http://www.npr.org/rss/podcast.php?id=510289").guid)
        assertEquals("rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss",
                Podcast(null, "http://www.rts.ch/la-1ere/programmes/on-en-parle/podcast/?flux=rss").guid)

        assertEquals("mygreatpodcast.test.com",
                Podcast(null, "http://mygreatpodcast.Test.com").guid)
        assertEquals("mygreatpodcast.test.com",
                Podcast(null, "feed://mygreatpodcast.Test.com").guid)
        assertEquals("mygreatpodcast.test.com",
                Podcast(null, "itPC://mygreatpodcast.Test.com").guid)
        assertEquals("mygreatpodcast.test.com",
                Podcast(null, "Itms://mygreatpodcast.Test.com").guid)
        assertEquals("mygreatpodcast.test.com/?format=rss",
                Podcast(null, "http://mygreatpodcast.Test.com/?format=rss").guid)
        assertEquals("mygreatpodcast.test.com/test?format=rss",
                Podcast(null, "http://mygreatpodcast.Test.com/test?format=rss#foo").guid)
        assertEquals("feeds.feedburner.com/TheTest",
                Podcast(null, " http://feeds.feedburner.com/TheTest ").guid)
        assertEquals("feeds.feedburner.com/TheTest",
                Podcast(null, "htTP://feeds2.FeedBurner.com:80/TheTest/").guid)
        assertEquals("feeds.feedburner.com:83/TheTest",
                Podcast(null, "htTP://feeds2.FeedBurner.com:83/TheTest?format=xml").guid)
        assertEquals("feeds.feedburner.com:83/TheTest",
                Podcast(null, "htTPs://feeds2.FeedBurner.com:83/TheTest?format=xml").guid)
        assertEquals("feeds.feedburner.com/TheTest",
                Podcast(null, "htTPs://feeds2.FeedBurner.com:443/TheTest?format=xml").guid)

        assertEquals("feeds.feedburner.com/TestPodcast",
                Podcast(null, "FB:TestPodcast?format=xml").guid)

        assertEquals("feeds.feedburner.com/TestPodcast",
                Podcast(null, "http://kevin@feeds.feedburner.com/TestPodcast").guid)
        assertEquals("feeds.feedburner.com/TestPodcast",
                Podcast(null, "http://kevin:@feeds.feedburner.com/TestPodcast").guid)
        assertEquals("feeds.feedburner.com/TestPodcast",
                Podcast(null, "http://feeds.feedburner.com/TestPodcast/").guid)
        assertEquals("feeds.feedburner.com/TestPodcast",
                Podcast(null, "http://kevin:test@feeds.feedburner.com/TestPodcast").guid)
    }

    @Test
    fun testToAbsoluteUrl() {
        val url = "http://some-server.com/feeds/podcast.xml"
        val dummy = PodcastDummy(null, url)

        assertEquals("", dummy.toAbsoluteUrlTest(""))
        assertEquals(url, dummy.toAbsoluteUrlTest(url))
        assertEquals("http://some-server.com/feeds/blödsinn",
                dummy.toAbsoluteUrlTest("blödsinn"))
        assertEquals("http://some-server.com/bla/image.png",
                dummy.toAbsoluteUrlTest("/bla/image.png"))
        assertEquals("http://some-server.com/feeds/bla/image.png",
                dummy.toAbsoluteUrlTest("bla/image.png"))
    }
}

private class PodcastDummy(name: String?, url: String) : Podcast(name, url) {
    fun toAbsoluteUrlTest(relativeUrl: String) = super.toAbsoluteUrl(relativeUrl)
}