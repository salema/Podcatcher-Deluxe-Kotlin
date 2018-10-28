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

package com.podcatcher.deluxe.model.tags

/**
 * Defines some constants used in RSS.
 *
 * @see [RSS specification](http://cyber.law.harvard.edu/rss/rss.html)
 */
object RSS {
    // These should be all lowercase because the parser compares
    // to the XML tag name when .toLowerCase() is applied!
    val ITEM = "item"
    val GUID = "guid"
    val IMAGE = "image"
    val TITLE = "title"
    val SUBTITLE = "subtitle"
    val EXPLICIT = "explicit"
    val ENCLOSURE = "enclosure"
    val URL = "url"
    val NEW_URL = "new-feed-url"
    val MEDIA_TYPE = "type"
    val MEDIA_TYPE_AUDIO = "audio"
    val MEDIA_TYPE_VIDEO = "video"
    val MEDIA_LENGTH = "length"
    val HREF = "href"
    val LINK = "link"
    val REL = "rel"
    val NEXT = "next"
    val CATEGORY = "category"
    val LANGUAGE = "language"
    val KEYWORDS = "keywords"
    val DATE = "date"
    val PUBDATE = "pubdate"
    val DURATION = "duration"
    val THUMBNAIL = "thumbnail"
    val DESCRIPTION = "description"
    val SUMMARY = "summary"
    val CONTENT_ENCODED = "encoded"
    val CHAPTERS = "chapters"
    val CHAPTER = "chapter"
    val CHAPTER_START = "start"
    val CHAPTER_TITLE = "title"

    val CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/"
    val ATOM_NAMESPACE = "http://www.w3.org/2005/Atom"
    val EXPLICIT_POSITIVE_VALUE = "yes"
    val DATE_NOW = "now"
}
