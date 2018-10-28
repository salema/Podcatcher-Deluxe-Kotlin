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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.podcatcher.deluxe.model.PodcastRepository
import com.podcatcher.deluxe.model.types.Podcast

class PutSamplePodcastsWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val TAG by lazy { PutSamplePodcastsWorker::class.java.simpleName }

    private data class SamplePodcast(var name: String, var feed: String)

    override fun doWork(): Result {
        val sampleType = object : TypeToken<List<SamplePodcast>>() {}.type
        var jsonReader: JsonReader? = null

        return try {
            val inputStream = applicationContext.assets.open("sample_podcasts.json")
            jsonReader = JsonReader(inputStream.reader())
            val samples: List<SamplePodcast> = Gson().fromJson(jsonReader, sampleType)

            val repo = PodcastRepository.getInstance(applicationContext)
            samples.forEach { repo.add(Podcast(it.name, it.feed)) }

            Result.SUCCESS
        } catch (ex: Exception) {
            Log.e(TAG, "Error putting sample podcasts to app database", ex)
            Result.FAILURE
        } finally {
            jsonReader?.close()
        }
    }
}
