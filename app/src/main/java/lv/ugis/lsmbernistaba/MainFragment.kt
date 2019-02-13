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

package lv.ugis.lsmbernistaba

import android.content.Intent
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseFragment
import androidx.leanback.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException
import java.util.*

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : BrowseFragment() {
    private val mHandler = Handler()
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics
    private var mBackgroundTimer: Timer? = null
    private var mBackgroundUri: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        prepareBackgroundManager()

        setupUIElements()


        object : Thread() {
            override fun run() {
                try {
                    // Connect to the web site
                    val mBlogDocument = Jsoup.connect("https://bernistaba.lsm.lv/klausies/3165-merijas-popinsas-dzimsanas-diena").get()
                    // Using Elements to get the Meta data
                    val mElementDataSize: Elements = mBlogDocument.select("a[class=audio-item]")
                    val items: List<AudioItem> = mElementDataSize.map {
                        val url: String = it.attr("href")
                        val imageUrl: String = it
                            .select("div[class=item-thumb]")
                            .attr("style")
                            .removePrefix("background-image:url(")
                            .removeSuffix(")")
                        val title: String = it
                            .select("div[class=item-title]")
                            .text()

                        AudioItem(url, imageUrl, title)
                    }



                    val mainHandler = Handler(activity.getMainLooper());
                    val myRunnable = Runnable {

                        loadRows(items)
                    }
                    mainHandler.post(myRunnable);

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()


        setupEventListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity.window)
        mDefaultBackground = ContextCompat.getDrawable(activity, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(mMetrics)
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        // over title
        headersState = BrowseFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // set fastLane (or headers) background color
        brandColor = ContextCompat.getColor(activity, R.color.fastlane_background)
        // set search icon color
        searchAffordanceColor = ContextCompat.getColor(activity, R.color.search_opaque)
    }

    private fun loadRows(list: List<AudioItem>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val cardPresenter = CardPresenter()


        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        list.forEach { listRowAdapter.add(it) }
        val header = HeaderItem(0, "Audio")
        rowsAdapter.add(ListRow(header, listRowAdapter))

        adapter = rowsAdapter
    }

    private fun setupEventListeners() {
        setOnSearchClickedListener {
            Toast.makeText(activity, "Implement your own in-app search", Toast.LENGTH_LONG)
                .show()
        }

        onItemViewClickedListener = ItemViewClickedListener()
        onItemViewSelectedListener = ItemViewSelectedListener()
    }

    private inner class ItemViewClickedListener : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder,
            row: Row
        ) {
            if (item is AudioItem) {
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

                            val mediaPlayer: MediaPlayer? = MediaPlayer.create(activity, url)
                            mediaPlayer?.start() // no need to call prepare(); create() does that for you
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }.start()
            } else if (item is Movie) {
                Log.d(TAG, "Item: " + item.toString())
                val intent = Intent(activity, DetailsActivity::class.java)
                intent.putExtra(DetailsActivity.MOVIE, item)

                val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    (itemViewHolder.view as ImageCardView).mainImageView,
                    DetailsActivity.SHARED_ELEMENT_NAME
                )
                    .toBundle()
                activity.startActivity(intent, bundle)
            }
        }
    }

    private inner class ItemViewSelectedListener : OnItemViewSelectedListener {
        override fun onItemSelected(
            itemViewHolder: Presenter.ViewHolder?, item: Any?,
            rowViewHolder: RowPresenter.ViewHolder, row: Row
        ) {
            if (item is Movie) {
                mBackgroundUri = item.backgroundImageUrl
                startBackgroundTimer()
            }
        }
    }

    private fun updateBackground(uri: String?) {
        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(activity)
            .load(uri)
            .centerCrop()
            .error(mDefaultBackground)
            .into<SimpleTarget<GlideDrawable>>(
                object : SimpleTarget<GlideDrawable>(width, height) {
                    override fun onResourceReady(
                        resource: GlideDrawable,
                        glideAnimation: GlideAnimation<in GlideDrawable>
                    ) {
                        mBackgroundManager.drawable = resource
                    }
                })
        mBackgroundTimer?.cancel()
    }

    private fun startBackgroundTimer() {
        mBackgroundTimer?.cancel()
        mBackgroundTimer = Timer()
        mBackgroundTimer?.schedule(UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY.toLong())
    }

    private inner class UpdateBackgroundTask : TimerTask() {

        override fun run() {
            mHandler.post { updateBackground(mBackgroundUri) }
        }
    }

    companion object {
        private val TAG = "MainFragment"

        private val BACKGROUND_UPDATE_DELAY = 300
    }
}
