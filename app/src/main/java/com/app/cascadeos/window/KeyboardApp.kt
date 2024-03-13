package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.content.ClipDescription
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.MediaPlayer
import android.view.DragEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.cascadeos.R
import com.app.cascadeos.adapter.MulticastAppAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutKeyboardLandscapeBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.APP_LIST_PREFS
import com.app.cascadeos.utility.GET_KEYBOARD_SCREEN_APP
import com.app.cascadeos.utility.ListConstants
import com.app.cascadeos.utility.MULTI_CAST_APP_HEIGHT
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class KeyboardApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    private var onMulticastAppClick: ((
        startX: Int,
        width: Int,
        height: Int, itemModel: AppModel,
    ) -> Unit),
    private val keyTextLiveData: MutableLiveData<String>?,
) : WindowApp<LayoutKeyboardLandscapeBinding>(
    context, lifecycle, R.layout.layout_keyboard_landscape, mBinding, configurationLiveData
) {
    private lateinit var multicastAppAdapter: MulticastAppAdapter
    private lateinit var appListPreference: SharedPreferences
    private var keyboardAppList = ArrayList<AppModel>()

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

    override fun start() {
        super.start()
        appListPreference = context.getSharedPreferences(APP_LIST_PREFS, Context.MODE_PRIVATE)

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition
        binding.keyboardApp = this
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_full_screen).visibility =
            View.GONE

        binding.apply {
            multicastAppAdapter =
                MulticastAppAdapter(
                    context,
                    appList = getAppListFromPreference(),
                    onAppClick = { item, _ ->
                        val startingPoint =
                            if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(
                                context.getString(R.string.multicast_system)
                            )?.startX
                                ?: 0 else 0
                        val appWidth =
                            if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(
                                context.getString(R.string.multicast_system)
                            )?.width
                                ?: 0 else ConstraintLayout.LayoutParams.MATCH_PARENT
                        val appHeight = 0
                        item.isMulticastApp = true
                        onMulticastAppClick(
                            startingPoint, appWidth, MULTI_CAST_APP_HEIGHT, item
                        )
                    },
                    onRemove = { appList ->
                        saveAppListFromPreference(appList)
                    })
            rvAppList.adapter = multicastAppAdapter
            rvAppList.setOnDragListener(dragListener)

            configurationLiveData.observe((context as MainActivity), configObserver)

        }

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
                val childView: View? = binding.rvAppList.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = binding.rvAppList.getChildAdapterPosition(childView)
                    saveAppListFromPreference(multicastAppAdapter.addItem(itemModel, position))
                    if (position == 0) {
                        binding.rvAppList.smoothScrollToPosition(position)
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

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
        configurationLiveData.removeObserver(configObserver)
    }

    fun top() {
        binding.root.bringToFront()
    }

    private fun getAppListFromPreference(): ArrayList<AppModel> {
        val gson = Gson()
        val jsonText: String? = appListPreference.getString(GET_KEYBOARD_SCREEN_APP, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            keyboardAppList = it
        }
        if (keyboardAppList.isEmpty()) {
            keyboardAppList = ListConstants.getKeyboardAppsList(context)
            saveAppListFromPreference(keyboardAppList)
        }
        return keyboardAppList
    }

    private fun saveAppListFromPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_KEYBOARD_SCREEN_APP, Gson().toJson(appList))
            apply()
        }
    }

    fun onClick(view: View) {
        when (view.id) {
            R.id.txt_one -> {
                addKeyValue(context.getString(R.string._1))
                playTone(R.raw.dtmf_1)
            }

            R.id.txt_two -> {
                addKeyValue(context.getString(R.string._2))
                playTone(R.raw.dtmf_2)
            }

            R.id.txt_three -> {
                addKeyValue(context.getString(R.string._3))
                playTone(R.raw.dtmf_3)
            }

            R.id.txt_four -> {
                addKeyValue(context.getString(R.string._4))
                playTone(R.raw.dtmf_4)
            }

            R.id.txt_five -> {
                addKeyValue(context.getString(R.string._5))
                playTone(R.raw.dtmf_5)
            }

            R.id.txt_six -> {
                addKeyValue(context.getString(R.string._6))
                playTone(R.raw.dtmf_6)
            }

            R.id.txt_seven -> {
                addKeyValue(context.getString(R.string._7))
                playTone(R.raw.dtmf_7)
            }

            R.id.txt_eight -> {
                addKeyValue(context.getString(R.string._8))
                playTone(R.raw.dtmf_8)
            }

            R.id.txt_nine -> {
                addKeyValue(context.getString(R.string._9))
                playTone(R.raw.dtmf_9)
            }

            R.id.txt_zero -> {
                addKeyValue(context.getString(R.string._0))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_q -> {
                addKeyValue(binding.txtQ.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_w -> {
                addKeyValue(binding.txtW.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_e -> {
                addKeyValue(binding.txtE.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_r -> {
                addKeyValue(binding.txtR.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_t -> {
                addKeyValue(binding.txtT.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_y -> {
                addKeyValue(binding.txtY.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_u -> {
                addKeyValue(binding.txtU.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_i -> {
                addKeyValue(binding.txtI.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_o -> {
                addKeyValue(binding.txtO.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_p -> {
                addKeyValue(binding.txtP.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_dash -> {
                addKeyValue(context.getString(R.string._dash))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_semi_colon -> {
                addKeyValue(context.getString(R.string._semi_colon))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_colon -> {
                addKeyValue(context.getString(R.string._colon))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_a -> {
                addKeyValue(binding.txtA.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_s -> {
                addKeyValue(binding.txtS.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_d -> {
                addKeyValue(binding.txtD.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_f -> {
                addKeyValue(binding.txtF.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_g -> {
                addKeyValue(binding.txtG.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_h -> {
                addKeyValue(binding.txtH.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_j -> {
                addKeyValue(binding.txtJ.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_k -> {
                addKeyValue(binding.txtK.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_l -> {
                addKeyValue(binding.txtL.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_open_bracket -> {
                addKeyValue(context.getString(R.string._open_round_bracket))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_close_bracket -> {
                addKeyValue(context.getString(R.string._close_round_bracket))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_dollar -> {
                addKeyValue(context.getString(R.string._dollar))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_z -> {
                addKeyValue(binding.txtZ.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_x -> {
                addKeyValue(binding.txtX.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_c -> {
                addKeyValue(binding.txtC.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_v -> {
                addKeyValue(binding.txtV.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_b -> {
                addKeyValue(binding.txtB.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_n -> {
                addKeyValue(binding.txtN.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_m -> {
                addKeyValue(binding.txtM.text.toString())
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_and -> {
                addKeyValue(context.getString(R.string._and))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_at -> {
                addKeyValue(context.getString(R.string._at))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_quotes -> {
                addKeyValue(context.getString(R.string._quotes))
                playTone(R.raw.dtmf_0)
            }

            R.id.layout_add -> {
                addKeyValue(context.getString(R.string._plus))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_plus -> {
                addKeyValue(context.getString(R.string._plus))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_coma -> {
                addKeyValue(context.getString(R.string._coma))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_dot -> {
                addKeyValue(context.getString(R.string._dot))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_question_mark -> {
                addKeyValue(context.getString(R.string._question_mark))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_exclamation_mark -> {
                addKeyValue(context.getString(R.string._exclamation_mark))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_single_quot -> {
                addKeyValue(context.getString(R.string._single_quot))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_equal_to -> {
                addKeyValue(context.getString(R.string._equal_to))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_return -> {
                addKeyValue("\n")
                playTone(R.raw.dtmf_0)
            }

            R.id.ib_close -> {
                addKeyValue(context.getString(R.string.clear))
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_space -> {
                addKeyValue(" ")
                playTone(R.raw.dtmf_0)
            }

            R.id.txt_caps -> {
                binding.isCaps = !binding.isCaps
                changeText(binding.isCaps)
                playTone(R.raw.dtmf_0)
            }

            else -> {

            }
        }
    }

    private fun changeText(isCaps: Boolean) {
        if (isCaps) {
            binding.txtQ.text = binding.txtQ.text.toString().toLowerCase()
            binding.txtW.text = binding.txtW.text.toString().toLowerCase()
            binding.txtE.text = binding.txtE.text.toString().toLowerCase()
            binding.txtR.text = binding.txtR.text.toString().toLowerCase()
            binding.txtT.text = binding.txtT.text.toString().toLowerCase()
            binding.txtY.text = binding.txtY.text.toString().toLowerCase()
            binding.txtU.text = binding.txtU.text.toString().toLowerCase()
            binding.txtI.text = binding.txtI.text.toString().toLowerCase()
            binding.txtO.text = binding.txtO.text.toString().toLowerCase()
            binding.txtP.text = binding.txtP.text.toString().toLowerCase()

            binding.txtA.text = binding.txtA.text.toString().toLowerCase()
            binding.txtS.text = binding.txtS.text.toString().toLowerCase()
            binding.txtD.text = binding.txtD.text.toString().toLowerCase()
            binding.txtF.text = binding.txtF.text.toString().toLowerCase()
            binding.txtG.text = binding.txtG.text.toString().toLowerCase()
            binding.txtH.text = binding.txtH.text.toString().toLowerCase()
            binding.txtJ.text = binding.txtJ.text.toString().toLowerCase()
            binding.txtK.text = binding.txtK.text.toString().toLowerCase()
            binding.txtL.text = binding.txtL.text.toString().toLowerCase()

            binding.txtZ.text = binding.txtZ.text.toString().toLowerCase()
            binding.txtX.text = binding.txtX.text.toString().toLowerCase()
            binding.txtC.text = binding.txtC.text.toString().toLowerCase()
            binding.txtV.text = binding.txtV.text.toString().toLowerCase()
            binding.txtB.text = binding.txtB.text.toString().toLowerCase()
            binding.txtN.text = binding.txtN.text.toString().toLowerCase()
            binding.txtM.text = binding.txtM.text.toString().toLowerCase()
        } else {
            binding.txtQ.text = binding.txtQ.text.toString().toUpperCase()
            binding.txtW.text = binding.txtW.text.toString().toUpperCase()
            binding.txtE.text = binding.txtE.text.toString().toUpperCase()
            binding.txtR.text = binding.txtR.text.toString().toUpperCase()
            binding.txtT.text = binding.txtT.text.toString().toUpperCase()
            binding.txtY.text = binding.txtY.text.toString().toUpperCase()
            binding.txtU.text = binding.txtU.text.toString().toUpperCase()
            binding.txtI.text = binding.txtI.text.toString().toUpperCase()
            binding.txtO.text = binding.txtO.text.toString().toUpperCase()
            binding.txtP.text = binding.txtP.text.toString().toUpperCase()

            binding.txtA.text = binding.txtA.text.toString().toUpperCase()
            binding.txtS.text = binding.txtS.text.toString().toUpperCase()
            binding.txtD.text = binding.txtD.text.toString().toUpperCase()
            binding.txtF.text = binding.txtF.text.toString().toUpperCase()
            binding.txtG.text = binding.txtG.text.toString().toUpperCase()
            binding.txtH.text = binding.txtH.text.toString().toUpperCase()
            binding.txtJ.text = binding.txtJ.text.toString().toUpperCase()
            binding.txtK.text = binding.txtK.text.toString().toUpperCase()
            binding.txtL.text = binding.txtL.text.toString().toUpperCase()

            binding.txtZ.text = binding.txtZ.text.toString().toUpperCase()
            binding.txtX.text = binding.txtX.text.toString().toUpperCase()
            binding.txtC.text = binding.txtC.text.toString().toUpperCase()
            binding.txtV.text = binding.txtV.text.toString().toUpperCase()
            binding.txtB.text = binding.txtB.text.toString().toUpperCase()
            binding.txtN.text = binding.txtN.text.toString().toUpperCase()
            binding.txtM.text = binding.txtM.text.toString().toUpperCase()
        }
    }

    private fun addKeyValue(key: String) {
        keyTextLiveData?.postValue(key)
    }

    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(context, tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }
}