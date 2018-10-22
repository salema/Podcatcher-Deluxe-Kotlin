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
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
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
import kotlinx.android.synthetic.deluxe.podcast_list_item.view.*

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
     * If this fragment instance works in dummy mode (see above). For this to work
     * in all cases, it is important for actAsDummy to default to true.
     */
    private var actAsDummy: Boolean = true

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
        inflater?.inflate(R.menu.menu_podcastlist, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_action_add_podcast -> {
                model.addPodcast()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!actAsDummy) {
            // Prepare recyclerview
            with(podcast_list) {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(activity)
                addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
                adapter = listAdapter
            }

            // Create the selection tracker and register observers
            selectionTracker = SelectionTracker.Builder<String>(SELECTION_ID,
                    podcast_list,
                    PodcastItemKeyProvider(),
                    PodcastItemDetailsLookup(),
                    StorageStrategy.createStringStorage())
                    .withOnItemActivatedListener { item, _ ->
                        model.selectPodcast(model.podcasts.value?.get(item.position))

                        // On small screen, use navigation to show episode list
                        if (isSmall())
                            navigate(if (isLandscape()) R.id.nav_action_global_episodes else R.id.nav_action_podcasts_episodes)

                        true
                    }.build()
            selectionTracker.addObserver(PodcastSelectionObserver())

            // Prepare list adapter
            listAdapter.showLogos = !isLandscape()
            listAdapter.highlightLastTapped = !isSmall() || isLandscape()
            listAdapter.selectionTracker = selectionTracker

            // Big, non-inline logos are only shown on large screens in landscape
            podcast_logo_large.visibility = if (!isSmall() && isLandscape()) VISIBLE else GONE
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!actAsDummy) {
            model.selectedPodcast.observe(this, Observer {
                onPodcastSelected(it)
            })
            model.podcasts.observe(this, Observer<List<Podcast>> {
                // This will update the podcast list with nice animations
                listAdapter.submitList(it)
            })

            if (savedInstanceState != null && !isSmall())
                selectionTracker.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()

        // Here our dummy helps us: if he comes back, no podcast is selected any longer!
        if (actAsDummy)
            model.selectPodcast(null)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Restoring selection will only work on large devices, since
        // small devices have two podcast list fragments
        if (!actAsDummy && !isSmall())
            selectionTracker.onSaveInstanceState(outState)
    }

    private fun onPodcastSelected(podcast: Podcast?) {
        // This will make sure highlight and logo survive configuration changes as needed
        val oldPosition = model.podcasts.value?.indexOf(listAdapter.lastTappedPodcast) ?: -1
        val newPosition = model.podcasts.value?.indexOf(podcast) ?: -1
        listAdapter.lastTappedPodcast = podcast
        listAdapter.notifyItemChanged(oldPosition)
        listAdapter.notifyItemChanged(newPosition)

        // On large screens, show/reset podcast logo
        if (isLandscape() && podcast != null)
            Picasso.get().load(podcast.logo)
                    .placeholder(R.drawable.default_podcast_logo)
                    .error(R.drawable.default_podcast_logo)
                    .noFade().fit().into(podcast_logo_large)
        else if (isLandscape())
            podcast_logo_large.setImageResource(R.drawable.default_podcast_logo)
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
    inner class ActionModeController : ActionMode.Callback {

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
            podcastToRemove?.let { model.removePodcast(*podcastToRemove.toTypedArray()) }

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
    inner class PodcastItemKeyProvider : ItemKeyProvider<String>(ItemKeyProvider.SCOPE_CACHED) {

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
    inner class PodcastItemDetailsLookup : ItemDetailsLookup<String>() {

        override fun getItemDetails(event: MotionEvent): ItemDetails<String>? {
            val view = podcast_list.findChildViewUnder(event.x, event.y)
            return view?.let {
                (podcast_list.getChildViewHolder(view) as PodcastListAdapter.ViewHolder).getItemDetails()
            }
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

    /**
     * The selection tracker used to follow item activation
     */
    lateinit var selectionTracker: SelectionTracker<String>

    /**
     * If true, podcast logo image views will be shown
     */
    var showLogos = false
    /**
     * If true, the position representing the selected podcast is highlighted
     */
    var highlightLastTapped = true
    /**
     * The podcast last tapped, update this to make highlighting work
     */
    var lastTappedPodcast: Podcast? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                R.layout.podcast_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val podcast = getItem(position)
        holder.bind(podcast)

        with(holder.itemView) {
            isActivated = selectionTracker.isSelected(podcast.feed)

            val highlight = highlightLastTapped && podcast == lastTappedPodcast
            background = ResourcesCompat.getDrawable(resources,
                    if (highlight) R.drawable.podcast_list_item_highlight
                    else R.drawable.podcast_list_item_background, null)
        }
    }

    inner class ViewHolder(private val binding: PodcastListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(podcast: Podcast) {
            // All the updating is done by Android DataBinding
            binding.setVariable(BR.podcast, podcast)
            binding.executePendingBindings()

            with(binding.root) {
                // Apply some extra padding if the view would visually collide
                // with the scrollbars otherwise, i.e. the logo image view is hidden
                podcast_new_count.setPadding(0, 0, if (showLogos) 0 else 8, 0)
                podcast_progress.setPadding(0, 0, if (showLogos) 0 else 8, 0)
                podcast_logo.visibility = if (showLogos) VISIBLE else GONE
                if (showLogos)
                    Picasso.get().load(podcast.logo).fit().into(podcast_logo)
            }
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