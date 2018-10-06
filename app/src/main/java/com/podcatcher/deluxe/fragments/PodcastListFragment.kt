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

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.*
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_action_add_podcast -> {
                model.addPodcastAtRandomPosition()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!actAsDummy) {
            podcast_list.setHasFixedSize(isSmall()) // TODO Is this accurate for all sizes/layouts?
            podcast_list.layoutManager = LinearLayoutManager(activity)
            podcast_list.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
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
        val resources = holder.view.resources
        val podcast = getItem(position)
        val status = podcast.getStatus()

        holder.titleView.text = podcast.name
        holder.captionView.text =
                if (status == 0)
                    resources.getQuantityString(R.plurals.episodes, podcast.episodes.size, podcast.episodes.size)
                else resources.getText(R.string.podcast_loading)

        holder.progressView.visibility = if (status == 0) View.GONE else View.VISIBLE

        holder.newEpisodesCount.visibility = if (status != 0) View.GONE else View.VISIBLE
        holder.newEpisodesCount.text = podcast.episodes.size.toString()

        Picasso.get().load(podcast.logo).into(holder.logoView)

        holder.view.setOnClickListener() {
            listener.onPodcastSelected(podcast)
        }
    }

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.podcast_title
        val captionView: TextView = view.podcast_caption
        val progressView: ProgressBar = view.podcast_progress
        val newEpisodesCount: TextView = view.podcast_new_count
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