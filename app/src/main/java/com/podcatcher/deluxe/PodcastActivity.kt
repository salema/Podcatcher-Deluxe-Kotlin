/**
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

import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.PopupMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.podcast_activity.*

class PodcastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.podcast_activity)

        setSupportActionBar(toolbar)
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
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.menu_toolbar)
        popup.setOnMenuItemClickListener{
            when (it.itemId) {
                R.id.menu_action_select_all_podcasts,
                R.id.menu_action_show_downloads,
                R.id.menu_action_show_playlist -> {
                    // Update LiveData

                    if (resources.configuration.isSmall())
                        Navigation.findNavController(this, R.id.navhost_fragment).navigate(R.id.nav_action_global_episodes)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }
}