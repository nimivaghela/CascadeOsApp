package com.app.cascadeos.ui.fragment

import android.animation.LayoutTransition
import android.content.ClipDescription
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Patterns.PHONE
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.adapter.MulticastAppAdapter
import com.app.cascadeos.databinding.FragmentDialerBinding
import com.app.cascadeos.model.AppModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.*
import com.app.cascadeos.utility.ListConstants.getPhoneSystemAppList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*


class DialerFragment(
    private val appClickLiveData: MutableLiveData<View>,
    val onMulticastAppClick: (startX: Int, width: Int, height: Int, itemModel: AppModel) -> Unit,
    val screen: Screen,
) : Fragment() {
    private lateinit var binding: FragmentDialerBinding
    private lateinit var mainOnClickListener: OnClickListener
    private var callType: String = END_CALL
    private lateinit var multicastAppAdapter: MulticastAppAdapter
    private lateinit var appListPreference: SharedPreferences
    private var phoneSystemAppList = ArrayList<AppModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dialer, container, false)
        binding.dialerFragment = this

        appListPreference = requireContext().getSharedPreferences(APP_LIST_PREFS, Context.MODE_PRIVATE)

        multicastAppAdapter =
            MulticastAppAdapter(
                requireContext(),
                appList = getPhoneSystemAppsFromPreference(),
                onAppClick = { itemModel, _ ->
                    //appClickLiveData.value = view
                    itemModel.isPhoneSystemApp = true
                    val startingPoint =
                        if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(getString(R.string.phone_system))?.startX
                            ?: 0 else 0
                    val appWidth =
                        if (screen == Screen.DEFAULT) (context as MainActivity).mainVM.getOpenApp(getString(R.string.phone_system))?.width
                            ?: 0 else ConstraintLayout.LayoutParams.MATCH_PARENT

                    onMulticastAppClick(
                        startingPoint,
                        appWidth,
                        PHONE_SYSTEM_APP_HEIGHT,
                        itemModel
                    )
                },
                onRemove = { appList ->
                    saveAppListFromPreference(appList)
                })
        binding.rvPhoneSystem.adapter = multicastAppAdapter
        binding.rvPhoneSystem.setOnDragListener(dragListener)

        return binding.root
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
                val childView: View? = binding.rvPhoneSystem.findChildViewUnder(x, y)
                if (childView != null) {
                    val position: Int = binding.rvPhoneSystem.getChildAdapterPosition(childView)
                    saveAppListFromPreference(multicastAppAdapter.addItem(itemModel, position))
                    if (position == 0) {
                        binding.rvPhoneSystem.smoothScrollToPosition(position)
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

    private fun getPhoneSystemAppsFromPreference(): ArrayList<AppModel> {
        val gson = Gson()
        val jsonText: String? = appListPreference.getString(GET_PHONE_SYSTEM_APP, null)
        val type: Type = object : TypeToken<ArrayList<AppModel?>?>() {}.type
        (gson.fromJson<Any>(jsonText, type) as? ArrayList<AppModel>)?.let {
            phoneSystemAppList = it
        }
        if (phoneSystemAppList.isEmpty()) {
            phoneSystemAppList = getPhoneSystemAppList(requireContext())
            saveAppListFromPreference(phoneSystemAppList)
        }
        return phoneSystemAppList
    }

    private fun saveAppListFromPreference(appList: ArrayList<AppModel>) {
        appListPreference.edit().apply {
            putString(GET_PHONE_SYSTEM_APP, Gson().toJson(appList))
            apply()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition

        parentFragmentManager.setFragmentResultListener(
            END_CALL,
            this
        ) { requestKey, _ ->
            if (requestKey == END_CALL) {
                binding.editDialer.text?.clear()
                callType = END_CALL
            }
        }

        parentFragmentManager.setFragmentResultListener(
            ADD_CALL,
            this
        ) { requestKey, _ ->
            if (requestKey == ADD_CALL) {
                binding.editDialer.text?.clear()
                callType = ADD_CALL
            }
        }

        parentFragmentManager.setFragmentResultListener(
            CONFERENCE_CALL,
            this
        ) { requestKey, _ ->
            if (requestKey == CONFERENCE_CALL) {
                binding.editDialer.text?.clear()
                callType = CONFERENCE_CALL
            }
        }

    }

    fun resumeCalling() {
        val ft: FragmentTransaction =
            parentFragmentManager.beginTransaction()
        ft.replace(
            R.id.fragment_container,
            CallFragment.newInstance("9876532111", callType)
        )
        ft.addToBackStack(null)
        ft.commit()
    }


    fun onClick(view: View) {
        when (view.id) {
            R.id.txt_one -> {
                addNumber(getString(R.string._1))
                playTone(R.raw.dtmf_1)
            }

            R.id.txt_two -> {
                addNumber(getString(R.string._2))
                playTone(R.raw.dtmf_2)


            }

            R.id.txt_three -> {
                addNumber(getString(R.string._3))
                playTone(R.raw.dtmf_3)
            }

            R.id.txt_four -> {
                addNumber(getString(R.string._4))
                playTone(R.raw.dtmf_4)

            }

            R.id.txt_five -> {
                addNumber(getString(R.string._5))
                playTone(R.raw.dtmf_5)

            }

            R.id.txt_six -> {
                addNumber(getString(R.string._6))
                playTone(R.raw.dtmf_6)

            }

            R.id.txt_seven -> {
                addNumber(getString(R.string._7))
                playTone(R.raw.dtmf_7)
            }

            R.id.txt_eight -> {
                addNumber(getString(R.string._8))
                playTone(R.raw.dtmf_8)

            }

            R.id.txt_nine -> {
                addNumber(getString(R.string._9))
                playTone(R.raw.dtmf_9)

            }

            R.id.txt_zero -> {
                addNumber(getString(R.string._0))
                playTone(R.raw.dtmf_0)

            }

            R.id.txt_star -> {
                addNumber(getString(R.string._star))
                playTone(R.raw.dtmf_star)

            }

            R.id.txt_hash -> {
                addNumber(getString(R.string._hash))
                playTone(R.raw.dtmf_hash)

            }

            R.id.layout_add -> {
                addNumber(getString(R.string._plus))
                playTone(R.raw.dtmf_1)
            }

            R.id.layout_clear -> {
                binding.editDialer.setText(binding.editDialer.text.toString().dropLast(1))
            }


            R.id.layout_call -> {
                val phoneNumber = binding.editDialer.text.toString()
                if (PHONE.matcher(phoneNumber).matches()) {
                    val ft: FragmentTransaction =
                        parentFragmentManager.beginTransaction()
                    ft.replace(
                        R.id.fragment_container,
                        CallFragment.newInstance(phoneNumber, callType)
                    )
                    ft.addToBackStack(null)
                    ft.commit()
                } else {
                    requireContext().showToast(getString(R.string.please_enter_valid_phone_number))
                }
            }

            else -> {

            }
        }
    }


    private fun addNumber(num: String) {
        val phoneNumber: String =
            binding.editDialer.text?.append(num).toString()
        val formatNumber = PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().country)
        val size: Int = if (formatNumber != null) {
            binding.editDialer.setText(formatNumber)
            formatNumber.length

        } else {
            binding.editDialer.setText(phoneNumber)
            phoneNumber.length
        }
        binding.editDialer.setSelection(size)//placing cursor at the end of the text


    }


    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(requireContext(), tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }
}