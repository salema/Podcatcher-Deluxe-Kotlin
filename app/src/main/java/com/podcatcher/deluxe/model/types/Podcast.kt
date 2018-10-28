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

import android.net.Uri
import androidx.databinding.Bindable
import androidx.room.*
import com.podcatcher.deluxe.BR
import com.podcatcher.deluxe.model.skipSubTree
import com.podcatcher.deluxe.model.tags.RSS
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

@Entity(tableName = "podcasts",
        indices = [Index("url", unique = true)])
open class Podcast(name: String?, url: String) : FeedEntity(name, url), Comparable<Podcast> {

    @PrimaryKey
    var guid: String = createGuid(url)

    @Ignore
    val feed = url

    @ColumnInfo(name = "logo_url")
    var logoUrl: String? = null

    var subscribed: Boolean = true

    enum class Status { READY, LOADING, FAILED }

    @get:Bindable
    var status: Status = Status.READY
        set(value) {
            field = value
            notifyPropertyChanged(BR.status)
        }

    var lastSuccessfulLoad: Date? = null

    override fun compareTo(other: Podcast): Int {
        return other.name?.let { this.name?.compareTo(it) } ?: 0
    }

    private fun createGuid(feed: String): String {
        // TODO omit http(s) and www. and trailing /
        return feed
    }

    /**
     * Set the RSS file parser representing this podcast. This is were the
     * object gets its information from. Many of its methods will not return
     * valid results unless this method was called. Calling this method resets
     * all episode information that might have been read earlier, other meta
     * data is preserved and will only change if the feed has actually changed.
     * However, episode information *is* preserved if parsing fails. In
     * this case the episode list will not be altered.
     *
     * @param parser Parser used to read the RSS/XML file.
     * @return The value of the new-feed-url tag given on the feed, if any.
     * @throws IOException            If we encounter problems reading the file.
     * @throws XmlPullParserException On parsing errors.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(parser: XmlPullParser): List<Episode> {
        val newEpisodes = mutableListOf<Episode>()
        var result: String? = null
        //this.nextPage = null

        try {
            // Start parsing
            //this.feedEncoding = parser.inputEncoding
            var eventType = parser.next()
            var episodeIndex = 0

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.toLowerCase(Locale.US)

                    when (tagName) {
                        //RSS.TITLE -> if (name == null || name.trim().isEmpty())
                        //    name = Html.fromHtml(parser.nextText().trim { it <= ' ' }).toString()
                        //RSS.LINK -> parseLink(parser)
                        //RSS.EXPLICIT -> explicit = parseExplicit(parser.nextText())
                        /*RSS.NEW_URL -> {
                            result = normalizeUrl(parser.nextText())
                            // Make sure this is a good URL to move to
                            if (result == null || equalByUrl(result) || !result.startsWith("http"))
                                result = null
                        }*/
                        RSS.IMAGE -> parseLogo(parser)
                        RSS.THUMBNAIL -> if (logoUrl == null) logoUrl = parser.getAttributeValue("", RSS.URL)
                        RSS.ITEM -> parseAndAddEpisode(parser, newEpisodes, episodeIndex++)
                        //else -> parse(parser, tagName)
                    }
                }

                // Done, get next parsing event
                eventType = parser.next()
            }

            // Parsing completed without errors, mark as updated
            //episodes.clear()
            //episodes.addAll(newEpisodes)
            //notifyPropertyChanged(BR.episodes)
            lastSuccessfulLoad = Date()
        } finally {
            // Make sure name is not empty
            name?.let { if (it.isBlank()) name = url }
        }

        return newEpisodes
    }

    @Throws(IOException::class)
    protected fun parseLogo(parser: XmlPullParser) {
        try {
            // Check for href attribute (of <itunes:image> tag)
            val href = parser.getAttributeValue("", RSS.HREF)

            if (href != null)
                logoUrl = toAbsoluteUrl(href)
            else if (logoUrl == null) {
                // URL tag used instead. We do not override any previous setting, because
                // the href is from the <itunes:image> tag which tends to have better pics.
                parser.require(XmlPullParser.START_TAG, "", RSS.IMAGE)

                // Look at all start tags of this image
                while (parser.nextTag() == XmlPullParser.START_TAG) {
                    // URL tag found
                    if (parser.name.equals(RSS.URL, ignoreCase = true))
                        logoUrl = toAbsoluteUrl(parser.nextText())
                    else
                        parser.skipSubTree()// Unneeded node, skip...
                }

                // Make sure we end at image tag
                parser.require(XmlPullParser.END_TAG, "", RSS.IMAGE)
            }
        } catch (e: XmlPullParserException) {
            // The podcast logoUrl information could not be read from the RSS file,
            // skip...
        }
    }

    /**
     * Rewrite given relative URL to an absolute URL for this podcast. If the
     * given URL is already absolute (or empty or `null`) it is
     * returned unchanged.
     *
     * @param relativeUrl URL to rewrite.
     * @return An absolute URL based on the podcast URL. E.g. if "test/pic.jpg"
     * is provided, "http://www.example.com/feed/test/pic.jpg" might be
     * returned.
     */
    protected fun toAbsoluteUrl(relativeUrl: String?): String? {
        var result = relativeUrl

        // Rewrite logoUrl url to be absolute
        if (logoUrl != null && relativeUrl != null && !relativeUrl.isEmpty() &&
                Uri.parse(relativeUrl).isRelative()) {
            val podcastUrl = Uri.parse(logoUrl)
            val prefix = podcastUrl.getScheme() + "://" + podcastUrl.getAuthority()

            if (relativeUrl.startsWith("/"))
                result = prefix + relativeUrl
            else {
                val path = podcastUrl.getPath()

                result = prefix + path.substring(0, path.length - podcastUrl.getLastPathSegment().length) + relativeUrl
            }
        }

        return result
    }

    protected fun parseAndAddEpisode(parser: XmlPullParser,
                                     list: MutableList<Episode>, index: Int) {
        // Create episode and parse the data
        val newEpisode = Episode("", "", "", guid)

        try {
            with(newEpisode) {
                parse(parser)

                // Only add if there is a title and some actual content to play
                if (!name.isBlank() && !enclosure.isBlank())
                    list.add(this)
            }
        } catch (e: XmlPullParserException) {
            // pass, episode will not be added
        } catch (e: IOException) {
        }
    }
}

/**
 * Helper sub-class to make the relation mapping to the episodes work
 */
class PodcastWithEpisodes(name: String?, url: String) : Podcast(name, url) {

    @get:Bindable
    @Relation(parentColumn = "guid", entityColumn = "podcast")
    var episodes: List<Episode> = arrayListOf()
}
