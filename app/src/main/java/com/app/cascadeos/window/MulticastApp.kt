package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.content.ClipDescription
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.DragEvent
import android.view.View
import android.view.View.OnClickListener
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.cascadeos.R
import com.app.cascadeos.adapter.MulticastAppAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutMulticastBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.interfaces.SnapToGridListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.APP_LIST_PREFS
import com.app.cascadeos.utility.GET_MULTICAST_APP
import com.app.cascadeos.utility.ListConstants.getMulticastAppList
import com.app.cascadeos.utility.MULTI_CAST_APP_HEIGHT
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class MulticastApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    val onClick: OnClickListener,
    override val snapToGridListener: SnapToGridListener,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    private val appClickLiveData: MutableLiveData<View>,
    private var onMulticastAppClick: ((
        startX: Int,
        width: Int,
        height: Int, itemModel: AppModel,
    ) -> Unit),
) : WindowApp<LayoutMulticastBinding>(context, lifecycle, R.layout.layout_multicast, mBinding, null) {

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


    override fun start() {
        super.start()
        appListPreference = context.getSharedPreferences(APP_LIST_PREFS, Context.MODE_PRIVATE)

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition

        binding.apply {
            multicastAppAdapter =
                MulticastAppAdapter(context, appList = getAppListFromPreference(), onAppClick = { item, _ ->
                    val startingPoint =
                        if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(context.getString(R.string.multicast_system))?.startX
                            ?: 0 else 0
                    val appWidth =
                        if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(context.getString(R.string.multicast_system))?.width
                            ?: 0 else ConstraintLayout.LayoutParams.MATCH_PARENT
                    item.isMulticastApp = true
                    onMulticastAppClick(
                        startingPoint,
                        appWidth,
                        MULTI_CAST_APP_HEIGHT,
                        item
                    )
                }, onRemove = { appList ->
                    saveAppListFromPreference(appList)
                })
            rvMulticastApps.adapter = multicastAppAdapter
            rvMulticastApps.setOnDragListener(dragListener)
            clickListener = onClick

            configurationLiveData.observe((context as MainActivity), configObserver)

        }
    }

    override fun close() {
        super.close()
        if ((context as MainActivity).mainVM.isKeyboardOpen.value == true) {
            context.keyboardApp.close()
            context.mainVM.isKeyboardOpen.value = false
        }
        closeWindowListener.closeWindow(appDetailModel)
        configurationLiveData.removeObserver(configObserver)
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
                val childView: View? = binding.rvMulticastApps.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = binding.rvMulticastApps.getChildAdapterPosition(childView)
                    saveAppListFromPreference(multicastAppAdapter.addItem(itemModel, position))
                    if (position == 0) {
                        binding.rvMulticastApps.smoothScrollToPosition(position)
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
        val jsonText: String? = appListPreference.getString(GET_MULTICAST_APP, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            multicastAppList = it
        }
        if (multicastAppList.isEmpty()) {
            multicastAppList = getMulticastAppList(context)
            saveAppListFromPreference(multicastAppList)
        }
        return multicastAppList
    }

    private fun saveAppListFromPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_MULTICAST_APP, Gson().toJson(appList))
            apply()
        }
    }
}
