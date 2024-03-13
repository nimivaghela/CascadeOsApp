package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.cascadeos.R
import com.app.cascadeos.adapter.LockerAppListAdapter
import com.app.cascadeos.adapter.LockerMenuAdapter
import com.app.cascadeos.databinding.ActivityDigitalLockerBinding
import com.app.cascadeos.model.DigitalLockerItemModel
import com.app.cascadeos.model.DigitalLockerMenuModel
import com.app.cascadeos.model.HasViewButton.*
import com.app.cascadeos.model.MediaType
import com.app.cascadeos.model.MediaType.*
import com.app.cascadeos.utility.*
import com.app.cascadeos.viewmodel.MainVM


class DigitalLockerActivity : AppCompatActivity(), LockerAppListAdapter.ClickListener {
    private lateinit var mBinding: ActivityDigitalLockerBinding
    private val itemList: ArrayList<DigitalLockerItemModel> = arrayListOf()
    private lateinit var lockerAdapter: LockerAppListAdapter
    private lateinit var lockerMenuAdapter: LockerMenuAdapter
    private val appBarHandler = Handler(Looper.getMainLooper())
    private val lockerMenuItemList: ArrayList<DigitalLockerMenuModel> = arrayListOf()

    private lateinit var lockerPreference: SharedPreferences
    private lateinit var prefEditor: SharedPreferences.Editor

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_digital_locker)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_digital_locker)
        mBinding.layoutMain.layoutTransition = LayoutTransition()


        lockerPreference = getSharedPreferences(LOCKER_MENU_PREFERENCE, Context.MODE_PRIVATE)


        val mediaType = intent.getStringExtra(SELECTED_MEDIA_TYPE)

        mediaType?.let {
            updateLockerPreference(MediaType.valueOf(it))
        }


        mBinding.apply {

            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text = getString(R.string.digital_locker)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close).setOnClickListener {
                MainVM.coolTvAppLivedata.postValue(getString(R.string.digital_locker))
                finish()
            }

            lockerAdapter =
                LockerAppListAdapter(this@DigitalLockerActivity, arrayListOf(), this@DigitalLockerActivity) { itemModel ->
                    when (itemModel.mediaType) {
                        VIDEO -> {
                            VideoPlayerActivity.start(this@DigitalLockerActivity, itemModel.webUrl)
                        }

                        GAMES -> {
                            val intent = Intent()
                            intent.putExtra(GAME_URL, itemModel.webUrl)
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }

                        AUDIO -> {
                            VideoPlayerActivity.start(this@DigitalLockerActivity, itemModel.webUrl)
                        }

                        FILES -> {
                            DocumentViewerActivity.start(
                                this@DigitalLockerActivity,
                                fileUrl = itemModel.webUrl.toString(),
                                fileName = itemModel.itemName
                            )
                        }

                        PHOTOS -> {
                            ImageViewerActivity.start(this@DigitalLockerActivity, itemModel.webUrl.toString())
                        }
                    }
                }

            lockerRecyclerview.layoutManager = LinearLayoutManager(
                this@DigitalLockerActivity, LinearLayoutManager.HORIZONTAL, false
            )
            lockerRecyclerview.adapter = lockerAdapter
            layoutHiddenActionbar.setOnClickListener {
                actionBar.actionBar.visibility = View.VISIBLE
                hideAppBar()
            }

            imgArrowBackward.setOnClickListener {
                val positionToMove =
                    (lockerRecyclerview.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                lockerRecyclerview.smoothScrollToPosition(
                    if (positionToMove != 0) positionToMove - 1 else 0
                )
            }
            imgArrowForward.setOnClickListener {
                val positionToMove =
                    (lockerRecyclerview.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                lockerRecyclerview.smoothScrollToPosition(if (positionToMove != itemList.size - 1) positionToMove + 1 else itemList.size - 1)
            }
        }

        hideAppBar()

    }

    private fun setDataAsMenuSelected() {
        when (lockerMenuItemList[lockerMenuAdapter.selectedItemPosition].mediaType) {
            VIDEO -> {
                mBinding.tvPurchasedVideos.setText(R.string.purchased_videos)
                loadVideos()
                updateLockerPreference(VIDEO)
            }

            GAMES -> {
                mBinding.tvPurchasedVideos.setText(R.string.purchased_games)
                loadGames()
                updateLockerPreference(GAMES)
            }

            AUDIO -> {
                mBinding.tvPurchasedVideos.setText(R.string.purchased_audios)
                loadAudios()
                updateLockerPreference(AUDIO)
            }

            FILES -> {
                mBinding.tvPurchasedVideos.setText(R.string.purchased_files)
                loadFiles()
                updateLockerPreference(FILES)
            }

            PHOTOS -> {
                mBinding.tvPurchasedVideos.setText(R.string.purchased_photos)
                loadPhotos()
                updateLockerPreference(PHOTOS)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (lockerMenuItemList.isNotEmpty()) lockerMenuItemList.clear()
        mBinding.imgPrevious.setOnClickListener {
            lockerMenuAdapter.selectedItemPosition =
                if (lockerMenuAdapter.selectedItemPosition != 0) lockerMenuAdapter.selectedItemPosition - 1 else 0
            lockerMenuItemList[lockerMenuAdapter.selectedItemPosition].isSelected = true
            lockerMenuAdapter.notifyDataSetChanged()
            mBinding.rvLockerOptions.smoothScrollToPosition(lockerMenuAdapter.selectedItemPosition)
            setDataAsMenuSelected()
        }
        mBinding.imgNext.setOnClickListener {
            lockerMenuAdapter.selectedItemPosition =
                if (lockerMenuAdapter.selectedItemPosition != lockerMenuItemList.size - 1) lockerMenuAdapter.selectedItemPosition + 1 else lockerMenuItemList.size - 1
            lockerMenuItemList[lockerMenuAdapter.selectedItemPosition].isSelected = true
            lockerMenuAdapter.notifyDataSetChanged()
            mBinding.rvLockerOptions.smoothScrollToPosition(lockerMenuAdapter.selectedItemPosition)
            setDataAsMenuSelected()
        }
        lockerMenuItemList.add(DigitalLockerMenuModel(R.string.purchased_audios, R.drawable.ic_audio, AUDIO))
        lockerMenuItemList.add(DigitalLockerMenuModel(R.string.purchased_videos, R.drawable.ic_video, VIDEO))
        lockerMenuItemList.add(DigitalLockerMenuModel(R.string.purchased_games, R.drawable.ic_game, GAMES))
        lockerMenuItemList.add(DigitalLockerMenuModel(R.string.purchased_files, R.drawable.ic_files, FILES))
        lockerMenuItemList.add(DigitalLockerMenuModel(R.string.purchased_photos, R.drawable.ic_photos, PHOTOS))

        if (getLockerPreference().isNotEmpty()) {
            for (i in lockerMenuItemList.indices) {
                if (lockerMenuItemList[i].mediaType.toString() == getLockerPreference()) {
                    lockerMenuItemList[i].isSelected = true
                }
            }
        } else {
            lockerMenuItemList[0].isSelected = true
        }

        lockerMenuAdapter = LockerMenuAdapter(this@DigitalLockerActivity, lockerMenuItemList) { selectedItem ->
            when (selectedItem.mediaType) {
                AUDIO -> {
                    mBinding.tvPurchasedVideos.setText(R.string.purchased_audios)
                    loadAudios()
                    updateLockerPreference(AUDIO)
                }

                VIDEO -> {
                    mBinding.tvPurchasedVideos.setText(R.string.purchased_videos)
                    loadVideos()
                    updateLockerPreference(VIDEO)
                }

                GAMES -> {
                    mBinding.tvPurchasedVideos.setText(R.string.purchased_games)
                    loadGames()
                    updateLockerPreference(GAMES)
                }

                FILES -> {
                    mBinding.tvPurchasedVideos.setText(R.string.purchased_files)
                    loadFiles()
                    updateLockerPreference(FILES)
                }

                PHOTOS -> {
                    mBinding.tvPurchasedVideos.setText(R.string.purchased_photos)
                    loadPhotos()
                    updateLockerPreference(PHOTOS)
                }
            }

        }
        mBinding.rvLockerOptions.adapter = lockerMenuAdapter
    }

    private fun loadGames() {
        mBinding.lockerRecyclerview.scrollToPosition(0)
        if (itemList.isNotEmpty()) itemList.clear()

        itemList.add(DigitalLockerItemModel(R.drawable.ic_game1, CALL_OF_DUTY_GAME_VIDEO_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game2, CONTRACT_KILLER_GAME_VIDEO_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game3, ANGRY_BIRD_GAME_VIDEO_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game4, GT_RACING_GAME_VIDEO_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game5, FORMULA_1_GAME_VIDEO_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game6, MINE_CRAFT_GAME_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game7, GRAND_THEFT_AUTO_GAME_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game8, PUBG_BATTLEGROUND_GAME_URL, GAMES, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_game9, CANDY_CRUSH_GAME_URL, GAMES, TRUE))

        lockerAdapter.addItems(itemList)
    }

    private fun loadVideos() {
        mBinding.lockerRecyclerview.scrollToPosition(0)
        if (itemList.isNotEmpty()) itemList.clear()

        itemList.add(DigitalLockerItemModel(R.drawable.ic_video1, AVTAR_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video2, SPIDER_MAN_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video3, STAR_WARS_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video4, FOREST_GUMP_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video5, ET_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video6, BACK_TO_FUTURE_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video7, JOHN_WICK_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video8, INCEPTION_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video9, JURASSIC_WORLD_URL, VIDEO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_video10, AVENGERS_URL, VIDEO, TRUE))

        lockerAdapter.addItems(itemList)
    }

    private fun loadAudios() {
        mBinding.lockerRecyclerview.scrollToPosition(0)
        if (itemList.isNotEmpty()) itemList.clear()

        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio1, BABY_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio2, BAD_BOY_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio3, WAKA_WAKA_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio4, UNSTOPPABLE_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio5, LOVE_MY_SELF_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio6, BETTER_PLACE_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio7, DESPACITO_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio8, UPTOWN_FUNK_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio9, GANGNAM_STYLE_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio10, DURA_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio11, SEE_YOU_AGAIN_URL, AUDIO, TRUE))
        itemList.add(DigitalLockerItemModel(R.drawable.ic_audio12, SUGAR_URL, AUDIO, TRUE))

        lockerAdapter.addItems(itemList)
    }

    private fun loadFiles() {
        mBinding.lockerRecyclerview.smoothScrollToPosition(0)
        if (itemList.isNotEmpty()) itemList.clear()

        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_ppt,
                FILE_1_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document1)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_word,
                FILE_2_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document2)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_3_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document3)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_4_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document4)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_5_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document5)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_6_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document6)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_7_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document7)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_8_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document8)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_9_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document9)
            )
        )
        itemList.add(
            DigitalLockerItemModel(
                R.drawable.ic_doc_pdf,
                FILE_10_URL,
                FILES,
                FALSE,
                itemName = getString(R.string.document10)
            )
        )

        lockerAdapter.addItems(itemList)
    }

    private fun loadPhotos() {
        mBinding.lockerRecyclerview.scrollToPosition(0)
        if (itemList.isNotEmpty()) itemList.clear()

        itemList.add(DigitalLockerItemModel(R.drawable.image01, PHOTO_1_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image02, PHOTO_2_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image03, PHOTO_3_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image04, PHOTO_4_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image05, PHOTO_5_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image06, PHOTO_6_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image07, PHOTO_7_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image08, PHOTO_8_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image09, PHOTO_9_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image10, PHOTO_10_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image11, PHOTO_11_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image12, PHOTO_12_URL, PHOTOS, FALSE))
        itemList.add(DigitalLockerItemModel(R.drawable.image13, PHOTO_13_URL, PHOTOS, FALSE))

        lockerAdapter.addItems(itemList)
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

    private fun updateLockerPreference(selectedOption: MediaType) {
        prefEditor = lockerPreference.edit()
        prefEditor.putString(LOCKER_MENU_SELECTED, selectedOption.toString())
        prefEditor.apply()
    }

    private fun clearLockerPreference() {
        prefEditor = lockerPreference.edit()
        prefEditor.clear()
        prefEditor.apply()
    }

    private fun getLockerPreference(): String {
        return lockerPreference.getString(LOCKER_MENU_SELECTED, "").toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearLockerPreference()
    }

    override fun onBeamToTvClick() {
        enablingWiFiDisplay()
    }

    private fun enablingWiFiDisplay() {
        try {
            startActivity(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
            return
        } catch (activityNotFoundException: ActivityNotFoundException) {
            activityNotFoundException.printStackTrace()
        }
        try {
            startActivity(Intent("android.settings.CAST_SETTINGS"))
            return
        } catch (exception1: Exception) {
            Toast.makeText(applicationContext, "Device not supported", Toast.LENGTH_LONG).show()
        }
    }
}