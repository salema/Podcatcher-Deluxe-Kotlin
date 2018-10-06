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

import java.util.*

data class Podcast(val name: String, val logo: String, var feed: String, val episodes: MutableList<Episode>) : Comparable<Podcast> {


    fun getStatus(): Int {
        return Random().nextInt(2)
    }

    override fun compareTo(other: Podcast): Int {
        return this.name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        // TODO omit http(s) and www. in comparison

        return super.equals(other)
    }
}