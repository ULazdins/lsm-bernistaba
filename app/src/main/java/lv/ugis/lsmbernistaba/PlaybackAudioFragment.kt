package lv.ugis.lsmbernistaba

import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.core.content.ContextCompat
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.PlaybackSupportFragment
import androidx.leanback.app.PlaybackSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.media.PlaybackTransportControlGlue
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import java.io.IOException


class PlaybackAudioFragment: PlaybackSupportFragment() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mBackgroundManager: BackgroundManager
    private var mDefaultBackground: Drawable? = null
    private lateinit var mMetrics: DisplayMetrics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prepareBackgroundManager()

        val item = activity?.intent?.getSerializableExtra(PlaybackActivity.MOVIE) as AudioItem

        val playerGlue = PlaybackTransportControlGlue(
            activity,
            MediaPlayerAdapter(activity)
        )
        playerGlue.host = PlaybackSupportFragmentGlueHost(this)
        playerGlue.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
            override fun onPreparedStateChanged(glue: PlaybackGlue) {
                if (glue.isPrepared()) {
//                    playerGlue.setSeekProvider(MySeekProvider())
                    playerGlue.play()
                }
            }
        })
        playerGlue.isControlsOverlayAutoHideEnabled = false
        playerGlue.subtitle = "Latvijas Radio Teātris"
        playerGlue.title = item.title

        val width = mMetrics.widthPixels
        val height = mMetrics.heightPixels
        Glide.with(activity)
            .load(item.imageUrl)
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

                    playerGlue.getPlayerAdapter().setDataSource(url)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    override fun onPause() {
        super.onPause()

        mediaPlayer?.stop()
    }


    private fun prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(activity)
        mBackgroundManager.attach(activity!!.window)
        mDefaultBackground = ContextCompat.getDrawable(context!!, R.drawable.default_background)
        mMetrics = DisplayMetrics()
        activity!!.windowManager.defaultDisplay.getMetrics(mMetrics)
    }
}