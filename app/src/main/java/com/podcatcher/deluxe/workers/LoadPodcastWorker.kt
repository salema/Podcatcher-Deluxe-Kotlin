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

package com.podcatcher.deluxe.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.podcatcher.deluxe.model.PodcastRepository
import com.podcatcher.deluxe.model.types.Podcast
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream

class LoadPodcastWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG by lazy { LoadPodcastWorker::class.java.simpleName }

    companion object {
        const val INPUT_NAME = "key_podcast_name"
        const val INPUT_FEED = "key_podcast_feed"

        private val client = OkHttpClient()
    }

    override fun doWork(): Result {
        val name = inputData.getString(INPUT_NAME)
        val feed = inputData.getString(INPUT_FEED)!!

        Log.i(TAG, "Loading $name from $feed")

        val db = PodcastRepository.PodcastDatabase.getInstance(applicationContext)
        val podcast = db.podcastDao().getPodcastForFeed(feed)

        podcast?.let {
            podcast.status = Podcast.Status.LOADING
            db.podcastDao().update(podcast)

            return try {
                val request = Request.Builder().url(feed).build()
                val response = client.newCall(request).execute()
                val feedContent = response.body()?.bytes()

                if (feedContent != null && podcast != null) {
                    Log.i(TAG, "Loaded feed for $name: ${feedContent.size} bytes")

                    val parserFactory = XmlPullParserFactory.newInstance()
                    parserFactory.isNamespaceAware = true

                    val parser = parserFactory.newPullParser()
                    parser.setInput(ByteArrayInputStream(feedContent), null)
                    val episodes = podcast.parse(parser)
                    Log.i(TAG, "Loaded podcast: ${podcast.name}, found ${episodes.size} episodes")
                    db.episodeDao().insert(*episodes.toTypedArray())

                    podcast.fileSize = feedContent.size.toLong()
                    podcast.status = Podcast.Status.READY
                    db.podcastDao().update(podcast)
                }

                Log.i(TAG, "Loaded podcast: ${podcast.name}")

                return Result.SUCCESS
            } catch (ex: Exception) {
                podcast.status = Podcast.Status.FAILED
                db.podcastDao().update(podcast)

                Log.e(TAG, "Error loading podcast $name", ex)
                return Result.FAILURE
            }
        }

        return Result.FAILURE
    }
}
