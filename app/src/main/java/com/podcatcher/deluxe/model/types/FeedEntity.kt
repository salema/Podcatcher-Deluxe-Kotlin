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

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.room.ColumnInfo
import com.podcatcher.deluxe.BR
import com.podcatcher.deluxe.model.tags.RSS
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * The abstract root type of the main podcatcher app types, including [Podcast],
 * [Episode], and [Suggestion]. Defines some members needed in all of them.
 */
abstract class FeedEntity(name: String?, url: String) : BaseObservable() {

    /**
     * The entity's title, e.g. podcast name or episode title
     */
    @get:Bindable
    var name: String? = name
        set(value) {
            field = value
            notifyPropertyChanged(BR.name)
        }

    /**
     * The entity's online location, e.g. the feed or enclosure URL
     */
    var url: String = url

    /**
     * The entity's description, e.g. the episode details
     */
    var description: String? = null

    /**
     * Whether the entity is considered explicit, i.e. contains adult-only material
     */
    var explicit = false

    /**
     * The element's file size in bytes.
     */
    @ColumnInfo(name = "file_size")
    var fileSize: Long = -1

    /**
     * Normalize the given URL string.
     * See http://en.wikipedia.org/wiki/URL_normalization for details.
     *
     * @param spec The URL string to normalize.
     * @return The same URL string with unchanged semantics, but normalized
     * syntax. When not a valid URL, the string given is returned unaltered.
     */
    protected fun normalizeUrl(spec: String): String {
        try {
            // Trim white spaces, normalize path, throw exception if mal-formed
            val url = URI(spec.trim()).normalize().toURL()

            // Make sure protocol and server are lower case
            val scheme = url.protocol.toLowerCase(Locale.US)
            val host = url.host.toLowerCase(Locale.US)

            // Normalize path to be at least "/"
            var path: String? = url.path
            if (path == null || path.isEmpty())
                path = "/"
            else if (path.length > 1 && path.endsWith("/") && url.query == null)
                path = path.substring(0, path.length - 1)

            // Look at ports and only keep non-defaults
            var needsPort = url.port != -1
            if (scheme == "http" && url.port == 80 || scheme == "https" && url.port == 443)
                needsPort = false

            // Reconstruct the string
            return "$scheme://$host" + (if (needsPort) ":" + url.port else "") +
                    "$path" + (if (url.query != null) "?" + url.query else "")
        } catch (e: MalformedURLException) {
            // We simply return the original string
            return spec
        } catch (e: URISyntaxException) {
            return spec
        } catch (e: IllegalArgumentException) {
            return spec
        }
    }

    /**
     * Check whether the given string value indicates that the feed entity is
     * considered explicit (adult-only content).
     *
     * @param value The string value from the feed, `null` results in `false`.
     * @return The explicit flag.
     */
    protected fun parseExplicit(value: String): Boolean {
        return value.trim().toLowerCase(Locale.US) == RSS.EXPLICIT_POSITIVE_VALUE
    }

    /**
     * Parse a string into a date. Can be used for last feed updates or
     * publication dates. The method will try to read different formats.
     *
     * @param dateString The string from the RSS/XML feed to parse.
     * @return The date or `null` if the string could not be parsed.
     */
    protected fun parseDate(dateString: String): Date? {
        // Make sure to remove any whitespaces that might make parsing fail
        val candidate = dateString.trim()

        try {
            // SimpleDateFormat is not thread safe
            synchronized(DATE_FORMATTER) {
                return DATE_FORMATTER.parse(candidate)
            }
        } catch (outer: ParseException) {
            // The default format is not available, try all the other formats we support...
            for (format in DATE_FORMAT_TEMPLATE_ALTERNATIVES)
                try {
                    return SimpleDateFormat(format, Locale.US).parse(candidate)
                } catch (inner: ParseException) {
                    // Does not fit the format, pass and try next
                }
        }

        // None of the formats matched
        return null
    }

    companion object {

        /**
         * The date format used by RSS feeds.
         */
        private const val DATE_FORMAT_TEMPLATE = "EEE, dd MMM yy HH:mm:ss zzz"
        /**
         * Our formatter used when reading the entities date string.
         */
        private val DATE_FORMATTER = SimpleDateFormat(DATE_FORMAT_TEMPLATE, Locale.US)
        /**
         * The alternative date formats supported because they are used by some
         * feeds, these are all tried in the given order if the default fails.
         */
        private val DATE_FORMAT_TEMPLATE_ALTERNATIVES = arrayOf("EEE, dd MMM yy", "yy-MM-dd", "dd MMM yy HH:mm:ss zzz", "EEE,dd MMM yy HH:mm:ss zzz")
    }
}
