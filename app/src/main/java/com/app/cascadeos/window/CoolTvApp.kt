package com.app.cascadeos.window

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.constraintlayout.widget.Constraints.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.app.cascadeos.R
import com.app.cascadeos.adapter.CoolTvAppAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutCoolTvBinding
import com.app.cascadeos.interfaces.BidListener
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.CoolTvAppsModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.InteractActivity
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.*
import com.app.cascadeos.utility.ListConstants.getBidList
import com.app.cascadeos.utility.ListConstants.getBidTimes
import com.app.cascadeos.utility.ListConstants.getCoolTvAppsList
import com.app.cascadeos.window.BidApp.Companion.myBidAmount
import java.util.concurrent.TimeUnit


@UnstableApi
class CoolTvApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle, private val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    var isResume: Boolean,
    configurationLiveData: MutableLiveData<Configuration?>,
    private val coolTvAppLivedata: MutableLiveData<String?>,
) : WindowApp<LayoutCoolTvBinding>(
    context, lifecycle, R.layout.layout_cool_tv, mBinding, configurationLiveData
) {
    private var mHandler: Handler? = null
    private var isMediaPlaying = true
    private var exoPlayer: ExoPlayer? = null
    private var bidPosition: Int = 0
    private lateinit var introMediaPlayer: MediaPlayer

    private lateinit var coolTvAppAdapter: CoolTvAppAdapter
    private var coolTvAppsList = ArrayList<CoolTvAppsModel>()

    val exoPlayerHandler = Handler(Looper.getMainLooper())
    private var bidTimeCount = 0
    private lateinit var bidListener: BidListener

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    private val updateTimeTask: Runnable = object : Runnable {
        override fun run() {
            exoPlayer?.currentPosition?.toInt()?.let { binding.seekbarVideo.progress = it }
            mHandler!!.postDelayed(this, 1000)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isMediaPlaying = true
        binding.imgIconPlay.performClick()
    }

    fun formatTime(timeMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()
        mHandler = Handler(Looper.getMainLooper())
        binding.imgBidSent.visibility = View.GONE
        coolTvAppsList = getCoolTvAppsList()
        buildVideoPlayer()
        coolTvAppAdapter = CoolTvAppAdapter(context, coolTvAppsList, onAppClick = { _, position ->
            if (!binding.exoPlayerView.isVisible) {
                binding.exoPlayerView.visibility = View.VISIBLE
                binding.introAnimationLayout.visibility = View.GONE
            }
            playTone(R.raw.cooltv_apps)
            completeIntroView()
            when (position) {
                0 -> {
                    pauseVideoOnAppLaunch()
                    (context as MainActivity).startBuyVideoApp(
                        app = AppDetailModel(
                            name = context.getString(R.string.txt_click_video_shop),
                            startX = context.resources.getInteger(R.integer.c_start),
                            width = context.resources.getInteger(R.integer.c_width),
                            gravity = Gravity.END
                        )
                    )
                }

                1 -> {
                    pauseVideoOnAppLaunch()
                    (context as MainActivity).startLinkApp(
                        app = AppDetailModel(
                            name = context.getString(R.string.txt_link),
                            startX = context.resources.getInteger(R.integer.c_start),
                            width = context.resources.getInteger(R.integer.c_width),
                            webUrl = AVATAR_WEB_URL,
                            gravity = Gravity.END
                        )
                    )
                }

                2 -> {
                    pauseVideoOnAppLaunch()
                    (context as MainActivity).startEntertainApp(
                        app = AppDetailModel(
                            name = context.getString(R.string.txt_entertain),
                            startX = context.resources.getInteger(R.integer.c_start),
                            width = context.resources.getInteger(R.integer.c_width),
                            webUrl = AVTAR_IMDB_URL,
                            gravity = Gravity.END
                        )
                    )
                }

                3 -> {
                    pauseVideoOnAppLaunch()

                    (context as MainActivity).startCoolECallApp(
                        app = AppDetailModel(
                            name = context.getString(R.string.txt_cool_Ecall),
                            startX = context.resources.getInteger(R.integer.a_start),
                            width = context.resources.getInteger(R.integer.a_width),
                            gravity = Gravity.START
                        )
                    )
                }

                4 -> {
                    pauseVideoOnAppLaunch()
                    InteractActivity.start(context)
                }

                5 -> {
                    val mediaItem = MediaItem.fromUri(BID_VIDEO_URL)
                    /*if (exoPlayer == null) {
                        buildVideoPlayer()
                    }*/
                    exoPlayer?.setMediaItem(mediaItem)
                    exoPlayer?.play()
                    binding.seekbarVideo.progress = 0
                    (context as MainActivity).startBidApp(
                        app = AppDetailModel(
                            name = context.getString(R.string.txt_bid),
                            startX = context.resources.getInteger(R.integer.c_start),
                            width = context.resources.getInteger(R.integer.c_width),
                            gravity = Gravity.END
                        )
                    ) {
                        pauseVideoOnAppLaunch()
                        showBidMessages()
                        binding.imgBidSent.visibility = View.VISIBLE
                        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, R.raw.chears)
                        mediaPlayer.setVolume(0.5f, 0.5f)
                        mediaPlayer.start()
                        mediaPlayer.setOnCompletionListener {
                            mediaPlayer.release()
                            binding.imgBidSent.visibility = View.GONE
                            resumeVideoOnAppLaunch()
                        }
                    }
                    getBidTimesFromVideo()
                }
            }
            setBlinkAnimationToCard(position)
        })
        binding.rvCoolTvApps.adapter = coolTvAppAdapter

        //binding.imgIntro.startAnimation(AnimationUtils.loadAnimation(context, R.anim.intro_top_to_bottom))
        val animation = AnimationUtils.loadAnimation(context, R.anim.intro_top_to_bottom)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                playIntroMusic()
            }

            override fun onAnimationEnd(animation: Animation?) {
                // add blow animation for IntroText here
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        binding.imgIntro.startAnimation(animation)
        binding.tvIntro.animateText(context.getString(R.string.coolTvNetworkTm))
        binding.tvIntro.setCharacterDelay(120)


        attachVideoDoubleTapListener()
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutContent.layoutTransition = layoutTransition

        binding.seekbarVideo.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(
                seekBar: SeekBar, progress: Int,
                fromUser: Boolean,
            ) {
                if (fromUser) {
                    exoPlayer?.seekTo(progress.toLong())
                }
            }
        })

        coolTvAppLivedata.observe((context as MainActivity)) { appName ->
            if (appName != null) {
                try {
                    // When click on buy now button only clear blink animation not play video
                    if (appName == context.getString(R.string.txt_click_video_shop) + "1") {
                        clearBlinkAnimation()
                        coolTvAppsList[getPositionFromName(appName.removeSuffix("1"))].isSelected = false
                        coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName.removeSuffix("1")))
                    } else {
                        when (appName) {
                            context.getString(R.string.txt_click_video_shop) -> {
                                clearBlinkAnimation()
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                                //binding.imgIconPlay.performClick()
                            }

                            context.getString(R.string.digital_locker) -> {/*if (!isMediaPlaying) {
                                binding.imgIconPlay.performClick()
                            }*/
                                isMediaPlaying = false
                            }

                            context.getString(R.string.purchase_video) -> {
                                isMediaPlaying = false
                            }

                            context.getString(R.string.txt_link) -> {
                                clearBlinkAnimation()
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                                //binding.imgIconPlay.performClick()
                            }

                            context.getString(R.string.txt_entertain) -> {
                                clearBlinkAnimation()
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                                //binding.imgIconPlay.performClick()
                            }

                            context.getString(R.string.txt_cool_Ecall) -> {
                                clearBlinkAnimation()
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                            }

                            context.getString(R.string.txt_interact) -> {
                                clearBlinkAnimation()
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                            }

                            context.getString(R.string.txt_bid) -> {
                                clearBlinkAnimation()
                                val mediaItem = MediaItem.fromUri(AVTAR_URL)
                                exoPlayer?.setMediaItem(mediaItem)
                                binding.seekbarVideo.progress = 0
                                coolTvAppsList[getPositionFromName(appName)].isSelected = false
                                coolTvAppAdapter.notifyItemChanged(getPositionFromName(appName))
                                exoPlayerHandler.removeCallbacksAndMessages(null)
                                binding.bidCountView.visibility = View.GONE
                                showCongratulationMessage(false)
                            }
                        }

                        when (getSelectedApps().size) {
                            0 -> {
                                resumeVideoOnAppLaunch()
                                /*if (appName == context.getString(R.string.txt_bid)) {
                                    pauseVideoOnAppLaunch()
                                } else {
                                    resumeVideoOnAppLaunch()
                                }*/
                            }

                            1 -> {
                                if (getSelectedApps().any { it.name == R.string.txt_bid }) {
                                    resumeVideoOnAppLaunch()
                                    setBlinkAnimationToCard(getPositionFromName(context.getString(getSelectedApps()[0].name)))
                                } else {
                                    setBlinkAnimationToCard(getPositionFromName(context.getString(getSelectedApps()[0].name)))
                                }
                            }

                            else -> {
                                setBlinkAnimationToCard(-1)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }


        binding.clickListener = View.OnClickListener {
            when (it.id) {
                R.id.img_icon_previous -> {
                    exoPlayer?.currentPosition?.minus(10000)?.let { it1 -> exoPlayer?.seekTo(it1) }
                }

                R.id.img_icon_play -> {
                    exoPlayer?.let { exoPlayer ->
                        if (exoPlayer.currentPosition == exoPlayer.duration || exoPlayer.duration.minus(
                                exoPlayer.currentPosition
                            ) <= 2
                        ) {
                            exoPlayer.seekTo(0)
                        }
                        if (isMediaPlaying) {
                            binding.imgIconPlay.setImageDrawable(
                                ActivityCompat.getDrawable(
                                    context, R.drawable.icon_play_with_shine
                                )
                            )
                            isMediaPlaying = false
                            pauseVideo()
                        } else {
                            binding.imgIconPlay.setImageDrawable(
                                ActivityCompat.getDrawable(
                                    context, R.drawable.icon_pause_with_bg
                                )
                            )
                            isMediaPlaying = true
                            resumeVideo()
                        }
                    }
                }

                R.id.img_icon_stop -> {
                    if (isMediaPlaying) {
                        binding.imgIconPlay.setImageDrawable(
                            ActivityCompat.getDrawable(
                                context, R.drawable.icon_play_with_shine
                            )
                        )
                        isMediaPlaying = false
                        pauseVideo()
                    }
                    exoPlayer?.duration?.let { it1 -> exoPlayer?.seekTo(it1) }
                }

                R.id.img_icon_next -> {
                    exoPlayer?.currentPosition?.plus(10000)?.let { it1 -> exoPlayer?.seekTo(it1) }
                }

                else -> {

                }
            }
        }
        manageCloseButton()
    }

    private fun buildVideoPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build().also { ePlayer ->
            binding.exoPlayerView.player = ePlayer
            val mediaItem = MediaItem.fromUri(AVTAR_URL)
            ePlayer.setMediaItem(mediaItem)
            ePlayer.repeatMode = ExoPlayer.REPEAT_MODE_ALL
            ePlayer.playWhenReady = false
            binding.imgIconPlay.setImageDrawable(
                ActivityCompat.getDrawable(
                    context, R.drawable.icon_pause_with_bg
                )
            )
            ePlayer.addListener(object : Player.Listener {
                @Deprecated("Deprecated in Java")
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    super.onPlayerStateChanged(playWhenReady, playbackState)
                    when (playbackState) {
                        ExoPlayer.STATE_BUFFERING -> {
                            binding.progressBarVideoLoading.visibility = View.VISIBLE
                        }

                        ExoPlayer.STATE_READY -> {
                            exoPlayer?.duration?.toInt()?.let {
                                binding.seekbarVideo.max = it
                            }
                            binding.progressBarVideoLoading.visibility = View.GONE
                            if (isResume) {
                                exoPlayer?.seekTo(TimeUnit.MILLISECONDS.convert(40, TimeUnit.SECONDS))
                                isResume = false
                            }
                        }

                        else -> {}
                    }
                }
            })
            ePlayer.prepare()
            //ePlayer.play()
            updateProgressBar()
        }
    }

    private fun getBidTimesFromVideo() {
        bidListener = (context as MainActivity).mainVM.bidApp as BidListener
        exoPlayerHandler.post(object : Runnable {
            override fun run() {
                if (coolTvAppsList.last().isSelected) {
                    val currentPosStr = exoPlayer?.currentPosition?.let { formatTime(it) }
                    if (currentPosStr.equals("03:35")) {
                        showCongratulationMessage(true)
                    } else if (currentPosStr.equals("03:45")) {
                        showCongratulationMessage(false)
                        bidTimeCount = 0
                        bidPosition = 0
                        myBidAmount = "0"
                        binding.bidCountView.visibility = View.GONE
                        bidListener.onBidMade(bidPosition)
                    } else {
                        if (getBidTimes().contains(currentPosStr)) {
                            bidTimeCount = getBidTimes().indexOf(currentPosStr)
                            bidPosition = bidTimeCount
                        }
                        if (currentPosStr.equals(getBidTimes()[bidTimeCount])) {
                            showBidMessages()
                            if (bidPosition == getBidList().size - 1) {
                                bidPosition = 0
                                bidTimeCount = 0
                            }
                            bidTimeCount += 1
                            bidPosition++
                        }
                    }
                    exoPlayerHandler.postDelayed(this, 1000)
                } else {
                    binding.bidCountView.visibility = View.GONE
                }
            }
        })
    }

    fun showCongratulationMessage(show: Boolean) {
        if (show) {
            binding.congratulationLayout.visibility = View.VISIBLE
            if (Integer.parseInt(myBidAmount) >= Integer.parseInt(getBidList().last().amount)) {
                binding.tvWinnerName.text = context.getString(R.string.simmy_jackson)
                binding.tvWinnerAmount.text = "$${myBidAmount} Million"
            } else {
                binding.tvWinnerName.text = getBidList().last().name
                binding.tvWinnerAmount.text = "$${getBidList().last().amount} Million"
            }
        } else {
            binding.congratulationLayout.visibility = View.GONE
        }
    }

    private fun showBidMessages() {
        val bidList = getBidList()
        if (!binding.bidCountView.isVisible) {
            binding.bidCountView.visibility = View.VISIBLE
        }
        if (Integer.parseInt(getBidList()[bidPosition].amount) < Integer.parseInt(myBidAmount)) {
            binding.bidUserName = context.getString(R.string.simmy_jackson)
            binding.bidAmount = "$${myBidAmount} Million"
        } else {
            binding.bidUserName = bidList[bidPosition].name
            binding.bidAmount = "$${bidList[bidPosition].amount} Million"
        }
        bidListener.onBidMade(bidPosition)
    }

    private fun pauseVideoOnAppLaunch() {
        exoPlayer?.let { exoPlayer ->
            if (exoPlayer.currentPosition == exoPlayer.duration || exoPlayer.duration.minus(
                    exoPlayer.currentPosition
                ) <= 2
            ) {
                exoPlayer.seekTo(0)
            }
            binding.imgIconPlay.setImageDrawable(
                ActivityCompat.getDrawable(
                    context, R.drawable.icon_play_with_shine
                )
            )
            isMediaPlaying = false
            pauseVideo()
        }
    }

    private fun resumeVideoOnAppLaunch() {
        exoPlayer?.let { exoPlayer ->
            if (exoPlayer.currentPosition == exoPlayer.duration || exoPlayer.duration.minus(
                    exoPlayer.currentPosition
                ) <= 2
            ) {
                exoPlayer.seekTo(0)
            }
            binding.imgIconPlay.setImageDrawable(
                ActivityCompat.getDrawable(
                    context, R.drawable.icon_pause_with_bg
                )
            )
            isMediaPlaying = true
            resumeVideo()
        }
    }

    private fun getPositionFromName(name: String): Int {
        for (i in coolTvAppsList.indices) {
            if (name == context.getString(coolTvAppsList[i].name)) {
                return i
            }
        }
        return -1
    }

    private fun getSelectedApps(): ArrayList<CoolTvAppsModel> {
        val list = ArrayList<CoolTvAppsModel>()
        for (i in coolTvAppsList.indices) {
            if (coolTvAppsList[i].isSelected) {
                list.add(coolTvAppsList[i])
            }
        }
        return list
    }

    /*private fun getSelectedAppCount(): Int {
        var selectedCount = 0
        for (i in coolTvAppsList.indices) {
            if (coolTvAppsList[i].isSelected) {
                selectedCount++
            }
        }
        return selectedCount
    }*/

    private fun clearBlinkAnimation() {
        val lt = (binding.highlightView.parent as ViewGroup).layoutTransition
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        binding.highlightView.visibility = View.GONE
        lt.enableTransitionType(LayoutTransition.DISAPPEARING)
        binding.highlightView.clearAnimation()
    }

    private fun completeIntroView() {
        introMediaPlayer.release()
        binding.introAnimationLayout.animate()
            .alpha(0.0f).duration = 1000
        exoPlayer?.play()

        binding.exoPlayerView.animate().alpha(1.0F).setDuration(1000).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
            }
        })
    }

    private fun playIntroMusic() {
        introMediaPlayer = MediaPlayer.create(context, R.raw.cool_tv_intro)
        introMediaPlayer.setVolume(0.5f, 0.5f)
        introMediaPlayer.start()
        introMediaPlayer.setOnCompletionListener {
            completeIntroView()
        }
    }

    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    private fun setBlinkAnimationToCard(position: Int) {
        val animation = AnimationUtils.loadAnimation(context, R.anim.blink_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        when (position) {
            0 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_clickVideo_blow)
            }

            1 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_link_blow)
            }

            2 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_entertain_blow)
            }

            3 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_coolCall_blow)
            }

            4 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_interact_blow)
            }

            5 -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_bid_blow)
            }

            else -> {
                binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_multiple_blow)
            }
        }
        if (position != -1) {
            coolTvAppsList[position].isSelected = true
        }
        binding.highlightView.visibility = View.VISIBLE
        binding.highlightView.startAnimation(animation)
        coolTvAppAdapter.notifyItemChanged(position)
        if (getSelectedApps().size > 1) {
            binding.highlightView.backgroundTintList = context.getColorStateList(R.color.color_multiple_blow)
        }
    }

    private fun manageCloseButton() {
        binding.layoutHiddenActionbarClose.setOnClickListener {
            if (binding.rvCoolTvApps.visibility == View.GONE) {
                if (binding.imgBack.visibility == View.GONE) {
                    binding.imgBack.visibility = View.VISIBLE
                    hideAppBar()
                } else {
                    binding.imgBack.visibility = View.GONE
                }
            }
        }
        binding.imgBack.setOnClickListener {
            binding.imgBack.visibility = View.GONE
            doubleTapped()
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.imgBack.visibility = View.GONE
        }, 3000)
    }

    private fun attachVideoDoubleTapListener() {
        binding.exoPlayerView.setOnTouchListener(object : OnTouchListener {
            private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    doubleTapped()
                    return super.onDoubleTap(e)

                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (binding.rvCoolTvApps.visibility == View.GONE) {
                        if (binding.exoPlayerView.isControllerFullyVisible) {
                            binding.exoPlayerView.hideController()
                        } else {
                            binding.exoPlayerView.showController()
                        }
                    }
                    return super.onSingleTapConfirmed(e)
                }
            })

            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                gestureDetector.onTouchEvent(event!!)
                return true
            }
        })
    }

    private fun doubleTapped() {
        if (binding.rvCoolTvApps.visibility == View.VISIBLE) {
            binding.rvCoolTvApps.visibility = View.GONE
            binding.playerWidgetGroup.visibility = View.GONE
            binding.layoutHiddenActionbar.visibility = View.GONE
            binding.isFullscreen = true
            binding.layoutMain.background = null
            binding.layoutMain.setBackgroundColor(
                ContextCompat.getColor(
                    context, R.color.black
                )
            )
            binding.cardViewVideoBg.setMargins(
                leftMarginDp = 0, topMarginDp = 0, rightMarginDp = 0, bottomMarginDp = 0
            )
            binding.mainCard.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0f, context.resources.displayMetrics
            )
            binding.cardViewVideoBg.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0f, context.resources.displayMetrics
            )
            binding.exoPlayerView.useController = true
            toggleLayoutFullScreen(true)
        } else {
            binding.layoutHiddenActionbar.visibility = View.VISIBLE
            binding.rvCoolTvApps.visibility = View.VISIBLE
            binding.playerWidgetGroup.visibility = View.VISIBLE
            binding.layoutMain.background = ContextCompat.getDrawable(context, R.drawable.img_bg_green)
            binding.isFullscreen = false
            binding.cardViewVideoBg.setMargins(
                leftMarginDp = 10, topMarginDp = 10, rightMarginDp = 10, bottomMarginDp = 10
            )
            binding.mainCard.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
            )
            binding.cardViewVideoBg.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 13f, context.resources.displayMetrics
            )
            binding.exoPlayerView.useController = false
            toggleLayoutFullScreen(false)

        }
    }

    private fun View.setMargins(
        leftMarginDp: Int? = null,
        topMarginDp: Int? = null,
        rightMarginDp: Int? = null,
        bottomMarginDp: Int? = null,
    ) {
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            val params = layoutParams as ViewGroup.MarginLayoutParams
            leftMarginDp?.run { params.leftMargin = this.dpToPx(context).toInt() }
            topMarginDp?.run { params.topMargin = this.dpToPx(context).toInt() }
            rightMarginDp?.run { params.rightMargin = this.dpToPx(context).toInt() }
            bottomMarginDp?.run { params.bottomMargin = this.dpToPx(context).toInt() }
            requestLayout()
        }
    }


    private fun pauseVideo() {
        exoPlayer?.playWhenReady = false
    }

    private fun toggleLayoutFullScreen(makeFullScreen: Boolean) {
        if (!makeFullScreen) {
            defaultMode()
        } else {
            fullScreenMode()
        }
    }

    private fun defaultMode() {
        screen = Screen.DEFAULT
        floatWindowLayoutParam.width = getDetailsModel().width.dpToPx(context).toInt()
        if (!isMiniMize) {
            floatWindowLayoutParam.height = getDetailsModel().height.dpToPx(context).toInt()
        }
        binding.root.translationX = getDetailsModel().startX.dpToPx(context)
        binding.root.translationY = startY.dpToPx(context)
        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
        binding.cardViewVideoBg.strokeWidth = 2.dpToPx(context).toInt()
    }

    private fun fullScreenMode() {
        screen = Screen.FULLSCREEN
        floatWindowLayoutParam.width = LayoutParams.MATCH_PARENT
        floatWindowLayoutParam.height = LayoutParams.MATCH_PARENT
        binding.root.translationX = 0f
        binding.root.translationY = 0f
        val paddingDp = 30
        val density = context.resources.displayMetrics.density
        val paddingPixel = (paddingDp * density)
        binding.layoutMain.setPadding(0, 0, 0, paddingPixel.toInt())
        binding.root.bringToFront()
        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
        binding.cardViewVideoBg.strokeWidth = 0
    }


    private fun resumeVideo() {
        exoPlayer?.playWhenReady = true
    }

    private fun updateProgressBar() {
        mHandler?.postDelayed(updateTimeTask, 1000)
    }

    override fun close() {
        pauseVideo()
        exoPlayer?.release()
        introMediaPlayer.release()
        super.close()
        //(context as MainActivity).mainVM.coolTvAppLivedata.postValue(null)
        closeWindowListener.closeWindow(appDetailModel)
        coolTvAppLivedata.postValue(null)
        exoPlayerHandler.removeCallbacksAndMessages(null)
    }
}