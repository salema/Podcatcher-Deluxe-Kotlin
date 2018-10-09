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
import com.podcatcher.deluxe.BR

class Podcast(name: String, var feed: String) : BaseObservable(), Comparable<Podcast> {

    enum class Status { READY, LOADING, FAILED }

    constructor(name: String, logo: String, feed: String, episodes: MutableList<Episode>) : this(name, feed) {
        this.logo = logo

        this.episodes.clear()
        this.episodes.addAll(episodes)
    }

    @get:Bindable
    var name: String = name
        set(value) {
            field = value
            notifyPropertyChanged(BR.name)
        }

    var logo: String? = null

    @get:Bindable
    val episodes: MutableList<Episode> = mutableListOf()

    fun addEpisode(episode: Episode) {
        episodes.add(episode)
        notifyPropertyChanged(BR.episodes)
    }

    @get:Bindable
    var status: Status = Status.READY
        set(value) {
            field = value
            notifyPropertyChanged(BR.status)
        }

    override fun compareTo(other: Podcast): Int {
        return this.name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        // TODO omit http(s) and www. in comparison

        return super.equals(other)
    }
}