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

import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import com.podcatcher.deluxe.R
import com.podcatcher.deluxe.model.types.Podcast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.podcast_list_fragment.*
import kotlinx.android.synthetic.main.podcast_list_item.view.*

/**
 * List fragment to display the list of podcasts.
 */
class PodcastListFragment : AbstractPodcastFragment(), OnPodcastSelectedListener {

    private var listAdapter: PodcastListAdapter = PodcastListAdapter(this)

    /**
     * Since we use the podcast list fragment in two modes when in small
     * landscape, we need to make sure to differentiate those. The dummy
     * instance is not inflated, so we grab that value onInflate()
     */
    private var wasInflated : Boolean = false
    /**
     * If this fragment instance works in dummy mode (see above)
     */
    private var actAsDummy : Boolean = false

    override fun onInflate(context: Context?, attrs: AttributeSet?, savedInstanceState: Bundle?) {
        super.onInflate(context, attrs, savedInstanceState)

        wasInflated = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        // This is a special case on small screens in landscape
        actAsDummy = !wasInflated && isSmall() && isLandscape()

        setHasOptionsMenu(!actAsDummy)
        return inflater.inflate(
                if (actAsDummy) R.layout.podcast_list_fragment_empty else R.layout.podcast_list_fragment,
                container,false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        activity?.menuInflater?.inflate(R.menu.menu_podcastlist, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!actAsDummy) {
            podcast_list.setHasFixedSize(isSmall()) // TODO Is this accurate for all sizes/layouts?
            podcast_list.layoutManager = LinearLayoutManager(activity)
            podcast_list.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.HORIZONTAL))
            podcast_list.adapter = listAdapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!actAsDummy) {
            model.podcasts.observe(this, Observer<List<Podcast>> {
                // This will update the podcast list with nice animations
                listAdapter.submitList(it)
            })
            model.selectedPodcast.observe(this, Observer<Podcast> {
                // TODO Handle selection here?
            })
        }
    }

    override fun onPodcastSelected(podcast: Podcast) {
        model.selectedPodcast.value = podcast

        // On small screen, use navigation to show episode list
        if (isSmall())
            Navigation.findNavController(activity as AppCompatActivity, R.id.navhost_fragment)
                    .navigate(if (isLandscape()) R.id.nav_action_global_episodes else R.id.nav_action_podcasts_episodes)
    }
}

private interface OnPodcastSelectedListener {

    /**
     * Callback alerted on podcast list "clicks"
     * @param podcast The podcast selected in the list
     */
    fun onPodcastSelected(podcast: Podcast)
}

/**
 * Recycler view adapter for the podcast list. Uses a diff callback
 * to enable nice animations and alerts the fragment on selection.
 */
private class PodcastListAdapter(private val listener: OnPodcastSelectedListener)
    : ListAdapter<Podcast, PodcastListAdapter.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.podcast_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)

        holder.titleView.text = podcast.name
        Picasso.get().load(podcast.logo).into(holder.logoView)

        holder.view.setOnClickListener() {
            listener.onPodcastSelected(podcast)
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.podcast_title
        val logoView: ImageView = view.podcast_logo
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Podcast>() {

            override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
                // Two podcasts are the same when their feed URLs match
                return oldItem.feed == newItem.feed
            }

            override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
                // TODO Check for the podcast properties actually shown in the list here
                return oldItem == newItem
            }
        }
    }
}