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

package com.podcatcher.deluxe.model.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.podcatcher.deluxe.model.types.Podcast
import com.podcatcher.deluxe.model.types.PodcastWithEpisodes

@Dao
interface PodcastDao {
    @Transaction
    @Query("SELECT * FROM podcasts WHERE subscribed ORDER BY name")
    fun getSubscriptions(): LiveData<List<PodcastWithEpisodes>>

    @Transaction
    @Query("SELECT * FROM podcasts WHERE url = :feed")
    fun getPodcastForFeed(feed: String): PodcastWithEpisodes?

    @Insert
    fun insert(vararg podcasts: Podcast)

    @Update
    fun update(vararg podcasts: Podcast)
}
