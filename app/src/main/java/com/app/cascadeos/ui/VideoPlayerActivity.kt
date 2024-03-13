package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityVideoPlayerBinding
import com.app.cascadeos.utility.COOL_TV_VIDEO_URL

@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityVideoPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        fun start(context: Context, videoUrl: String?) {
            val starter = Intent(context, VideoPlayerActivity::class.java)
            starter.putExtra("url", videoUrl)
            context.startActivity(starter)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        hideAppBar()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_video_player)
        mBinding.layoutMain.layoutTransition = LayoutTransition()

        mBinding.apply {
            layoutHiddenActionbar.setOnClickListener {
                imgBack.visibility = View.VISIBLE
                hideAppBar()
            }
            videoUrl = intent.getStringExtra("url")

            exoPlayer = ExoPlayer.Builder(this@VideoPlayerActivity).build().also { ePlayer ->
                exoPlayerView.player = ePlayer
                val mediaItem =
                    if (videoUrl != null) {
                        MediaItem.fromUri(videoUrl.toString())
                    } else {
                        MediaItem.fromUri(COOL_TV_VIDEO_URL)
                    }
                ePlayer.setMediaItem(mediaItem)
                ePlayer.prepare()
                ePlayer.play()
            }

            imgBack.setOnClickListener { finish() }

            exoPlayer?.addListener(object : Player.Listener {
                @Deprecated("Deprecated in Java")
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    if (playbackState == ExoPlayer.STATE_READY) {
                        progressBarVideoLoading.visibility = View.GONE
                    } else if (playbackState == ExoPlayer.STATE_BUFFERING || playbackState == ExoPlayer.STATE_IDLE) {
                        progressBarVideoLoading.visibility = View.VISIBLE
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.stop()
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            mBinding.imgBack.visibility = View.GONE
        }, 3000)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}