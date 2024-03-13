package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.View.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.app.cascadeos.R
import com.app.cascadeos.adapter.HomeScreenAppListAdapter
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.interfaces.SnapToGridListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.MulticastModel
import com.app.cascadeos.ui.camera.CameraActivity
import com.app.cascadeos.ui.gallery.GalleryActivity
import com.app.cascadeos.utility.*
import com.app.cascadeos.viewmodel.MainVM
import com.app.cascadeos.viewmodel.MainVM.Companion.coolTvAppLivedata
import com.app.cascadeos.window.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class MainActivity : AppCompatActivity(), CloseWindowListener, SnapToGridListener {

    private lateinit var mBinding: ActivityMainBinding
    private var appListA = ArrayList<AppModel>()
    private var appListC = ArrayList<AppModel>()
    private var homeScreenAppListAdapter: HomeScreenAppListAdapter? = null
    private var homeScreenAppListAdapterC: HomeScreenAppListAdapter? = null
    private var isFromA = false
    private var isFromC = false
    private var appPickPositionForA: Int = 0
    private var appPickChildViewForA: View? = null
    private var appPickPositionForC: Int = 0
    private var appPickChildViewForC: View? = null

    private lateinit var appListPreference: SharedPreferences
    lateinit var keyboardApp: KeyboardApp

    private val overlayPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    val mainVM: MainVM by viewModels()
    val getCascadeResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val appsName = result.data?.getStringExtra("ResumeApp")
            if (result.resultCode == Activity.RESULT_OK) {
                when (appsName) {
                    getString(R.string.phone_andrea) -> {
                        this.startMulticastApp(
                            app = AppDetailModel(
                                getString(R.string.multicast_system),
                                resources.getInteger(R.integer.c_start),
                                resources.getInteger(R.integer.c_width),
                                gravity = Gravity.END
                            ), isResume = true
                        )
                        this.startDialerApp(
                            app = AppDetailModel(
                                getString(R.string.phone_system),
                                resources.getInteger(R.integer.a_start),
                                resources.getInteger(R.integer.a_width),
                                gravity = Gravity.START
                            ), isResume = true
                        )
                        this.startCoolTvApp(
                            app = AppDetailModel(
                                getString(R.string.cool_tv),
                                resources.getInteger(R.integer.b_start),
                                resources.getInteger(R.integer.b_width),
                                gravity = Gravity.CENTER
                            ), isResume = true
                        )
                    }

                    getString(R.string.game_angry_bird) -> {
                        startWebApp(
                            AppDetailModel(
                                getString(R.string.whatsapp),
                                resources.getInteger(R.integer.a_start),
                                resources.getInteger(R.integer.a_width),
                                WHATSAPP_URL,
                                Gravity.START
                            )
                        )
                        val app = AppDetailModel(
                            getString(R.string.txt_video_game),
                            startX = resources.getInteger(R.integer.b_start),
                            width = resources.getInteger(R.integer.b_width) + resources.getInteger(R.integer.c_width),
                            gravity = Gravity.CENTER,
                            height = resources.getInteger(R.integer.b_height)

                        )
                        startVideoGameApp(app, ANGRY_BIRD_GAME_VIDEO_URL)
                    }

                    getString(R.string.digital_locker) -> {
                        val intent = Intent(this, DigitalLockerActivity::class.java)
                        getDigitalLockerResult.launch(intent)
                    }
                }
            }
        }

    private val getDigitalLockerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val app = AppDetailModel(
                getString(R.string.txt_video_game),
                startX = resources.getInteger(R.integer.a_start),
                width = (resources.getInteger(R.integer.a_width) + resources.getInteger(R.integer.b_width) + resources.getInteger(
                    R.integer.c_width
                )),
                gravity = Gravity.CENTER,
                height = resources.getInteger(R.integer.b_height)
            )
            startVideoGameApp(app, it.data?.getStringExtra(GAME_URL).toString())
        }
    }

    private val getCameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val app = AppDetailModel(
                getString(R.string.txt_video_game),
                startX = resources.getInteger(R.integer.a_start),
                (resources.getInteger(R.integer.a_width) + resources.getInteger(R.integer.b_width) + resources.getInteger(
                    R.integer.c_width
                )),
                gravity = Gravity.CENTER,
                height = resources.getInteger(R.integer.b_height)
            )
            startVideoGameApp(app, it.data?.getStringExtra(GAME_URL).toString())
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.layoutMain.layoutTransition = LayoutTransition()

        mBinding.recyclerViewAppListA.setOnDragListener(dragCToAListener)
        mBinding.recyclerViewAppListC.setOnDragListener(dragAtoCListener)

        appListPreference = getSharedPreferences(APP_LIST_PREFS, Context.MODE_PRIVATE)

        homeScreenAppListAdapter = HomeScreenAppListAdapter(getAppListAFromPreference()) { itemModel, position ->
            isFromA = true
            isFromC = false
            val itemGson = Gson()
            val itemString = itemGson.toJson(itemModel)
            val item = ClipData.Item(itemString)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val data = ClipData("cloneApp", mimeTypes, item)

            val viewHolder = mBinding.recyclerViewAppListA.findViewHolderForAdapterPosition(position)
            val currentView = (viewHolder as HomeScreenAppListAdapter.ViewHolder).itemView

            val dragShadowBuilder = View.DragShadowBuilder(currentView)
            currentView.startDragAndDrop(data, dragShadowBuilder, currentView, 0)
        }
        homeScreenAppListAdapterC = HomeScreenAppListAdapter(getAppListCFromPreference()) { itemModel, position ->
            isFromC = true
            isFromA = false
            val itemGson = Gson()
            val itemString = itemGson.toJson(itemModel)
            val item = ClipData.Item(itemString)
            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
            val data = ClipData("cloneApp", mimeTypes, item)

            val viewHolder = mBinding.recyclerViewAppListC.findViewHolderForAdapterPosition(position)
            val currentView = (viewHolder as HomeScreenAppListAdapter.ViewHolder).itemView

            val dragShadowBuilder = View.DragShadowBuilder(currentView)
            currentView.startDragAndDrop(data, dragShadowBuilder, currentView, 0)
        }
        PagerSnapHelper().attachToRecyclerView(mBinding.recyclerViewAppListA)
        PagerSnapHelper().attachToRecyclerView(mBinding.recyclerViewAppListC)
        mBinding.recyclerViewAppListA.adapter = homeScreenAppListAdapter
        mBinding.recyclerViewAppListC.adapter = homeScreenAppListAdapterC

        addVerticalSwipeListener()

        mBinding.layoutMain.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
            }
            return@setOnTouchListener false
        }

        homeScreenAppListAdapter?.appClickLiveData?.observe(this) {
            it?.let {
                onClick(it, resources.getInteger(R.integer.a_start),resources.getInteger(R.integer.a_width),Gravity.START)
                homeScreenAppListAdapter?.appClickLiveData?.value = null
            }
        }
        homeScreenAppListAdapterC?.appClickLiveData?.observe(this) {
            it?.let {
                onClick(it, resources.getInteger(R.integer.c_start),resources.getInteger(R.integer.c_width),Gravity.END)
                homeScreenAppListAdapterC?.appClickLiveData?.value = null
            }
        }

        mBinding.apply {
            appPhoneSystem.setOnClickListener {
                onClick(it, resources.getInteger(R.integer.a_start),resources.getInteger(R.integer.a_width),Gravity.START)
            }
            appDigitalLocker.setOnClickListener {
                val intent = Intent(this@MainActivity, DigitalLockerActivity::class.java)
                getDigitalLockerResult.launch(intent)
            }
            appCoolTv.setOnClickListener {
                onClick(it, resources.getInteger(R.integer.b_start),resources.getInteger(R.integer.b_width),Gravity.CENTER)
            }
            appMuticast.setOnClickListener {
                onClick(it, resources.getInteger(R.integer.c_start),resources.getInteger(R.integer.c_width),Gravity.END)
            }
            internet.setOnClickListener {
                onClick(it, resources.getInteger(R.integer.b_start),resources.getInteger(R.integer.b_width),Gravity.CENTER)
            }
        }

    }

    private val dragCToAListener = OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)

                val x = event.x
                val y = event.y
                appPickChildViewForC = mBinding.recyclerViewAppListC.findChildViewUnder(x, y)
                if (appPickChildViewForC != null) {
                    appPickPositionForC = mBinding.recyclerViewAppListC.getChildLayoutPosition(appPickChildViewForC!!)
                    //saveAppListCToPreference(homeScreenAppListAdapter!!.removeItemFromList(position))
                }
                true
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
                val childView: View? = mBinding.recyclerViewAppListA.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = mBinding.recyclerViewAppListA.getChildLayoutPosition(childView)
                    saveAppListAToPreference(
                        homeScreenAppListAdapter!!.addItem(
                            itemModel, position
                        )
                    )
                    if (position == 0) {
                        mBinding.recyclerViewAppListA.smoothScrollToPosition(position)
                    }
                }
                if (!isFromA) {
                    if (appPickChildViewForA != null) {
                        homeScreenAppListAdapterC?.let {
                            saveAppListCToPreference(
                                it.removeItemFromList(
                                    itemModel
                                )
                            )
                        }
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

    private val dragAtoCListener = OnDragListener { v, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                val x = event.x
                val y = event.y
                appPickChildViewForA = mBinding.recyclerViewAppListA.findChildViewUnder(x, y)
                if (appPickChildViewForA != null) {
                    appPickPositionForA = mBinding.recyclerViewAppListA.getChildLayoutPosition(appPickChildViewForA!!)
                    //saveAppListCToPreference(homeScreenAppListAdapter!!.removeItemFromList(position))
                }
                true
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
                val childView: View? = mBinding.recyclerViewAppListC.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = mBinding.recyclerViewAppListC.getChildLayoutPosition(childView)
                    saveAppListCToPreference(
                        homeScreenAppListAdapterC!!.addItem(
                            itemModel, position
                        )
                    )
                    if (position == 0) {
                        mBinding.recyclerViewAppListC.smoothScrollToPosition(position)
                    }
                }
                if (!isFromC) {
                    if (appPickChildViewForC != null) {
                        homeScreenAppListAdapter?.let {
                            saveAppListAToPreference(
                                it.removeItemFromList(
                                    itemModel
                                )
                            )
                        }
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

    private fun getAppListAFromPreference(): ArrayList<AppModel> {
        val gson = Gson()
        val jsonText: String? = appListPreference.getString(GET_APP_LIST_A, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            appListA = it
        }
        if (appListA.isEmpty()) {
            appListA = getAppListA()
            saveAppListAToPreference(appListA)
        }
        return appListA
    }

    private fun getAppListCFromPreference(): ArrayList<AppModel> {
        val gson = Gson()
        val jsonText: String? = appListPreference.getString(GET_APP_LIST_C, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            appListC = it
        }
        if (appListC.isEmpty()) {
            appListC = getAppListC()
            saveAppListCToPreference(appListC)
        }
        return appListC
    }

    private fun saveAppListAToPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_APP_LIST_A, Gson().toJson(appList))
            apply()
        }
    }

    private fun saveAppListCToPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_APP_LIST_C, Gson().toJson(appList))
            apply()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addHorizontalSwipeListener() {
        mBinding.imgLogo.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {
            override fun onSwipeRight() {
                mBinding.groupBottomBar.visibility = View.GONE
            }

            override fun onSwipeLeft() {
                mBinding.groupBottomBar.visibility = View.VISIBLE
            }

            override fun onSwipeTop() {
            }

            override fun onSwipeBottom() {
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addVerticalSwipeListener() {
        mBinding.imgLogo.setOnTouchListener(object : OnSwipeTouchListener(this@MainActivity) {

            override fun onSwipeTop() {
                mBinding.groupBottomBar.visibility = View.VISIBLE
            }

            override fun onSwipeBottom() {
                mBinding.groupBottomBar.visibility = View.GONE
            }

            override fun onSwipeRight() {
            }

            override fun onSwipeLeft() {
            }
        })
    }

    private fun getAppListC(): ArrayList<AppModel> {
        appListC.add(
            AppModel(
                id = R.id.app_settings, appName = getString(R.string.txt_settings), icon = R.drawable.icon_app_settings
            )
        )
        appListC.add(
            AppModel(
                id = R.id.twitter, appName = getString(R.string.twitter), icon = R.drawable.ic_twitter
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_click_video_shop,
                appName = getString(R.string.txt_click_video_shop),
                icon = R.drawable.icon_click_video_shop_new
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_netflix, appName = getString(R.string.txt_netflix), icon = R.drawable.icon_netflix
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_npr_news, appName = getString(R.string.npr_news), icon = R.drawable.ic_npr_news
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_webmd, appName = getString(R.string.web_md_mobile), icon = R.drawable.ic_web_md
            )
        )
        appListC.add(
            AppModel(
                id = R.id.fb, appName = getString(R.string.facebook), icon = R.drawable.ic_fb
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_itunes, appName = getString(R.string.txt_itunes), icon = R.drawable.icon_itunes
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_weather, appName = getString(R.string.weather_channel), icon = R.drawable.ic_weather_channel
            )
        )
        appListC.add(
            AppModel(
                id = R.id.linkedin, appName = getString(R.string.linkedin), icon = R.drawable.ic_linkdin
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_pandora_radio, appName = getString(R.string.pandora_radio), icon = R.drawable.ic_pandora
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_youtube, appName = getString(R.string.youtube), icon = R.drawable.ic_youtube
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_drop_box, appName = getString(R.string.dropbox), icon = R.drawable.ic_dropbox
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_barcode, appName = getString(R.string.barcode), icon = R.drawable.ic_bar_code
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_discovery, appName = getString(R.string.discovery), icon = R.drawable.ic_discovery
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_cnn_news, appName = getString(R.string.txt_cnn_news), icon = R.drawable.icon_cnn_news
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_blinkist, appName = getString(R.string.txt_blinkist), icon = R.drawable.icon_app_blinkit
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_chime, appName = getString(R.string.txt_chime), icon = R.drawable.icon_app_chime
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_cash_app, appName = getString(R.string.txt_cash_app), icon = R.drawable.icon_app_cash_app
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_hbo_max, appName = getString(R.string.txt_hbo_max), icon = R.drawable.icon_app_hbo_max
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_uber_eats, appName = getString(R.string.txt_uber_eats), icon = R.drawable.icon_app_uber_eats
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_disney_plus,
                appName = getString(R.string.txt_disney_plus),
                icon = R.drawable.icon_app_disney_plus
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_temu, appName = getString(R.string.txt_temu), icon = R.drawable.icon_app_temu
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_walmart, appName = getString(R.string.txt_walmart), icon = R.drawable.icon_app_walmart
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_door_dash, appName = getString(R.string.txt_door_dash), icon = R.drawable.icon_app_door_dash
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_pinterest, appName = getString(R.string.txt_pinterest), icon = R.drawable.icon_app_pinterest
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_indeed, appName = getString(R.string.txt_indeed), icon = R.drawable.icon_app_indeed
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_hopper, appName = getString(R.string.txt_hopper), icon = R.drawable.icon_app_hopper
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_kfc, appName = getString(R.string.txt_kfc), icon = R.drawable.icon_app_kfc
            )
        )
        appListC.add(
            AppModel(
                id = R.id.app_news_break, appName = getString(R.string.txt_news_break), icon = R.drawable.icon_app_news_break
            )
        )

        return appListC
    }

    private fun getAppListA(): ArrayList<AppModel> {
        appListA.add(
            AppModel(
                id = R.id.app_camera, appName = getString(R.string.txt_camera), icon = R.drawable.icon_app_camera
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_gallery, appName = getString(R.string.txt_gallery), icon = R.drawable.icon_app_gallery
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_amazon_music,
                appName = getString(R.string.txt_amazon_music),
                icon = R.drawable.icon_amazon_music
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_amazon, appName = getString(R.string.amazon), icon = R.drawable.ic_amazon
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_zoom, appName = getString(R.string.txt_zoom), icon = R.drawable.icon_zoom
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_whatsapp, appName = getString(R.string.whatsapp), icon = R.drawable.ic_whatsapp
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_messages, appName = getString(R.string.messages), icon = R.drawable.ic_message
            )
        )
        appListA.add(
            AppModel(
                id = R.id.instagram, appName = getString(R.string.instagram), icon = R.drawable.ic_instagram
            )
        )
        appListA.add(
            AppModel(
                id = R.id.citi_bank, appName = getString(R.string.citi_bank), icon = R.drawable.ic_citi
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_uber, appName = getString(R.string.txt_uber), icon = R.drawable.icon_uber
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_bumble, appName = getString(R.string.bumble), icon = R.drawable.ic_bumble
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_espn, appName = getString(R.string.espn_score_center), icon = R.drawable.ic_espn
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_expedia, appName = getString(R.string.txt_expedia), icon = R.drawable.icon_expedia
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_yelp, appName = getString(R.string.yelp), icon = R.drawable.ic_yelp
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_air_bnb, appName = getString(R.string.txt_airbnb), icon = R.drawable.icon_airbnb
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_snap_chat, appName = getString(R.string.snapchat), icon = R.drawable.ic_snapchat
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_google_meet,
                appName = getString(R.string.txt_google_meet),
                icon = R.drawable.icon_app_google_meet
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_venmo, appName = getString(R.string.txt_venmo), icon = R.drawable.icon_app_venmo
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_hinge, appName = getString(R.string.txt_hinge), icon = R.drawable.icon_app_hinge
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_muscle_booster_workout_planner,
                appName = getString(R.string.txt_muscle_booster_workout_planner),
                icon = R.drawable.icon_app_muscle_booster_workout_planner
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_google_classroom,
                appName = getString(R.string.txt_google_classroom),
                icon = R.drawable.icon_app_google_classroom
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_ebay, appName = getString(R.string.txt_ebay), icon = R.drawable.icon_app_ebay
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_reddit, appName = getString(R.string.txt_reddit), icon = R.drawable.icon_app_reddit
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_booking_com, appName = getString(R.string.txt_booking), icon = R.drawable.icon_app_booking_com
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_group_me, appName = getString(R.string.txt_group_me), icon = R.drawable.icon_app_group_me
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_vrbo, appName = getString(R.string.txt_vrbo), icon = R.drawable.icon_vrbo
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_tiktok, appName = getString(R.string.txt_tiktok), icon = R.drawable.icon_app_tiktok
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_google_drive,
                appName = getString(R.string.txt_google_drive),
                icon = R.drawable.icon_app_google_drive
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_outlook, appName = getString(R.string.txt_outlook), icon = R.drawable.icon_app_outlook
            )
        )
        appListA.add(
            AppModel(
                id = R.id.app_youtube_music,
                appName = getString(R.string.txt_youtube_music),
                icon = R.drawable.icon_app_youtube_music
            )
        )
        return appListA
    }


    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun onClick(view: View, startX: Int,width: Int,gravity : Int) {
        when (view.id) {
            R.id.app_camera -> {
                startActivity(Intent(this, CameraActivity::class.java))
            }

            R.id.app_gallery -> {
                startActivity(Intent(this, GalleryActivity::class.java))
            }

            R.id.instagram -> {
                val app = AppDetailModel(
                    getString(R.string.instagram),
                    startX,
                    width,
                    INSTAGRAM_URL,
                    gravity
                )

                startWebApp(app)

            }

            R.id.app_whatsapp -> {
                val app = AppDetailModel(
                    getString(R.string.whatsapp),
                    startX,
                    width,
                    WHATSAPP_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_amazon -> {
                val app = AppDetailModel(
                    getString(R.string.amazon), startX, width, AMAZON_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_uber -> {
                val app = AppDetailModel(
                    getString(R.string.txt_uber), startX, width, UBER_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_bumble -> {
                val app = AppDetailModel(
                    getString(R.string.bumble), startX, width, BUMBLE_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_espn -> {
                val app = AppDetailModel(
                    getString(R.string.espn_score_center),
                    startX,
                    width,
                    ESPN_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_yelp -> {
                val app = AppDetailModel(
                    getString(R.string.yelp), startX, width, YELP_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_snap_chat -> {
                val app = AppDetailModel(
                    getString(R.string.snapchat),
                    startX,
                    width,
                    SNAP_CHAT_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_google_meet -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_meet),
                    startX,
                    width,
                    GOOGLE_MEET_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_venmo -> {
                val app = AppDetailModel(
                    getString(R.string.txt_venmo), startX, width, VENMO_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_hinge -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hinge), startX, width, HINGE_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_muscle_booster_workout_planner -> {
                val app = AppDetailModel(
                    getString(R.string.txt_muscle_booster_workout_planner),
                    startX,
                    width,
                    MUSCLE_BOOSTER_WORKOUT_PLANNER_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_google_classroom -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_classroom),
                    startX,
                    width,
                    GOOGLE_CLASSROOM_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_ebay -> {
                val app = AppDetailModel(
                    getString(R.string.txt_ebay), startX, width, EBAY_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_reddit -> {
                val app = AppDetailModel(
                    getString(R.string.txt_reddit),
                    startX,
                    width,
                    REDDIT_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_booking_com -> {
                val app = AppDetailModel(
                    getString(R.string.txt_booking),
                    startX,
                    width,
                    BOOKING_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_group_me -> {
                val app = AppDetailModel(
                    getString(R.string.txt_group_me),
                    startX,
                    width,
                    GROUP_ME_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_vrbo -> {
                val app = AppDetailModel(
                    getString(R.string.txt_vrbo), startX, width, VRBO_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_tiktok -> {
                val app = AppDetailModel(
                    getString(R.string.txt_tiktok),
                    startX,
                    width,
                    TIKTOK_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_google_drive -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_drive),
                    startX,
                    width,
                    GOOGLE_DRIVE_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_outlook -> {
                val app = AppDetailModel(
                    getString(R.string.txt_outlook),
                    startX,
                    width,
                    OUTLOOK_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_youtube_music -> {
                val app = AppDetailModel(
                    getString(R.string.txt_youtube_music),
                    startX,
                    width,
                    YOUTUBE_MUSIC_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_news_break -> {
                val app = AppDetailModel(
                    getString(R.string.txt_news_break),
                    startX,
                    width,
                    NEWS_BREAK_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_drop_box -> {
                val app = AppDetailModel(
                    getString(R.string.dropbox), startX, width, DROP_BOX_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_npr_news -> {
                val app = AppDetailModel(
                    getString(R.string.npr_news), startX, width, NPR_NEWS_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_webmd -> {
                val app = AppDetailModel(
                    getString(R.string.web_md_mobile),
                    startX,
                    width,
                    WEBMD_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_weather -> {
                val app = AppDetailModel(
                    getString(R.string.weather_channel),
                    startX,
                    width,
                    WEATHER_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_pandora_radio -> {
                val app = AppDetailModel(
                    getString(R.string.pandora_radio),
                    startX,
                    width,
                    PANDORA_RADIO_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_youtube -> {
                val app = AppDetailModel(
                    getString(R.string.youtube), startX, width, YOUTUBE_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_google_keep -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_keep),
                    startX,
                    width,
                    GOOGLE_KEEP_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_blinkist -> {
                val app = AppDetailModel(
                    getString(R.string.txt_blinkist),
                    startX,
                    width,
                    BLINKIST_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_chime -> {
                val app = AppDetailModel(
                    getString(R.string.txt_chime), startX, width, CHIME_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_cash_app -> {
                val app = AppDetailModel(
                    getString(R.string.txt_cash_app),
                    startX,
                    width,
                    CASH_APP_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_hbo_max -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hbo_max),
                    startX,
                    width,
                    HBO_MAX_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_uber_eats -> {
                val app = AppDetailModel(
                    getString(R.string.txt_uber_eats),
                    startX,
                    width,
                    UBER_EATS_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_disney_plus -> {
                val app = AppDetailModel(
                    getString(R.string.txt_disney_plus),
                    startX,
                    width,
                    DISNEY_PLUS_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_temu -> {
                val app = AppDetailModel(
                    getString(R.string.txt_temu), startX, width, TEMU_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_turbo_tax -> {
                val app = AppDetailModel(
                    getString(R.string.txt_turbo_tax),
                    startX,
                    width,
                    TURBO_TAX_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_walmart -> {
                val app = AppDetailModel(
                    getString(R.string.txt_walmart),
                    startX,
                    width,
                    WALMART_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_door_dash -> {
                val app = AppDetailModel(
                    getString(R.string.txt_door_dash),
                    startX,
                    width,
                    DOOR_DASH_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_pinterest -> {
                val app = AppDetailModel(
                    getString(R.string.txt_pinterest),
                    startX,
                    width,
                    PINTEREST_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_indeed -> {
                val app = AppDetailModel(
                    getString(R.string.txt_indeed), startX, width, INDEED_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_hopper -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hopper), startX, width, HOPPER_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_kfc -> {
                val app = AppDetailModel(
                    getString(R.string.txt_kfc), startX, width, KFC_URL, gravity
                )
                startWebApp(app)
            }


            R.id.app_barcode -> {
                val app = AppDetailModel(
                    getString(R.string.barcode), startX, width, BARCODE_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_discovery -> {
                val app = AppDetailModel(
                    getString(R.string.discovery),
                    startX,
                    width,
                    DISCOVERY_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_cnn_news -> {
                val app = AppDetailModel(
                    getString(R.string.txt_cnn_news),
                    startX,
                    width,
                    CNN_NEWS_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_zoom -> {
                val app = AppDetailModel(
                    getString(R.string.txt_zoom), startX, width, ZOOM_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_air_bnb -> {
                val app = AppDetailModel(
                    getString(R.string.txt_airbnb),
                    startX,
                    width,
                    AIR_BNB_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_expedia -> {
                val app = AppDetailModel(
                    getString(R.string.txt_expedia),
                    startX,
                    width,
                    EXPEDIA_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_amazon_music -> {
                val app = AppDetailModel(
                    getString(R.string.txt_amazon_music),
                    startX,
                    width,
                    AMAZON_MUSIC_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_itunes -> {
                val app = AppDetailModel(
                    getString(R.string.txt_itunes), startX, width, ITUNES_URL, gravity
                )
                startWebApp(app)
            }

            R.id.app_click_video_shop -> {
                val app = AppDetailModel(
                    getString(R.string.txt_click_video_shop),
                    startX,
                    width,
                    CLICK_VIDEO_SHOP_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.app_digital_locker -> {
                val intent = Intent(this, DigitalLockerActivity::class.java)
                getDigitalLockerResult.launch(intent)

            }

            R.id.app_netflix -> {
                val app = AppDetailModel(
                    getString(R.string.txt_netflix),
                    startX,
                    width,
                    NETFLIX_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.tv_click_vedio_shop -> {
                val app = AppDetailModel(
                    getString(R.string.txt_click_video_shop),
                    startX,
                    width,
                    CLICK_VIDEO_SHOP_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.fb -> {
                val app = AppDetailModel(
                    getString(R.string.facebook), startX, width, FB_URL, gravity
                )

                startWebApp(app)


            }

            R.id.twitter -> {

                val app = AppDetailModel(
                    getString(R.string.twitter), startX, width, TWITTER_URL, gravity
                )
                startWebApp(app)
            }

            R.id.linkedin -> {


                val app = AppDetailModel(
                    getString(R.string.linkedin), startX, width, LINKDIN_URL, gravity
                )

                startWebApp(app)
            }

            R.id.citi_bank -> {

                val app = AppDetailModel(
                    getString(R.string.citi_bank),
                    startX,
                    width,
                    CITIBANK_URL,
                    gravity
                )
                startWebApp(app)
            }

            R.id.internet -> {

                val app = AppDetailModel(
                    getString(R.string.interactive_internet),
                    resources.getInteger(R.integer.b_start),
                    resources.getInteger(R.integer.b_width),
                    GOOGLE_URL,
                    gravity=gravity
                )
                startWebApp(app)
            }

            R.id.app_phone_system -> {

                val app = AppDetailModel(
                    getString(R.string.phone_system),
                    startX,
                    width,
                    gravity = Gravity.START
                )
                startDialerApp(app)


            }

            R.id.app_cool_tv -> {

                val app = AppDetailModel(
                    getString(R.string.cool_tv),
                    resources.getInteger(R.integer.b_start),
                    resources.getInteger(R.integer.b_width),
                    gravity = Gravity.CENTER
                )
                startCoolTvApp(app)

            }

            R.id.app_settings -> {
                val app = AppDetailModel(
                    getString(R.string.setting_screen),
                    resources.getInteger(R.integer.b_start),
                    resources.getInteger(R.integer.b_width),
                    gravity = Gravity.CENTER
                )
                startSettingScreen(app)
            }

            R.id.app_messages -> {
                val app = AppDetailModel(
                    getString(R.string.messages),
                    resources.getInteger(R.integer.b_start),
                    resources.getInteger(R.integer.b_width),
                    gravity = Gravity.CENTER
                )
                startMessagesApp(app)
            }

            R.id.app_muticast -> {
                val app = AppDetailModel(
                    name = getString(R.string.multicast_system),
                    startX = resources.getInteger(R.integer.c_start),
                    width = width,
                    gravity = Gravity.END
                )

                startMulticastApp(app)
            }

            else -> {

            }
        }/* } else {
             requestOverlayDisplayPermission()
         }*/
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    override fun closeWindow(app: AppDetailModel) {
        mainVM.appList.remove(app)
        if (mainVM.multicastList.isNotEmpty()) {
            mainVM.removeMulticast(app)
        }
        if (mainVM.gameMulticastList.isNotEmpty()) {
            mainVM.removeMulticast(app)
        }

        if (app.name == getString(R.string.cool_tv)) {
            if (mainVM.clickVideoApp != null) {
                mainVM.clickVideoApp!!.close()
            }
            if (mainVM.coolEcall != null) {
                mainVM.coolEcall!!.close()
            }
            if (mainVM.bidApp != null) {
                mainVM.bidApp!!.close()
            }
            if (mainVM.linkApp != null) {
                mainVM.linkApp!!.close()
            }
            if (mainVM.entertainApp != null) {
                mainVM.entertainApp!!.close()
            }
        }
    }

    private fun startWebApp(
        app: AppDetailModel,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
    ) {
        if (!mainVM.appList.contains(app)) {
            val webApp = WebApp(
                app,
                this,
                lifecycle,
                this,
                appList = mainVM.appList,
                mBinding,
                configurationLiveData = mainVM.configurationLiveData
            )
            webApp.start()
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, webApp))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }

            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, webApp))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, webApp))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }

            if (app.name == getString(R.string.txt_link)) {
                mainVM.linkApp = webApp
            } else if (app.name == getString(R.string.txt_entertain)) {
                mainVM.linkApp = webApp
            }
        }
    }

    private fun startMulticastApp(app: AppDetailModel, isResume: Boolean = false) {

        if (isPortrait()) {
            return
        }
        val onClick = OnClickListener {
            when (it.id) {
                R.id.img_keypad -> {
                    val keyBoardApp = AppDetailModel(
                        name = getString(R.string.key_board),
                        startX = resources.getInteger(R.integer.a_start),
                        width = (resources.getInteger(R.integer.a_width) + resources.getInteger(R.integer.b_width)),
                        gravity = Gravity.START,
                        height = MULTICAST_KEYBOARD_HEIGHT

                    )
                    startKeyBoardApp(keyBoardApp)
                }

                R.id.img_forward -> {
                    if (mainVM.multicastList.size > 1) {
                        val nextIndex: Int = (mainVM.multicastCurrentIndex + 1) % mainVM.multicastList.size
                        val multicastModel = mainVM.multicastList[nextIndex]

                        when (multicastModel.title) {
                            getString(R.string.mail) -> {
                                (multicastModel.app as EmailApp).top()
                            }

                            getString(R.string.messages) -> {
                                (multicastModel.app as MessageApp).top()
                            }

                            getString(R.string.setting_screen) -> {
                                (multicastModel.app as Setting).top()
                            }

                            else -> {
                                (multicastModel.app as WebApp).top()
                            }
                        }

                        mainVM.multicastCurrentIndex = nextIndex
                    }
                }

                R.id.img_backward -> {
                    if (mainVM.multicastList.size > 1) {
                        val previousIndex: Int =
                            (mainVM.multicastCurrentIndex + mainVM.multicastList.size - 1) % mainVM.multicastList.size

                        val multicastModel = mainVM.multicastList[previousIndex]

                        when (multicastModel.title) {
                            getString(R.string.mail) -> {
                                (multicastModel.app as EmailApp).top()
                            }

                            getString(R.string.messages) -> {
                                (multicastModel.app as MessageApp).top()
                            }

                            getString(R.string.setting_screen) -> {
                                (multicastModel.app as Setting).top()
                            }

                            else -> {
                                (multicastModel.app as WebApp).top()
                            }
                        }

                        mainVM.multicastCurrentIndex = previousIndex
                    }
                }

                R.id.img_mail -> {
                    startEmailApp(
                        AppDetailModel(
                            name = getString(R.string.mail),
                            startX = mainVM.getOpenApp(getString(R.string.multicast_system))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.multicast_system))?.width ?: 0,
                            height = MULTI_CAST_APP_HEIGHT,
                            gravity = Gravity.END
                        ), true
                    )
                }

                /*R.id.fb -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.facebook),
                            startX = mainVM.getOpenApp(getString(R.string.multicast_system))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.multicast_system))?.width ?: 0,
                            webUrl = FB_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), true
                    )
                }

                R.id.twitter -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.twitter),
                            startX = mainVM.getOpenApp(getString(R.string.multicast_system))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.multicast_system))?.width ?: 0,
                            webUrl = TWITTER_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), true
                    )

                }

                R.id.app_discovery -> {


                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.discovery),
                            startX = mainVM.getOpenApp(getString(R.string.multicast_system))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.multicast_system))?.width ?: 0,
                            webUrl = DISCOVERY_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isMultiCast = true
                    )
                }

                R.id.app_barcode -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.barcode),
                            startX = mainVM.getOpenApp(getString(R.string.multicast_system))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.multicast_system))?.width ?: 0,
                            webUrl = BARCODE_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isMultiCast = true
                    )
                }*/
            }
        }

        if (!mainVM.appList.contains(app)) {
            MulticastApp(appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                onClick = onClick,
                snapToGridListener = this,
                configurationLiveData = mainVM.configurationLiveData,
                homeScreenAppListAdapter!!.appClickLiveData,
                onMulticastAppClick = { startX, width, height, itemModel ->
                    onMulticastAppClick(itemModel, startX, width, height)
                }

            ).start()
            mainVM.appList.add(app)

            if (isResume) {
                startEmailApp(
                    AppDetailModel(
                        name = getString(R.string.mail),
                        startX = resources.getInteger(R.integer.c_start),
                        width = resources.getInteger(R.integer.c_width),
                        height = MULTI_CAST_APP_HEIGHT,
                        gravity = Gravity.END
                    ), isMultiCast = true, isResume = true
                )
            }
        }
    }

    private fun startDialerApp(app: AppDetailModel, isResume: Boolean = false) {

        if (isPortrait()) {
            return
        }


        if (!mainVM.appList.contains(app)) {
            PhoneSystemApp(app,
                this,
                lifecycle,
                this,
                appList = mainVM.appList,
                mBinding,
                isResume = isResume,
                this,
                mainVM.configurationLiveData,
                appClickLiveData = homeScreenAppListAdapter!!.appClickLiveData,
                onMulticastAppClick = { startX, width, height, itemModel ->
                    onMulticastAppClick(itemModel, startX, width, height)
                }).start()
            mainVM.appList.add(app)
        }
    }

    private fun startCoolTvApp(app: AppDetailModel, isResume: Boolean = false) {
        if (!mainVM.appList.contains(app)) {
            CoolTvApp(
                app,
                this,
                lifecycle,
                this,
                appList = mainVM.appList,
                mBinding,
                isResume,
                mainVM.configurationLiveData,
                coolTvAppLivedata
            ).start()
            mainVM.appList.add(app)
        }
    }

    private fun startSettingScreen(app: AppDetailModel, isResume: Boolean = false) {
        if (!mainVM.appList.contains(app)) {
            Setting(
                app, this, lifecycle, this, appList = mainVM.appList, mBinding, isResume, mainVM.configurationLiveData
            ).start()
            mainVM.appList.add(app)
        }
    }


    private fun startVideoGameApp(
        app: AppDetailModel,
        gameUrl: String,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
    ) {
        if (isPortrait()) {
            return
        }

        val videoGameClick = OnClickListener {
            when (it.id) {
                R.id.img_icon_forward -> {
                    if (mainVM.gameMulticastList.size > 1) {
                        val nextIndex: Int = (mainVM.gameMulticastCurrentIndex + 1) % mainVM.gameMulticastList.size
                        val multicastModel = mainVM.gameMulticastList[nextIndex]

                        if (multicastModel.title == getString(R.string.mail)) {
                            (multicastModel.app as EmailApp).top()
                        } else {
                            (multicastModel.app as WebApp).top()
                        }

                        mainVM.gameMulticastCurrentIndex = nextIndex
                    }
                }

                R.id.img_icon_backward -> {
                    if (mainVM.gameMulticastList.size > 1) {
                        val previousIndex: Int =
                            (mainVM.gameMulticastCurrentIndex + mainVM.gameMulticastList.size - 1) % mainVM.gameMulticastList.size

                        val multicastModel = mainVM.gameMulticastList[previousIndex]

                        if (multicastModel.title == getString(R.string.mail)) {
                            (multicastModel.app as EmailApp).top()
                        } else {
                            (multicastModel.app as WebApp).top()
                        }

                        mainVM.gameMulticastCurrentIndex = previousIndex
                    }
                }

                /*R.id.fb -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.facebook),
                            startX = mainVM.getOpenApp(getString(R.string.txt_video_game))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.txt_video_game))?.width ?: 0,
                            webUrl = FB_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isGameMultiCast = true
                    )
                }

                R.id.twitter -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.twitter),
                            startX = mainVM.getOpenApp(getString(R.string.txt_video_game))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.txt_video_game))?.width ?: 0,
                            webUrl = TWITTER_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isGameMultiCast = true
                    )
                }

                R.id.app_discovery -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.discovery),
                            startX = mainVM.getOpenApp(getString(R.string.txt_video_game))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.txt_video_game))?.width ?: 0,
                            webUrl = DISCOVERY_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isGameMultiCast = true
                    )
                }

                R.id.app_amazon -> {
                    val amazonApp = AppDetailModel(
                        getString(R.string.amazon),
                        startX = mainVM.getOpenApp(getString(R.string.txt_video_game))?.startX ?: 0,
                        width = mainVM.getOpenApp(getString(R.string.txt_video_game))?.width ?: 0,
                        AMAZON_URL,
                        Gravity.START,
                        height = MULTI_CAST_APP_HEIGHT,
                    )
                    startWebApp(amazonApp, isGameMultiCast = true)
                }

                R.id.app_barcode -> {
                    startWebApp(
                        AppDetailModel(
                            name = getString(R.string.barcode),
                            startX = mainVM.getOpenApp(getString(R.string.txt_video_game))?.startX ?: 0,
                            width = mainVM.getOpenApp(getString(R.string.txt_video_game))?.width ?: 0,
                            webUrl = BARCODE_URL,
                            gravity = Gravity.END,
                            height = MULTI_CAST_APP_HEIGHT
                        ), isGameMultiCast = true
                    )
                }*/

            }

        }

        if (!mainVM.appList.contains(app)) {
            val windowApp = VideoGameApp(app,
                this,
                lifecycle,
                this,
                appList = mainVM.appList,
                mBinding,
                gameUrl,
                onClickListener = videoGameClick,
                configurationLiveData = mainVM.configurationLiveData,
                appClickLiveData = homeScreenAppListAdapter!!.appClickLiveData,
                onMulticastAppClick = { startX, width, height, itemModel ->
                    onMulticastAppClick(itemModel, startX, width, height)
                })
            windowApp.start()
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, windowApp))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }
            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, windowApp))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, windowApp))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }
        }
    }

    private fun startMessagesApp(
        app: AppDetailModel,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
    ) {
        if (!mainVM.appList.contains(app)) {
            val windowApp = MessageApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                mainVM.configurationLiveData
            )
            windowApp.start()
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, windowApp))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }

            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, windowApp))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, windowApp))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }
        }
    }

    fun startBuyVideoApp(app: AppDetailModel) {
        if (!mainVM.appList.contains(app)) {
            mainVM.clickVideoApp = BuyVideoApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                configurationLiveData = mainVM.configurationLiveData,
            )
            mainVM.clickVideoApp!!.start()
            mainVM.appList.add(app)
        }
    }

    fun startCoolECallApp(app: AppDetailModel) {
        if (!mainVM.appList.contains(app)) {
            mainVM.coolEcall = CoolEcall(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                configurationLiveData = mainVM.configurationLiveData
            )
            mainVM.coolEcall!!.start()
            mainVM.appList.add(app)
        }
    }

    fun startInteractApp(app: AppDetailModel) {
        if (!mainVM.appList.contains(app)) {
            InteractApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                configurationLiveData = mainVM.configurationLiveData
            ).start()
            mainVM.appList.add(app)
        }
    }

    fun startBidApp(app: AppDetailModel, onBidSent: (() -> Unit)) {
        if (!mainVM.appList.contains(app)) {
            mainVM.bidApp = BidApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                configurationLiveData = mainVM.configurationLiveData,
                onBidSent = onBidSent
            )
            mainVM.bidApp!!.start()
            mainVM.appList.add(app)
        }
    }


    fun startLinkApp(app: AppDetailModel) {
        startWebApp(app)
    }

    fun startEntertainApp(app: AppDetailModel) {
        startWebApp(app)
    }

    private fun startEmailApp(
        app: AppDetailModel,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
        isResume: Boolean = false,
    ) {
        if (!mainVM.appList.contains(app)) {
            val emailApp = EmailApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                mainVM.configurationLiveData,
                onFocusClick = onEditTextClick,
                mainVM.keyTextLiveData,
            )
            emailApp.start()
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, emailApp))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }

            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, emailApp))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, emailApp))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }

            if (isResume) {
                emailApp.resumeDefaultText()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onEditTextClick = OnFocusChangeListener { _, hasFocus ->
        val keyBoardApp = AppDetailModel(
            name = getString(R.string.key_board),
            startX = resources.getInteger(R.integer.a_start),
            width = (resources.getInteger(R.integer.a_width) + resources.getInteger(R.integer.b_width)),
            gravity = Gravity.START,
            height = MULTICAST_KEYBOARD_HEIGHT

        )
        if (hasFocus) startKeyBoardApp(keyBoardApp)
    }

    private fun startKeyBoardApp(
        app: AppDetailModel,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
        isResume: Boolean = false,
    ) {
        if (!mainVM.appList.contains(app)) {
            keyboardApp = KeyboardApp(
                appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                mainVM.configurationLiveData,
                onMulticastAppClick = { startX, width, height, itemModel ->
                    onMulticastAppClick(itemModel, startX, width, height)
                },
                mainVM.keyTextLiveData
            )
            keyboardApp.start()
            mainVM.isKeyboardOpen.value = true
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, keyboardApp))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }
            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, keyboardApp))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, keyboardApp))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }

            if (isResume) {
//                emailApp.resumeDefaultText()
            }
        }
    }

    private fun startVideoCallWithKeyBoardApp(
        app: AppDetailModel,
        isResume: Boolean = false,
        isMultiCast: Boolean = false,
        isGameMultiCast: Boolean = false,
        isPhoneSystem: Boolean = false,
        isVideoCallFromKeyBoard: Boolean = false,
    ) {
        if (!mainVM.appList.contains(app)) {

            val videoCallWithKeyBoard = VideoCallWithKeyboardApp(appDetailModel = app,
                context = this,
                lifecycle = lifecycle,
                closeWindowListener = this,
                appList = mainVM.appList,
                mBinding = mBinding,
                mainVM.configurationLiveData,
                onMulticastAppClick = { startX, width, height, itemModel ->
                    onMulticastAppClick(itemModel, startX, width, height)
                })
            videoCallWithKeyBoard.start()
            mainVM.appList.add(app)
            if (isMultiCast) {
                mainVM.multicastList.add(MulticastModel(app.name, videoCallWithKeyBoard))
                mainVM.multicastCurrentIndex = mainVM.multicastList.size.minus(1)
            }

            if (isGameMultiCast) {
                mainVM.gameMulticastList.add(MulticastModel(app.name, videoCallWithKeyBoard))
                mainVM.gameMulticastCurrentIndex = mainVM.gameMulticastList.size.minus(1)
            }
            if (isPhoneSystem) {
                mainVM.phoneSystemAppList.add(MulticastModel(app.name, videoCallWithKeyBoard))
                mainVM.phoneSystemCurrentIndex = mainVM.phoneSystemAppList.size.minus(1)
            }
            if (isVideoCallFromKeyBoard) {
                mainVM.videoCallWithKeyboardAppList.add(
                    MulticastModel(
                        app.name, videoCallWithKeyBoard
                    )
                )
                mainVM.videoCallWithKeyboardAppCurrentIndex = mainVM.videoCallWithKeyboardAppList.size.minus(1)
            }
        }
    }


    private fun onMulticastAppClick(
        itemModel: AppModel,
        startX: Int,
        width: Int,
        height: Int = 0,
    ) {
        when (itemModel.id) {
            R.id.app_camera -> {
                startActivity(Intent(this, CameraActivity::class.java))
            }

            R.id.app_gallery -> {
                startActivity(Intent(this, GalleryActivity::class.java))
            }

            R.id.instagram -> {
                val app = AppDetailModel(
                    getString(R.string.instagram),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = INSTAGRAM_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_whatsapp -> {
                val app = AppDetailModel(
                    getString(R.string.whatsapp),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = WHATSAPP_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_amazon -> {
                val app = AppDetailModel(
                    name = getString(R.string.amazon),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = AMAZON_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_uber -> {
                val app = AppDetailModel(
                    getString(R.string.txt_uber),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = UBER_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_bumble -> {
                val app = AppDetailModel(
                    getString(R.string.bumble),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = BUMBLE_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_espn -> {
                val app = AppDetailModel(
                    getString(R.string.espn_score_center),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = ESPN_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_yelp -> {
                val app = AppDetailModel(
                    getString(R.string.yelp),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = YELP_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_snap_chat -> {
                val app = AppDetailModel(
                    getString(R.string.snapchat),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = SNAP_CHAT_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_google_meet -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_meet),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = GOOGLE_MEET_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_venmo -> {
                val app = AppDetailModel(
                    getString(R.string.txt_venmo),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = VENMO_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_hinge -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hinge),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = HINGE_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_muscle_booster_workout_planner -> {
                val app = AppDetailModel(
                    getString(R.string.txt_muscle_booster_workout_planner),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = MUSCLE_BOOSTER_WORKOUT_PLANNER_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_google_classroom -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_classroom),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = GOOGLE_CLASSROOM_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_ebay -> {
                val app = AppDetailModel(
                    getString(R.string.txt_ebay),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = EBAY_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_reddit -> {
                val app = AppDetailModel(
                    getString(R.string.txt_reddit),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = REDDIT_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_booking_com -> {
                val app = AppDetailModel(
                    getString(R.string.txt_booking),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = BOOKING_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_group_me -> {
                val app = AppDetailModel(
                    getString(R.string.txt_group_me),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = GROUP_ME_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_vrbo -> {
                val app = AppDetailModel(
                    getString(R.string.txt_vrbo),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = VRBO_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_tiktok -> {
                val app = AppDetailModel(
                    getString(R.string.txt_tiktok),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = TIKTOK_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_google_drive -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_drive),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = GOOGLE_DRIVE_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_outlook -> {
                val app = AppDetailModel(
                    getString(R.string.txt_outlook),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = OUTLOOK_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_youtube_music -> {
                val app = AppDetailModel(
                    getString(R.string.txt_youtube_music),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = YOUTUBE_MUSIC_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_news_break -> {
                val app = AppDetailModel(
                    getString(R.string.txt_news_break),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = NEWS_BREAK_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_drop_box -> {
                val app = AppDetailModel(
                    getString(R.string.dropbox),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = DROP_BOX_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_npr_news -> {
                val app = AppDetailModel(
                    getString(R.string.npr_news),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = NPR_NEWS_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_webmd -> {
                val app = AppDetailModel(
                    getString(R.string.web_md_mobile),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = WEBMD_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_weather -> {
                val app = AppDetailModel(
                    getString(R.string.weather_channel),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = WEATHER_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_pandora_radio -> {
                val app = AppDetailModel(
                    getString(R.string.pandora_radio),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = PANDORA_RADIO_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_youtube -> {
                val app = AppDetailModel(
                    getString(R.string.youtube),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = YOUTUBE_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_google_keep -> {
                val app = AppDetailModel(
                    getString(R.string.txt_google_keep),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = GOOGLE_KEEP_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_blinkist -> {
                val app = AppDetailModel(
                    getString(R.string.txt_blinkist),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = BLINKIST_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_chime -> {
                val app = AppDetailModel(
                    getString(R.string.txt_chime),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CHIME_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_cash_app -> {
                val app = AppDetailModel(
                    getString(R.string.txt_cash_app),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CASH_APP_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_hbo_max -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hbo_max),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = HBO_MAX_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_uber_eats -> {
                val app = AppDetailModel(
                    getString(R.string.txt_uber_eats),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = UBER_EATS_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_disney_plus -> {
                val app = AppDetailModel(
                    getString(R.string.txt_disney_plus),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = DISNEY_PLUS_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_temu -> {
                val app = AppDetailModel(
                    getString(R.string.txt_temu),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = TEMU_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_turbo_tax -> {
                val app = AppDetailModel(
                    getString(R.string.txt_turbo_tax),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = TURBO_TAX_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_walmart -> {
                val app = AppDetailModel(
                    getString(R.string.txt_walmart),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = WALMART_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_door_dash -> {
                val app = AppDetailModel(
                    getString(R.string.txt_door_dash),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = DOOR_DASH_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_pinterest -> {
                val app = AppDetailModel(
                    getString(R.string.txt_pinterest),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = PINTEREST_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_indeed -> {
                val app = AppDetailModel(
                    getString(R.string.txt_indeed),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = INDEED_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_hopper -> {
                val app = AppDetailModel(
                    getString(R.string.txt_hopper),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = HOPPER_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_kfc -> {
                val app = AppDetailModel(
                    getString(R.string.txt_kfc),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = KFC_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }


            R.id.app_barcode -> {
                val app = AppDetailModel(
                    getString(R.string.barcode),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = BARCODE_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_discovery -> {
                val app = AppDetailModel(
                    getString(R.string.discovery),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = DISCOVERY_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_cnn_news -> {
                val app = AppDetailModel(
                    getString(R.string.txt_cnn_news),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CNN_NEWS_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_zoom -> {
                val app = AppDetailModel(
                    getString(R.string.txt_zoom),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = ZOOM_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_air_bnb -> {
                val app = AppDetailModel(
                    getString(R.string.txt_airbnb),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = AIR_BNB_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_expedia -> {
                val app = AppDetailModel(
                    getString(R.string.txt_expedia),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = EXPEDIA_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_amazon_music -> {
                val app = AppDetailModel(
                    getString(R.string.txt_amazon_music),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = AMAZON_MUSIC_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_itunes -> {
                val app = AppDetailModel(
                    getString(R.string.txt_itunes),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = ITUNES_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_click_video_shop -> {
                val app = AppDetailModel(
                    getString(R.string.txt_click_video_shop),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CLICK_VIDEO_SHOP_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_netflix -> {
                val app = AppDetailModel(
                    getString(R.string.txt_netflix),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = NETFLIX_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.tv_click_vedio_shop -> {
                val app = AppDetailModel(
                    getString(R.string.txt_click_video_shop),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CLICK_VIDEO_SHOP_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.fb -> {
                val app = AppDetailModel(
                    getString(R.string.facebook),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = FB_URL,
                    gravity = Gravity.END
                )

                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )


            }

            R.id.twitter -> {

                val app = AppDetailModel(
                    getString(R.string.twitter),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = TWITTER_URL,
                    gravity = Gravity.END
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.linkedin -> {


                val app = AppDetailModel(
                    getString(R.string.linkedin),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = LINKDIN_URL,
                    gravity = Gravity.END
                )

                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.citi_bank -> {

                val app = AppDetailModel(
                    getString(R.string.citi_bank),
                    startX = startX,
                    width = width,
                    height = height,
                    webUrl = CITIBANK_URL,
                    gravity = Gravity.START
                )
                startWebApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            R.id.app_settings -> {
                val app = AppDetailModel(
                    getString(R.string.setting_screen),
                    startX = startX,
                    width = width,
                    height = height,
                    gravity = Gravity.CENTER
                )
                startSettingScreen(app)
            }

            R.id.app_messages -> {
                val app = AppDetailModel(
                    getString(R.string.messages), startX = startX, width = width, height = height, gravity = Gravity.CENTER
                )
                startMessagesApp(
                    app,
                    isMultiCast = itemModel.isMulticastApp,
                    isGameMultiCast = itemModel.isVideoGameApp,
                    isPhoneSystem = itemModel.isPhoneSystemApp
                )
            }

            else -> {

            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemBars()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideKeyboard()
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_main)
            constraintSet.applyTo(mBinding.layoutMain)
            mBinding.flowC.setOrientation(Flow.HORIZONTAL)
            mBinding.recyclerViewAppListA.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
            mBinding.recyclerViewAppListC.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_main_portraite)
            constraintSet.applyTo(mBinding.layoutMain)
            mBinding.flowC.setOrientation(Flow.VERTICAL)
            mBinding.recyclerViewAppListA.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false)
            mBinding.recyclerViewAppListC.layoutManager = GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false)
        }

        mBinding.root.post {
            mainVM.configurationLiveData.postValue(newConfig)
        }


    }

    override fun moveView(app: AppDetailModel) {
        if (app.name == getString(R.string.multicast_system)) {
            val videoApp = AppDetailModel(
                getString(R.string.txt_video_game),
                startX = resources.getInteger(R.integer.b_start),
                resources.getInteger(R.integer.b_width) + resources.getInteger(R.integer.c_width),
                gravity = Gravity.CENTER,
                height = resources.getInteger(R.integer.b_height)
            )
            startVideoGameApp(videoApp, CALL_OF_DUTY_GAME_VIDEO_URL)
        } else if (app.name == getString(R.string.phone_system)) {
            val keyBoardApp = AppDetailModel(
                name = getString(R.string.txt_chat_video_call),
                startX = resources.getInteger(R.integer.a_start),
                width = (resources.getInteger(R.integer.a_width) + resources.getInteger(R.integer.b_width)),
                gravity = Gravity.START,
                height = resources.getInteger(R.integer.b_height)

            )
            startVideoCallWithKeyBoardApp(keyBoardApp, isVideoCallFromKeyBoard = true)
        }
    }
}

