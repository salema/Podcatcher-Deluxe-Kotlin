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
package com.podcatcher.deluxe.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.podcatcher.deluxe.model.types.Episode
import com.podcatcher.deluxe.model.types.Podcast
import java.util.*

class PodcastViewModel(app: Application) : AndroidViewModel(app) {

    val selectedPodcast: MutableLiveData<Podcast> by lazy { MutableLiveData<Podcast>() }
    val selectedEpisode: MutableLiveData<Episode> by lazy { MutableLiveData<Episode>() }

    private lateinit var podcastList: MutableLiveData<List<Podcast>>
    val podcasts: LiveData<List<Podcast>> by lazy {
        podcastList = MutableLiveData<List<Podcast>>()
        podcastList.value = listOf(
                Podcast("Radiolab", "http://media.wnyc.org/i/raw/1/Radiolab_WNYCStudios_1400_2dq02Dh.png", "http://feeds.wnyc.org/radiolab", mutableListOf()),
                Podcast("This American Life", "http://files.thisamericanlife.org/sites/all/themes/thislife/img/tal-name-1400x1400.png", "http://www.thisamericanlife.org/podcast/rss.xml", mutableListOf()),
                Podcast("Heldenstadt", "https://images.podigee.com/0x,sEm-lJYlWr9ZdNbHTarf6cC2z5C-YBOy4Z34JkiILpis=/https://cdn.podigee.com/uploads/u2989/7ee53a3d-0973-49c1-a728-f964c403e72c.jpg", "https://heldenstadt.podigee.io/feed/mp3", mutableListOf()),
                Podcast("Security Now!", "http://twit.cachefly.net/coverart/sn/sn1400.jpg", "https://feeds.twit.tv/sn.xml", mutableListOf())
        ).sorted()

        podcastList
    }
/*
    private val _podcastList = mutableListOf<Podcast>()
    val podcastLis: LiveData<List<Podcast>>
        get() {

            return
        }*/

    fun addPodcast() {
        val newPodcast = Podcast("Testpodcast", "https://cdn.learn2crack.com/wp-content/uploads/2016/02/cover5-1024x483.png", UUID.randomUUID().toString(), mutableListOf())

        val newList = podcastList.value?.toMutableList()
        newList?.add(newPodcast)

        podcastList.value = newList?.sorted()

        podcastList.value?.forEach { it.status = if (it.status == Podcast.Status.LOADING) Podcast.Status.READY else Podcast.Status.LOADING }
        podcastList.value?.forEach { it.addEpisode(Episode("nlsd", "dfd")) }
    }

    fun removePodcast(vararg podcasts: Podcast) {
        podcastList.value = podcastList.value?.filterNot { podcasts.contains(it) }
    }
}