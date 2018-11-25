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
import android.text.Html
import android.util.Base64
import androidx.databinding.Bindable
import androidx.room.*
import com.podcatcher.deluxe.BR
import com.podcatcher.deluxe.model.skipSubTree
import com.podcatcher.deluxe.model.tags.RSS
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.util.*

/**
 * TODO Update this description to reflect new model
 * The podcast type. This represents the most important type in the podcatcher
 * application. To create a podcast, give its name and an online location to
 * load its RSS/XML file from. The online location is not verified or checked
 * inside the podcast type.
 *
 * **Parsing:** Call [.parse] with the parser set up to
 * the correct content in order to make the podcast object read and refresh its
 * members. Use [.getLastLoaded] to find out whether and when this last
 * happened to a given podcast instance.
 *
 * **Comparisons and Equals:** For the purpose of the podcatcher app, two
 * podcasts are equal iff they point at the same online feed resource. The
 * [.compareTo] method works on the podcast's name though and is
 * therefore *not* consistent with [.equals].
 *
 * **Logo:** Podcast often have logos. This podcast type allows for access to
 * the logo's online location (after [.parse], of course).
 *
 * **Paged feeds:** This type supports paged feeds as defined by
 * http://podlove.org/paged-feeds/. Call [.getNextPage] after parsing to
 * find if the podcast has any additional pages. There is also an expanded flag
 * you can use to mark the podcast as "should read all pages when parsed".
 */
@Entity(tableName = "podcasts",
        indices = [Index("url", unique = true)])
open class Podcast(name: String?, url: String) : FeedEntity(name, url), Comparable<Podcast> {
    init {
        this.name = name?.trim()
        this.url = normalizeUrl(url)
    }

    /**
     * The online location this podcast can be refreshed from.
     */
    @Ignore
    val feed = this.url

    @PrimaryKey
    var guid: String = createGuid(this.url)

    /**
     * Whether the user is currently subscribed to this podcast
     */
    var subscribed: Boolean = true

    /**
     * The feed remote file encoding or `null` if not yet parsed or
     * detected by the parser.
     */
    @ColumnInfo(name = "feed_encoding")
    var feedEncoding: String? = null

    /**
     * The podcast's image (logo) location
     */
    @ColumnInfo(name = "logo_url")
    var logoUrl: String? = null


    enum class Status { READY, LOADING, FAILED }

    @get:Bindable
    var status: Status = Status.READY
        set(value) {
            field = value
            notifyPropertyChanged(BR.status)
        }

    /**
     * Username for http authorization
     */
    var username: String? = null
    /**
     * Password for http authorization
     */
    var password: String? = null
    /**
     * @return Authorization string to be used as a HTTP request header or
     * `null` if user name or password are not set.
     */
    val authorization: String?
        get() {
            var result: String? = null

            if (username != null && password != null) {
                val userpass = "$username:$password"
                val authBytes = userpass.toByteArray(Charset.forName("UTF-8"))

                result = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP)
            }

            return result
        }

    /**
     * The point in time when the RSS file as last been loaded and parsed successfully
     */
    var lastSuccessfulLoad: Date? = null
    /**
     * The count of failed load attempts
     */
    var failedLoadAttemptCount: Int = 0

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is Podcast -> false
            else ->
                // Podcasts are equal iff they have the same guid
                guid == other.guid
        }
    }

    override fun hashCode(): Int {
        return 42 + guid.hashCode();
    }

    override fun compareTo(other: Podcast): Int {
        return other.name?.let { this.name?.compareTo(it) } ?: 0
    }

    private fun createGuid(feed: String): String {
        // Omit http(s), www. and trailing /
        return feed.replaceFirst("^https?://(www.)?".toRegex(), "")
                .replace("/$".toRegex(), "")
    }

    /**
     * TODO Update this description to reflect new model
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
        val episodes = mutableListOf<Episode>()

        try {
            // Start parsing
            feedEncoding = parser.inputEncoding
            var eventType = parser.next()

            // Read complete document
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // We only need start tags here
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name.toLowerCase(Locale.US)

                    when (tagName) {
                        RSS.TITLE -> if (name.isNullOrBlank()) {
                            name = Html.fromHtml(parser.nextText().trim()).toString()
                        }
                        //RSS.LINK -> parseLink(parser)
                        RSS.EXPLICIT -> explicit = parseExplicit(parser.nextText())
                        /*RSS.NEW_URL -> {
                            result = normalizeUrl(parser.nextText())
                            // Make sure this is a good URL to move to
                            if (result == null || equalByUrl(result) || !result.startsWith("http"))
                                result = null
                        }*/
                        RSS.IMAGE -> parseLogo(parser)
                        RSS.THUMBNAIL -> if (logoUrl == null) logoUrl = parser.getAttributeValue("", RSS.URL)
                        RSS.ITEM -> parseAndAddEpisode(parser, episodes)
                        else -> parse(parser, tagName)
                    }
                }

                // Done, get next parsing event
                eventType = parser.next()
            }
        } finally {
            // Make sure name is not empty
            name?.let { if (it.isBlank()) name = url }
        }

        return episodes
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

    protected fun parseAndAddEpisode(parser: XmlPullParser, list: MutableList<Episode>) {
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

    /**
     * Called for tags not consumed by the podcast parsing. Use this in sub-classes,
     * if you need to get more data from the podcast feed. This will *not* be
     * called for tags the podcast consumes itself.
     *
     * @param parser  The current parser. Make sure to only consume the current tag!
     * @param tagName The current tag's name, all lower case.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    protected fun parse(parser: XmlPullParser, tagName: String) {
        // Do nothing, subclass might want to use this hook
        // to read other information they care about from the feed
    }

    /**
     * Rewrite given relative URL to an absolute URL for this podcast. If the
     * given URL is already absolute (or empty) it is returned unchanged.
     *
     * @param relativeUrl URL to rewrite.
     * @return An absolute URL based on the podcast URL. E.g. if "test/pic.jpg"
     * is provided, "http://www.example.com/feed/test/pic.jpg" might be returned.
     */
    protected fun toAbsoluteUrl(relativeUrl: String): String {
        return if (!relativeUrl.isEmpty() && Uri.parse(relativeUrl).isRelative)
            URL(URL(url), relativeUrl).toExternalForm()
        else relativeUrl
    }

    override fun normalizeUrl(spec: String): String {
        var result = spec

        // We put some extra bit in here to that only apply to podcast URLs and
        // then call the base class method.
        with(result.toLowerCase(Locale.US)) {
            if (matches("^(feed|itpc|itms)://.*".toRegex()))
                result = "http" + result.substring(4)
            else if (startsWith("fb:"))
                result = "http://feeds.feedburner.com/" + result.substring(3)
        }

        // Try to get username and password if present
        Uri.parse(result).userInfo?.let {
            val parts = it.split(':', limit = 2)

            this.username = parts[0]
            if (parts.size > 1 && !parts[1].isBlank()) this.password = parts[1]
        }

        result = super.normalizeUrl(result)
        // Re-append final slash
        if (spec.endsWith('/') && !result.endsWith('/'))
            result += '/'

        if (result.contains("://feeds2.feedburner.com"))
            result = result.replaceFirst("feeds2", "feeds")
        if (result.contains("://feeds.feedburner.com") && result.endsWith("?format=xml"))
            result = result.replace("?format=xml", "")

        return result
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
