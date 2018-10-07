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
package com.podcatcher.deluxe.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.podcatcher.deluxe.R
import com.podcatcher.deluxe.model.types.Episode
import com.podcatcher.deluxe.model.types.Podcast
import kotlinx.android.synthetic.main.episode_list_fragment.*

class EpisodeListFragment : AbstractPodcastFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.episode_list_fragment, container,false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        activity?.menuInflater?.inflate(R.menu.menu_episodelist, menu)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model.selectedPodcast.observe(this, Observer<Podcast> { podcast ->
            message.text = podcast.name
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        select_episode_button.setOnClickListener {
            model.selectedEpisode.value = Episode("Episode list episode", "")

            if (isSmall())
                Navigation.findNavController(activity as AppCompatActivity, R.id.navhost_fragment).navigate(R.id.nav_action_episodes_episode)
        }
    }

    override fun onResume() {
        super.onResume()

        message.text = model.selectedPodcast.value?.name ?: "Null!"
    }
}