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

import android.text.Html
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.podcatcher.deluxe.model.skipSubTree
import com.podcatcher.deluxe.model.tags.RSS
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*

@Entity(tableName = "episodes",
        foreignKeys = [ForeignKey(entity = Podcast::class, parentColumns = ["guid"], childColumns = ["podcast"])],
        indices = [Index("podcast")])
data class Episode(@PrimaryKey var guid: String, var name: String, var enclosure: String, var podcast: String?) {

    /**
     * Read data from an item node in the RSS/XML podcast file and use it to set
     * this episode's fields.
     *
     * @param parser Podcast feed file parser, set to the start tag of the item to read.
     * @throws XmlPullParserException On parsing problems.
     * @throws IOException            On I/O problems.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(parser: XmlPullParser) {
        // Make sure we start at item tag
        parser.require(XmlPullParser.START_TAG, "", RSS.ITEM)

        // Look at all start tags of this item
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            val tagName = parser.name.toLowerCase(Locale.US)

            if (tagName == RSS.TITLE)
                name = Html.fromHtml(parser.nextText().trim { it <= ' ' }).toString()
            /*else if (tagName == RSS.GUID)
                guid = parser.nextText()
            else if (tagName == RSS.LINK)
                url = parser.nextText()
            else if (tagName == RSS.EXPLICIT)
                explicit = parseExplicit(parser.nextText())
            else if (tagName == RSS.DATE && pubDate == null)
                pubDate = parseDate(parser.nextText())
            else if (tagName == RSS.PUBDATE)
                pubDate = parseDate(parser.nextText())
            else if (tagName == RSS.DURATION)
                duration = parseDuration(parser.nextText())
            else if (tagName == RSS.DESCRIPTION || tagName == RSS.SUMMARY)
                description = parseDescription(parser.nextText()).toInt()
            else if (isContentEncodedTag(parser))
                content = parser.nextText()*/
            else if (tagName == RSS.ENCLOSURE)
                parseEnclosure(parser)
            /*else if (tagName == RSS.CHAPTERS)
                parseChapters(parser)*/
            else
                parser.skipSubTree()
        }

        // Make sure we end at item tag
        parser.require(XmlPullParser.END_TAG, "", RSS.ITEM)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    protected fun parseEnclosure(parser: XmlPullParser) {
        // Extract information needed to decide if we pick this enclosure
        //val urlAttribute = normalizeUrl(parser.getAttributeValue("", RSS.URL))
        enclosure = parser.getAttributeValue("", RSS.URL)
        guid = enclosure
        //val typeAttribute = parseMediaType(parser.getAttributeValue("", RSS.MEDIA_TYPE))
        //val lengthAttribute = parseFileSize(parser.getAttributeValue("", RSS.MEDIA_LENGTH))

        // This enclosure is only picked if (1) it actually has a media URL
        // and we either have (2) nothing at all yet or this one is (3) better than the current one
        /*if (urlAttribute != null && (mediaUrl == null || isBetterEnclosure(typeAttribute))) {
            mediaUrl = urlAttribute
            mediaType = typeAttribute
            fileSize = lengthAttribute
        }
*/
        parser.nextText()
    }
}
