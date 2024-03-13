package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.ActivitySettingBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.ui.fragment.SettingScreenFragment


class Setting(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle, val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    val isResume: Boolean = false,
    configurationLiveData: MutableLiveData<Configuration?>,

    ) :
    WindowApp<ActivitySettingBinding>(context, lifecycle, R.layout.activity_setting, mBinding, configurationLiveData) {
    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList
    fun top() {
        binding.root.bringToFront()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()

        val settingFragment = SettingScreenFragment()

        val ft: FragmentTransaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_setting_container, settingFragment)
        ft.commit()

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition

    }

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
    }


}