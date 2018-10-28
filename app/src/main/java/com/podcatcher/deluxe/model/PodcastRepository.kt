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

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.*
import com.podcatcher.deluxe.BuildConfig
import com.podcatcher.deluxe.model.daos.EpisodeDao
import com.podcatcher.deluxe.model.daos.PodcastDao
import com.podcatcher.deluxe.model.types.Episode
import com.podcatcher.deluxe.model.types.Podcast
import com.podcatcher.deluxe.model.types.PodcastWithEpisodes
import com.podcatcher.deluxe.workers.LoadPodcastWorker
import com.podcatcher.deluxe.workers.LoadPodcastWorker.Companion.INPUT_FEED
import com.podcatcher.deluxe.workers.LoadPodcastWorker.Companion.INPUT_NAME
import com.podcatcher.deluxe.workers.PutSamplePodcastsWorker
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val DATABASE_NAME = "podcatcher-db"

private val DB_EXECUTOR = Executors.newSingleThreadExecutor()
fun runOnDbThread(f: () -> Unit) {
    DB_EXECUTOR.execute(f)
}

class PodcastRepository private constructor(context: Context) {
    init {
        if (BuildConfig.DEBUG)
        ;//StrictMode.enableDefaults();
    }

    private val TAG by lazy { PodcastRepository::class.java.simpleName }

    companion object {

        private var instance: PodcastRepository? = null

        fun getInstance(context: Context) =
                instance ?: synchronized(this) {
                    instance ?: PodcastRepository(context).also { instance = it }
                }
    }

    private val loadPodcastWorkersTag = "load_podcast"

    private val workManager = WorkManager.getInstance()

    private val db = PodcastDatabase.getInstance(context)

    private val subscriptions = db.podcastDao().getSubscriptions()

    init {
        subscriptions.observeForever { list ->
            Log.i(TAG, "Podcast list changed: $list")
            val LOADER_TAG = "load_podcast"

            // Stop loading removed podcasts
            workManager.getStatusesByTagLiveData(LOADER_TAG).observeForever { statuses ->
                statuses.forEach { status ->
                    Log.i(TAG, "Work ${status.state}: ${status.tags}")

                    /*if (status.state != State.CANCELLED &&
                            !status.tags.any { tag -> list.any { podcast -> podcast.guid == tag}})
                        workManager.cancelWorkById(status.id).also { Log.i(TAG, "Stop updating podcast: ${status.tags}") }*/
                }
            }

            // Add podcasts not already loading
            list.forEach { podcast ->
                workManager.getStatusesByTagLiveData(podcast.guid).observeForever { statuses ->
                    if (statuses.isEmpty()) {
                        Log.i(TAG, "Start updating podcast: ${podcast.name}")

                        val data = workDataOf(INPUT_NAME to podcast.name, INPUT_FEED to podcast.feed)
                        val loadWork = PeriodicWorkRequestBuilder<LoadPodcastWorker>(30, TimeUnit.MINUTES)
                                .setInputData(data).addTag(LOADER_TAG).addTag(podcast.guid).build()

                        workManager.enqueueUniquePeriodicWork(podcast.guid, ExistingPeriodicWorkPolicy.KEEP, loadWork)
                    }
                }
            }
        }
    }

    // Database singleton
    object PodcastDatabase {
        @Volatile
        private var instance: PodcastRoomDatabase? = null

        fun getInstance(context: Context): PodcastRoomDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDb(context).also { instance = it }
            }
        }

        private fun buildDb(context: Context): PodcastRoomDatabase {
            return Room.databaseBuilder(context, PodcastRoomDatabase::class.java, DATABASE_NAME)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)

                            if (BuildConfig.DEBUG) {
                                val request = OneTimeWorkRequestBuilder<PutSamplePodcastsWorker>().build()
                                WorkManager.getInstance().enqueue(request)
                            }
                        }
                    })
                    .build()
        }
    }

    fun getSubcriptions(): LiveData<List<PodcastWithEpisodes>> {
        return db.podcastDao().getSubscriptions()
    }

    fun add(podcast: Podcast) {
        runOnDbThread {
            db.podcastDao().insert(podcast)
        }
    }

    fun remove(podcast: Podcast) {
        runOnDbThread {
            podcast.subscribed = false
            db.podcastDao().update(podcast)
        }
    }

    fun onAppClosed() {
        Log.i(TAG, "App has terminated, podcast repository needs to finish")

        workManager.cancelAllWorkByTag(loadPodcastWorkersTag)
    }
}

@Database(entities = [Podcast::class, Episode::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class PodcastRoomDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
}

class Converters {
    @TypeConverter
    fun dateToLong(date: Date?): Long = date?.time ?: -1L

    @TypeConverter
    fun longToDate(value: Long): Date? = if (value == -1L) null else Date(value)

    @TypeConverter
    fun statusToString(status: Podcast.Status): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): Podcast.Status = Podcast.Status.valueOf(value)
}

/**
 * Skip the entire sub tree the given parser is currently pointing at.
 */
@Throws(XmlPullParserException::class, IOException::class)
fun XmlPullParser.skipSubTree() {
    // We need to see a start tag next. The tag and any sub-tree it might
    // have will be skipped.
    require(XmlPullParser.START_TAG, null, null)

    var level = 1
    // Continue parsing and increase/decrease the level
    while (level > 0) {
        val eventType = next()

        if (eventType == XmlPullParser.END_TAG) --level
        else if (eventType == XmlPullParser.START_TAG) ++level
    }

    // We are back to the original level, behind the start tag given and any
    // sub-tree that might have been there. Return.
}