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
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.*
import com.podcatcher.deluxe.BR
import com.podcatcher.deluxe.R
import com.podcatcher.deluxe.databinding.PodcastListItemBinding
import com.podcatcher.deluxe.model.types.Podcast
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.podcast_list_fragment.*
import kotlinx.android.synthetic.main.podcast_list_item.view.*

/**
 * List fragment to display the list of podcasts.
 * Selection is handled using a selection tracker.
 */
class PodcastListFragment : AbstractPodcastFragment() {

    companion object {
        /**
         * Our selection tracker id. Note that the selection is not carried across
         * configuration changes on small devices because those use two instances of
         * the podcast list fragment in landscape mode.
         * TODO Check if this works on large devices
         */
        const val SELECTION_ID = "podcast_list_selection"
    }

    /**
     * Our recycler view adapter using ListAdapter
     */
    private val listAdapter: PodcastListAdapter = PodcastListAdapter()
    /**
     * The selection tracker handle
     */
    private lateinit var selectionTracker: SelectionTracker<String>
    /**
     * The action mode handling long presses
     */
    private var actionMode: ActionMode? = null

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

            // Create the selection tracker and register observers
            selectionTracker = SelectionTracker.Builder<String>(SELECTION_ID,
                    podcast_list,
                    PodcastItemKeyProvider(),
                    PodcastItemDetailsLookup(),
                    StorageStrategy.createStringStorage())
                    .withOnItemActivatedListener { item, _ ->
                        onPodcastSelected(item.position)
                        true
                    }.build()
            selectionTracker.addObserver(PodcastSelectionObserver())
            listAdapter.selectionTracker = selectionTracker
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

            if (savedInstanceState != null)
                selectionTracker.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (!actAsDummy) {
            selectionTracker.onSaveInstanceState(outState)
        }
    }

    private fun onPodcastSelected(position: Int) {
        model.selectedPodcast.value = model.podcasts.value?.get(position)

        // On small screen, use navigation to show episode list
        if (isSmall())
            Navigation.findNavController(activity as AppCompatActivity, R.id.navhost_fragment)
                    .navigate(if (isLandscape()) R.id.nav_action_global_episodes else R.id.nav_action_podcasts_episodes)
    }

    /**
     * The observer to handle selection changes, moves the activity
     * in and out of the action mode
     */
    inner class PodcastSelectionObserver : SelectionTracker.SelectionObserver<String>() {

        override fun onSelectionChanged() {
            if (selectionTracker.hasSelection() && actionMode == null)
                actionMode = activity?.startActionMode(ActionModeController())
            else if (!selectionTracker.hasSelection() && actionMode != null) {
                actionMode?.finish()
                actionMode = null
            }

            val count = selectionTracker.selection.size()
            actionMode?.title = resources.getQuantityString(R.plurals.podcasts, count, count)
        }
    }

    /**
     * Callback for the podcast list context action mode
     */
    inner class ActionModeController() : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.menu_podcastlist_context, menu)

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val podcastToRemove = model.podcasts.value?.filter {
                selectionTracker.selection.contains(it.feed)
            }
            if (podcastToRemove != null)
                model.removePodcast(*podcastToRemove.toTypedArray())

            onDestroyActionMode(mode)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            // This will also make the selection observer finish the mode
            selectionTracker.clearSelection()
        }
    }

    /**
     * Provides a mapping for podcasts to their list position. Feed URLs are used as unique keys.
     */
    inner class PodcastItemKeyProvider() : ItemKeyProvider<String>(ItemKeyProvider.SCOPE_CACHED) {

        override fun getKey(position: Int): String? {
            return model.podcasts.value?.get(position)?.feed
        }

        override fun getPosition(key: String): Int {
            return model.podcasts.value?.indexOfFirst { it.feed == key } ?: -1
        }
    }

    /**
     * Selection tracker helper to find ItemDetails for an input event.
     */
    inner class PodcastItemDetailsLookup() : ItemDetailsLookup<String>() {

        override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
            val view = podcast_list.findChildViewUnder(event.x, event.y)

            if (view != null && podcast_list.getChildViewHolder(view) is PodcastListAdapter.ViewHolder) {
                return (podcast_list.getChildViewHolder(view) as PodcastListAdapter.ViewHolder).getItemDetails()
            }

            return null
        }
    }
}

/**
 * Actual podcast list element details.
 */
private class PodcastItemDetail(private val adapterPosition: Int, private val selectionKey: String)
    : ItemDetailsLookup.ItemDetails<String>() {

    override fun getSelectionKey(): String? {
        return selectionKey
    }

    override fun getPosition(): Int {
        return adapterPosition
    }
}

/**
 * Recycler view adapter for the podcast list. Uses a diff callback to enable nice animations.
 * Selection is handled by the fragment using a selection tracker. The podcast properties are
 * shown using data binding.
 */
private class PodcastListAdapter : ListAdapter<Podcast, PodcastListAdapter.ViewHolder>(diffCallback) {

    lateinit var selectionTracker: SelectionTracker<String>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.podcast_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast, selectionTracker.isSelected(podcast.feed))
    }

    inner class ViewHolder(private val binding: PodcastListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(podcast: Podcast, selected: Boolean) {
            // All the updating is done by Android DataBinding
            binding.setVariable(BR.podcast, podcast)
            binding.executePendingBindings()

            // TODO Make Picasso follow cross-protocol redirects
            Picasso.get().load(podcast.logo).into(binding.root.podcast_logo)

            binding.root.isActivated = selected
        }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<String>? {
            return PodcastItemDetail(adapterPosition, getItem(adapterPosition).feed)
        }
    }

    companion object {
        val diffCallback = object : DiffUtil.ItemCallback<Podcast>() {

            override fun areItemsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
                // Two podcasts are the same when their feed URLs match
                return oldItem.feed == newItem.feed
            }

            override fun areContentsTheSame(oldItem: Podcast, newItem: Podcast): Boolean {
                // TODO Make sure the podcast properties actually shown in the list are compared here
                return oldItem.name == newItem.name &&
                        oldItem.status == oldItem.status &&
                        oldItem.episodes.size == newItem.episodes.size
            }
        }
    }
}