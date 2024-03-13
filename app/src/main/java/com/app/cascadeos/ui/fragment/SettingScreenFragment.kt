package com.app.cascadeos.ui.fragment

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.cascadeos.R
import com.app.cascadeos.adapter.SettingAdapter
import com.app.cascadeos.databinding.ActivitySettingAppBinding
import com.app.cascadeos.model.SettingModel

class SettingScreenFragment() : Fragment() {
    private lateinit var binding: ActivitySettingAppBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_setting_app, container, false)
        // binding.clickListener = mainOnClickListener
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.clRootSetting.layoutTransition = layoutTransition
        setUpRecyclerView()

    }

    private fun setUpRecyclerView() {

        val onSetting: MutableList<SettingModel> = mutableListOf()

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
        binding.rvSettingList.adapter =
            SettingAdapter(requireContext(), object : SettingAdapter.SettingItemClicked {
                override fun onSettingItemClicked(position: Int, settingModel: SettingModel) {
                    when (settingModel.id) {
                        1->{
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            requireActivity().startActivity(intent)
                        }
                        12 -> {
                            val ft: FragmentTransaction =
                                parentFragmentManager.beginTransaction()
                            ft.replace(
                                R.id.fragment_setting_container,
                                SecurityScreenFragment()
                            )
                            ft.addToBackStack(null)
                            ft.commit()
                        }
                    }
                }

            }, onSetting)
        binding.rvSettingList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

}
