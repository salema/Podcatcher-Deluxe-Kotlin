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
package com.podcatcher.deluxe

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import com.podcatcher.deluxe.model.PodcastViewModel
import com.podcatcher.deluxe.model.types.Podcast
import kotlinx.android.synthetic.main.podcast_activity.*

class PodcastActivity : AppCompatActivity() {

    private lateinit var model: PodcastViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.podcast_activity)

        setSupportActionBar(toolbar)

        model = ViewModelProviders.of(this).get(PodcastViewModel::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun showToolbarPopup(view: View) {
        fun selectPodcastAndNavigate(podcast: String): Boolean {
            model.selectedPodcast.value = Podcast(podcast, "", "", mutableListOf())

            if (resources.configuration.isSmall())
                Navigation.findNavController(this, R.id.navhost_fragment).navigate(R.id.nav_action_global_episodes)

            return true
        }

        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.menu_toolbar)
        popup.setOnMenuItemClickListener{
            when (it.itemId) {
                R.id.menu_action_select_all_podcasts -> selectPodcastAndNavigate("All podcasts")
                R.id.menu_action_show_downloads ->selectPodcastAndNavigate("Downloads")
                R.id.menu_action_show_playlist -> selectPodcastAndNavigate("Playlist")
                else -> false
            }
        }

        popup.show()
    }
}