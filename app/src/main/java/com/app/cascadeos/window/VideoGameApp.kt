package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.ClipDescription
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.util.TypedValue
import android.view.*
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.app.cascadeos.R
import com.app.cascadeos.adapter.AppListAdapter
import com.app.cascadeos.adapter.MulticastAppAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutVideoGameBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


@UnstableApi
class VideoGameApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle, private val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    private val gameUrl: String,
    val onClickListener: OnClickListener,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    private val appClickLiveData: MutableLiveData<View>,
    private var onMulticastAppClick: ((
        startX: Int,
        width: Int,
        height: Int,
        itemModel: AppModel,
    ) -> Unit),
) : WindowApp<LayoutVideoGameBinding>(
    context,
    lifecycle,
    R.layout.layout_video_game,
    mBinding,
    null
),
    AppListAdapter.ItemClickListener {
    private var mMediaPlayer: MediaPlayer? = null
    private var exoPlayer: ExoPlayer? = null
    private var isMediaPlaying = true
    lateinit var mAdapter: AppListAdapter
    private lateinit var multicastAppAdapter: MulticastAppAdapter
    private lateinit var appListPreference: SharedPreferences
    private var multicastAppList = ArrayList<AppModel>()

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    private val configObserver = Observer<Configuration?> {

        if (isObserverActive) {
            it?.let {
                if (it.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    close()
                }
            }
        }
        isObserverActive = true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()
        hideAppBar()
        attachVideoDoubleTapListener()
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutContent.layoutTransition = layoutTransition

        appListPreference = context.getSharedPreferences(APP_LIST_PREFS, Context.MODE_PRIVATE)
//        toggleLayoutFullScreen(true)
        mAdapter = AppListAdapter(getGamesList(), this, true, gameUrl)
        binding.rvGames.adapter = mAdapter
        multicastAppAdapter = MulticastAppAdapter(context,
            appList = getAppListFromPreference(),
            onAppClick = { item, _ ->
                item.isVideoGameApp = true
                val startingPoint =
                    if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(
                        context.getString(R.string.txt_video_game)
                    )?.startX
                        ?: 0 else 0
                val appWidth =
                    if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(
                        context.getString(R.string.txt_video_game)
                    )?.width
                        ?: 0 else ConstraintLayout.LayoutParams.MATCH_PARENT
                pauseVideo()
                onMulticastAppClick(
                    startingPoint,
                    appWidth,
                    GAME_SYSTEM_APP_HEIGHT,
                    item
                )
            },
            onRemove = { appList ->
                saveAppListFromPreference(appList)
            })

        binding.rvGameApps.adapter = multicastAppAdapter
        binding.rvGameApps.setOnDragListener(dragListener)
        binding.clickListener = onClickListener

        configurationLiveData.observe((context as MainActivity), configObserver)


        exoPlayer = ExoPlayer.Builder(context).build().also { ePlayer ->
            binding.exoPlayerView.player = ePlayer
            val mediaItem = MediaItem.fromUri(gameUrl)
            ePlayer.setMediaItem(mediaItem)
            ePlayer.prepare()
            ePlayer.play()
        }
        exoPlayer?.addListener(object : Player.Listener {
            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    binding.progressBarVideoLoading.visibility = View.VISIBLE
                } else if (playbackState == ExoPlayer.STATE_READY) {
                    binding.progressBarVideoLoading.visibility = View.INVISIBLE
                }
            }
        })


        binding.mainClickListener = onClickListener
        binding.clickListener = OnClickListener {
            when (it.id) {
                R.id.fb, R.id.twitter, R.id.app_discovery, R.id.app_barcode, R.id.app_amazon -> {
                    pauseVideo()
                    isMediaPlaying = false
                    onClickListener.onClick(it)
                }

                R.id.img_icon_call_of_duty -> {
                    isMediaPlaying = true
                    changeGameVideo(CALL_OF_DUTY_GAME_VIDEO_URL)
                }

                R.id.img_icon_contract_killer -> {
                    isMediaPlaying = true
                    changeGameVideo(CONTRACT_KILLER_GAME_VIDEO_URL)
                }

                R.id.img_icon_angry_bird -> {
                    isMediaPlaying = true
                    changeGameVideo(ANGRY_BIRD_GAME_VIDEO_URL)
                }

                R.id.img_icon_gt_racing -> {
                    isMediaPlaying = true
                    changeGameVideo(GT_RACING_GAME_VIDEO_URL)
                }

                R.id.img_icon_formula_1 -> {
                    isMediaPlaying = true
                    changeGameVideo(FORMULA_1_GAME_VIDEO_URL)
                }

                R.id.img_icon_joystick, R.id.img_icon_joystick_after_zoom -> {
                    if (isMediaPlaying) {
                        isMediaPlaying = false
                        pauseVideo()
                    } else {
                        isMediaPlaying = true
                        resumeVideo()
                    }
                }

                else -> {

                }
            }
        }
        attachClickListenerForBackButton()
    }

    private fun attachClickListenerForBackButton() {
        binding.layoutHiddenActionbarClose.setOnClickListener {
            if (binding.cardViewController.visibility == View.GONE) {
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
            onDoubleTaped()
        }
    }

    private fun getGamesList(): ArrayList<AppModel> {
        val appList = ArrayList<AppModel>()
        appList.add(
            AppModel(
                id = 1,
                appName = context.getString(R.string.txt_call_of_duty),
                icon = R.drawable.icon_call_of_duty,
                appUrl = CALL_OF_DUTY_GAME_VIDEO_URL
            )
        )
        appList.add(
            AppModel(
                id = 2,
                appName = context.getString(R.string.txt_contract_killer),
                icon = R.drawable.icon_contract_killer_game,
                appUrl = CONTRACT_KILLER_GAME_VIDEO_URL
            )
        )

        appList.add(
            AppModel(
                id = 3,
                appName = context.getString(R.string.txt_angry_bird),
                icon = R.drawable.icon_angry_bird_game,
                appUrl = ANGRY_BIRD_GAME_VIDEO_URL
            )
        )

        appList.add(
            AppModel(
                id = 4,
                appName = context.getString(R.string.txt_gt_racing),
                icon = R.drawable.icon_gt_racing_game,
                appUrl = GT_RACING_GAME_VIDEO_URL
            )
        )

        appList.add(
            AppModel(
                id = 5,
                appName = context.getString(R.string.txt_formula_1),
                icon = R.drawable.icon_formula_1_gaming,
                appUrl = FORMULA_1_GAME_VIDEO_URL
            )
        )

        appList.add(
            AppModel(
                id = 6,
                appName = context.getString(R.string.txt_mine_craft),
                icon = R.drawable.icon_minecraft,
                appUrl = MINE_CRAFT_GAME_URL
            )
        )

        appList.add(
            AppModel(
                id = 7,
                appName = context.getString(R.string.txt_grand_theft_auto),
                icon = R.drawable.icon_grand_theft_auto,
                appUrl = GRAND_THEFT_AUTO_GAME_URL
            )
        )

        appList.add(
            AppModel(
                id = 8,
                appName = context.getString(R.string.txt_pubg_battleground),
                icon = R.drawable.icon_pubg_battleground,
                appUrl = PUBG_BATTLEGROUND_GAME_URL
            )
        )

        appList.add(
            AppModel(
                id = 9,
                appName = context.getString(R.string.txt_candy_crush_saga),
                icon = R.drawable.icon_candy_crush_saga,
                appUrl = CANDY_CRUSH_GAME_URL
            )
        )

        return appList
    }

    private val dragListener = View.OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            }

            DragEvent.ACTION_DRAG_ENTERED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DRAG_LOCATION -> {
                true
            }

            DragEvent.ACTION_DRAG_EXITED -> {
                v.invalidate()
                true
            }

            DragEvent.ACTION_DROP -> {
                val items = event.clipData.getItemAt(0)
                val dragData = items.text

                val gson = Gson()
                val itemModel = gson.fromJson(dragData.toString(), AppModel::class.java)
                v.invalidate()

                val x = event.x
                val y = event.y
                val childView: View? = binding.rvGameApps.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = binding.rvGameApps.getChildAdapterPosition(childView)
                    saveAppListFromPreference(multicastAppAdapter.addItem(itemModel, position))
                    if (position == 0) {
                        binding.rvGameApps.smoothScrollToPosition(position)
                    }
                }
                true
            }

            DragEvent.ACTION_DRAG_ENDED -> {
                v.invalidate()
                true
            }

            else -> false
        }
    }

    private fun getAppListFromPreference(): ArrayList<AppModel> {
        val gson = Gson()
        val jsonText: String? = appListPreference.getString(GET_GAME_SCREEN_APP, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            multicastAppList = it
        }
        if (multicastAppList.isEmpty()) {
            multicastAppList = ListConstants.getGameAppsList(context)
            saveAppListFromPreference(multicastAppList)
        }
        return multicastAppList
    }

    private fun saveAppListFromPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_GAME_SCREEN_APP, Gson().toJson(appList))
            apply()
        }
    }

    private fun changeGameVideo(newVideoUrl: String) {
        pauseVideo()
        exoPlayer?.setMediaItem(MediaItem.fromUri(newVideoUrl))
        exoPlayer?.prepare()
        exoPlayer?.play()
    }

    private fun pauseVideo() {
        isMediaPlaying = false
        mMediaPlayer?.pause()
        exoPlayer?.playWhenReady = false
    }

    private fun attachVideoDoubleTapListener() {
        binding.exoPlayerView.setOnTouchListener(object : OnTouchListener {
            private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onDoubleTaped()
                    return super.onDoubleTap(e)

                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
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

    private fun onDoubleTaped() {
        if (binding.cardViewController.visibility == View.VISIBLE) {
            binding.layoutHiddenActionbar.visibility = View.GONE
            binding.cardViewController.visibility = View.GONE
            binding.clAfterFullScreen.visibility = View.VISIBLE
            binding.imgResize.visibility = View.INVISIBLE
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
            val paddingDp = 35
            val density = context.resources.displayMetrics.density
            val paddingPixel = (paddingDp * density)
            binding.layoutMain.setPadding(0, 0, 0, paddingPixel.toInt())
//                            toggleLayoutFullScreen(true)
        } else {
            binding.layoutHiddenActionbar.visibility = View.VISIBLE
            binding.imgResize.visibility = View.INVISIBLE
            binding.cardViewController.visibility = View.VISIBLE
            binding.clAfterFullScreen.visibility = View.GONE
            binding.layoutMain.background = ContextCompat.getDrawable(context, R.drawable.img_bg_green)
            binding.cardViewVideoBg.setMargins(
                leftMarginDp = 10, topMarginDp = 10, rightMarginDp = 10, bottomMarginDp = 10
            )
            binding.mainCard.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 10f, context.resources.displayMetrics
            )
            binding.cardViewVideoBg.radius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 13f, context.resources.displayMetrics
            )
//                            toggleLayoutFullScreen(false)

        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.imgBack.visibility = View.GONE
        }, 3000)
    }

    private fun toggleLayoutFullScreen(makeFullScreen: Boolean) {
        screen = Screen.FULLSCREEN
        if (isPortrait()) {

            floatWindowLayoutParam.height =
                (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(R.integer.b_height) + context.resources.getInteger(
                    R.integer.c_height
                )).toInt().dpToPx(context).toInt()

            if (!isMiniMize) {

                floatWindowLayoutParam.width =
                    context.resources.getInteger(R.integer.a_width).dpToPx(context).toInt()
            }

            binding.root.translationY = 0f


        } else {
            floatWindowLayoutParam.width =
                (context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                    R.integer.c_width
                )).dpToPx(context).toInt()

            if (!isMiniMize) {
                floatWindowLayoutParam.height =
                    context.resources.getInteger(R.integer.b_height).dpToPx(context).toInt()
            }
            binding.root.translationX = 0f
        }


        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
        binding.root.bringToFront()
    }

    private fun resumeVideo() {
        mMediaPlayer?.start()
        exoPlayer?.playWhenReady = true
    }

    fun View.setMargins(
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

    override fun close() {
        pauseVideo()
        exoPlayer?.release()
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
        configurationLiveData.removeObserver(configObserver)
    }

    override fun onAppIconClick(appUrl: String?) {
        appUrl?.let { changeGameVideo(it) }
    }
}