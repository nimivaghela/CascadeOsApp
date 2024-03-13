package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.cascadeos.R
import com.app.cascadeos.adapter.SettingAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.ActivitySettingAppBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.SettingModel
import com.app.cascadeos.utility.*


@UnstableApi
class SettingScreen(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle, private val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    var isResume: Boolean,
    private val configurationLiveData: MutableLiveData<Configuration?>
) : WindowApp<ActivitySettingAppBinding>(context, lifecycle, R.layout.activity_setting_app, mBinding,configurationLiveData) {


    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList



    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.clRootSetting.layoutTransition = layoutTransition
        setUpRecycleview()
        }
    private fun setUpRecycleview() {

        val onSetting: MutableList<SettingModel> = mutableListOf<SettingModel>()
        onSetting.add(SettingModel(1, R.drawable.ic_keyboard, "Keyboard", "On screen keyboard"))
        onSetting.add(SettingModel(2, R.drawable.ic_wifi, "Network & Internet", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(3, R.drawable.ic_connected_device, "Connected devices", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(4, R.drawable.ic_apps, "Apps", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(5, R.drawable.ic_notification, "Notifications", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(6, R.drawable.ic_battery, "Battery", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(7, R.drawable.ic_storage, "Storage", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(8, R.drawable.ic_sound_vibration, "Sound & vibration", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(9, R.drawable.ic_display, "Display", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(10, R.drawable.ic_wallpaper, "Wallpaper", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(11, R.drawable.ic_accessibility, "Accessibility", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(12, R.drawable.ic_security, "Security", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(13, R.drawable.ic_privacy, "Privacy", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(14, R.drawable.ic_location, "Location", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(15, R.drawable.ic_safety_emergency, "Safety & emergency", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(16, R.drawable.ic_password_account, "Passwords & accounts", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(17, R.drawable.ic_digital_wellbeing, "Digital Wellbeing and paren...", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(18, R.drawable.ic_google, "Google", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(19, R.drawable.ic_system_update, "System Update", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(20, R.drawable.ic_rating_feedback, "Rating & Feedback", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(21, R.drawable.ic_help, "Help", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(22, R.drawable.ic_system, "System", "Mobile wi-fi hotspot"))
        onSetting.add(SettingModel(23, R.drawable.ic_about_phone, "About Phone", "Mobile wi-fi hotspot"))
        binding.rvSettingList.adapter = SettingAdapter(context,object : SettingAdapter.SettingItemClicked{
            override fun onSettingItemClicked(position: Int, settingModel: SettingModel) {
                when(settingModel.id){
                    1->{
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    }
                    11 -> {
                        Toast.makeText(context,"$position",Toast.LENGTH_SHORT).show()
                    }
                }
            }

        },onSetting)
        binding.rvSettingList.layoutManager =  LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

        }

