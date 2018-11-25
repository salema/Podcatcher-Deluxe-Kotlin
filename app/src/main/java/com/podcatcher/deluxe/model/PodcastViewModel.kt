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
import com.podcatcher.deluxe.BuildConfig
import com.podcatcher.deluxe.model.types.Episode
import com.podcatcher.deluxe.model.types.Podcast
import java.util.*

class PodcastViewModel(app: Application) : AndroidViewModel(app) {
    init {
        // TODO Find a proper place for this
        Picasso.get().setIndicatorsEnabled(BuildConfig.DEBUG)
    }

    private val repo = PodcastRepository.getInstance(app.applicationContext)

    enum class SpecialEpisodeList { ALL_PODCASTS, DOWNLOADS, PLAYLIST }

    private var _selectedSpecialEpisodeList = MutableLiveData<SpecialEpisodeList?>()
    val selectedSpecialEpisodeList: LiveData<SpecialEpisodeList?> by lazy {
        _selectedSpecialEpisodeList
    }

    fun selectSpecialEpisodeList(list: SpecialEpisodeList?) {
        _selectedSpecialEpisodeList.value = list
        if (list != null)
            _selectedPodcast.value = null
    }

    private lateinit var _selectedPodcast: MutableLiveData<Podcast?>
    val selectedPodcast: LiveData<Podcast?> by lazy {
        _selectedPodcast = MutableLiveData()
        _selectedPodcast
    }

    fun selectPodcast(podcast: Podcast?) {
        _selectedPodcast.value = podcast
        if (podcast != null)
            _selectedSpecialEpisodeList.value = null
    }

    val selectedEpisode: MutableLiveData<Episode> by lazy { MutableLiveData<Episode>() }

    val podcasts = repo.getSubscriptions()

    fun addPodcast() {
        val newPodcast = Podcast("Testpodcast", UUID.randomUUID().toString())
        newPodcast.logoUrl = "https://cdn.learn2crack.com/wp-content/uploads/2016/02/cover5-1024x483.png"
        newPodcast.subscribed = true

        repo.add(newPodcast)
    }

    fun removePodcast(vararg podcasts: Podcast) {
        podcasts.forEach { repo.remove(it) }
        if (!podcasts.contains(_selectedPodcast.value))
            _selectedPodcast.value = null
    }

    override fun onCleared() {
        super.onCleared()

        repo.onAppClosed()
    }
}