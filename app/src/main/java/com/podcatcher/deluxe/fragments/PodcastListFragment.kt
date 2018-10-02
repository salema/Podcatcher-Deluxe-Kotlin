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
package com.podcatcher.deluxe.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.podcatcher.deluxe.R
import com.podcatcher.deluxe.isLandscape
import com.podcatcher.deluxe.isSmall
import kotlinx.android.synthetic.main.podcast_list_fragment.*

fun Fragment.isLandscape(): Boolean {
    return resources.isLandscape()
}

fun Fragment.isSmall(): Boolean {
    return resources.isSmall()
}

class PodcastListFragment : Fragment() {

    private var wasInfalted : Boolean = false
    private var actAsDummy : Boolean = false

    override fun onInflate(context: Context?, attrs: AttributeSet?, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)

        wasInfalted = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        actAsDummy = !wasInfalted && isSmall() && isLandscape()
        return inflater.inflate(
                if (actAsDummy) R.layout.podcast_list_fragment_empty else R.layout.podcast_list_fragment,
                container,false)    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!actAsDummy)
            select_podcast_button.setOnClickListener {
                // Update LiveData

                if (isSmall())
                    Navigation.findNavController(activity as AppCompatActivity, R.id.navhost_fragment)
                            .navigate(if (isLandscape()) R.id.nav_action_global_episodes else R.id.nav_action_podcasts_episodes)
            }
    }
}