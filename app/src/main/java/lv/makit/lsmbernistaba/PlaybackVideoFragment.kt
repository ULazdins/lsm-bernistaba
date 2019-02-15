/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package lv.makit.lsmbernistaba

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.leanback.app.VideoSupportFragment
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {
//    private lateinit var mTransportControlGlue: PlaybackTransportControlGlue<MediaPlayerAdapter>

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val item = activity?.intent?.getSerializableExtra(PlaybackActivity.MOVIE) as AudioItem

        object : Thread() {
            override fun run() {
                try {
                    // Connect to the web site
                    val mBlogDocument = Jsoup.connect(item.url).get()
                    // Using Elements to get the Meta data
                    val mElementDataSize: Elements = mBlogDocument
                        .select("source[type=application/x-mpegURL]")

                    val element = mElementDataSize.first()
                    val urlStr: String = element.attr("src")
                    val url = Uri.parse(urlStr)
                    playUrl(url)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()

//        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
//        val playerAdapter = MediaPlayerAdapter(activity)
//        playerAdapter.setRepeatAction(PlaybackControlsRow.RepeatAction.INDEX_NONE)
//
//        mTransportControlGlue = PlaybackTransportControlGlue(getActivity(), playerAdapter)
//        mTransportControlGlue.host = glueHost
//        mTransportControlGlue.title = title
//        mTransportControlGlue.subtitle = description
//        mTransportControlGlue.playWhenPrepared()
//
//        playerAdapter.setDataSource(Uri.parse(videoUrl))
    }

    private fun playUrl(url: Uri) {
        mediaPlayer?.stop()

        mediaPlayer = MediaPlayer.create(activity, url)
        mediaPlayer?.start() // no need to call prepare(); create() does that for you
    }

    override fun onPause() {
        super.onPause()
        
        mediaPlayer?.stop()
//        mTransportControlGlue.pause()
    }
}