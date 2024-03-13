package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import coil.request.CachePolicy
import com.app.cascadeos.R
import com.app.cascadeos.adapter.ReactionAdapter
import com.app.cascadeos.databinding.ActivityInteractBinding
import com.app.cascadeos.model.ReactionModel
import com.app.cascadeos.utility.AVTAR_URL
import com.app.cascadeos.utility.ListConstants
import com.app.cascadeos.utility.getURLFromDrawable
import com.app.cascadeos.utility.hideKeyboard
import com.app.cascadeos.utility.showToast
import com.app.cascadeos.viewmodel.MainVM

class InteractActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityInteractBinding
    private val appBarHandler = Handler(Looper.getMainLooper())

    private lateinit var interactAdapter: ReactionAdapter
    private var reactionList = ArrayList<ReactionModel>()
    private var exoPlayer: ExoPlayer? = null

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, InteractActivity::class.java)
            context.startActivity(starter)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interact)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_interact)
        mBinding.layoutMain.layoutTransition = LayoutTransition()

        mBinding.apply {
            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text = getString(R.string.txt_interact)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close).setOnClickListener {
                MainVM.coolTvAppLivedata.postValue(getString(R.string.txt_interact))
                exoPlayer?.release()
                finish()
            }

            layoutHiddenActionbar.setOnClickListener {
                actionBar.actionBar.visibility = View.VISIBLE
                hideAppBar()
            }
            clickListener = View.OnClickListener {
                when (it.id) {
                    R.id.fabChat -> {
                        reactionsView.visibility = View.VISIBLE
                        fabChat.visibility = View.GONE
                    }

                    R.id.imgCloseReactions -> {
                        reactionsView.visibility = View.GONE
                        fabChat.visibility = View.VISIBLE
                    }

                    R.id.imgReact -> {
                        showReactionAnimation()
                        playTone(R.raw.cooltv_apps)
                    }

                    R.id.imgSend -> {
                        if (edtReaction.text.toString().trim().isEmpty()) {
                            showToast(getString(R.string.please_enter_message))
                        } else {
                            interactAdapter.addMessage(
                                ReactionModel(
                                    profileImage = R.drawable.img_interact_profile,
                                    name = getString(R.string.simmy_jackson),
                                    message = edtReaction.text.toString().trim()
                                )
                            )
                            rvReaction.scrollToPosition(interactAdapter.itemCount - 1)
                            edtReaction.text?.clear()
                        }
                        hideKeyboard()
                        edtReaction.clearFocus()
                    }
                }
            }

            reactionsView.viewTreeObserver.addOnGlobalLayoutListener {
                val r = Rect()
                root.getWindowVisibleDisplayFrame(r)
                val screenHeight: Int = root.rootView.height

                val heightDifference = (screenHeight - r.bottom - r.top) - (screenHeight - mBinding.root.height)
                if (heightDifference > 10) {
                    mBinding.layoutContent.setPadding(0, 0, 0, heightDifference)

                } else {
                    mBinding.layoutContent.setPadding(0, 0, 0, 0)
                }
            }

            layoutMain.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    hideKeyboard()
                }
                return@setOnTouchListener true
            }
        }
        setupRecyclerview()
        setupVideoPlayer()
        hideAppBar()
    }

    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    private fun showReactionAnimation() {
        val gifPath = getURLFromDrawable(R.raw.reaction_animation)
        val imageLoader = ImageLoader.Builder(this).components {
            if (SDK_INT >= VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }.diskCachePolicy(CachePolicy.ENABLED).build()
        mBinding.imgReactionView.load(gifPath, imageLoader = imageLoader)
        mBinding.imgReactionView.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            mBinding.imgReactionView.visibility = View.GONE
        }, 5000)
    }

    private fun setupVideoPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().also { ePlayer ->
            mBinding.exoPlayerView.player = ePlayer
            val mediaItem = MediaItem.fromUri(AVTAR_URL)
            ePlayer.setMediaItem(mediaItem)
            ePlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            ePlayer.prepare()
            ePlayer.play()
        }

        exoPlayer?.addListener(@UnstableApi object : Player.Listener {
            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playbackState == ExoPlayer.STATE_BUFFERING || playbackState == ExoPlayer.STATE_IDLE) {
                    mBinding.progressBarVideoLoading.visibility = View.VISIBLE
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    mBinding.progressBarVideoLoading.visibility = View.GONE
                }
            }
        })
    }

    private fun setupRecyclerview() {
        Handler(Looper.getMainLooper()).postDelayed({
            reactionList = ListConstants.getReactionList()
            interactAdapter = ReactionAdapter(this, reactionList)
            mBinding.rvReaction.adapter = interactAdapter

            mBinding.reactionsView.visibility = View.VISIBLE
        }, 2000)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            mBinding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}