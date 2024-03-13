package com.app.cascadeos.ui.fragment

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.app.cascadeos.R
import com.app.cascadeos.databinding.FragmentCallBinding
import com.app.cascadeos.utility.ADD_CALL
import com.app.cascadeos.utility.CONFERENCE_CALL
import com.app.cascadeos.utility.END_CALL
import com.app.cascadeos.utility.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import kotlinx.coroutines.delay


const val PHONE_NUMBER = "phone_number"
const val CALL_TYPE = "call_type"

class CallFragment : Fragment() {
    private lateinit var binding: FragmentCallBinding
    private var phoneNumber: String? = null
    private var callType: String? = null
    private var isAddCall: Boolean = false
    private var pauseOffsetConference: Long = 0
    private var pauseOffsetUser: Long = 0
    private var pauseOffsetAdded: Long = 0
    private var pauseOffsetDefault: Long = 0
    private var running = false
    private var runningUser = false
    private var runningAdded = false
    private var runningConference = false
    private var isMergedConference = false
    private lateinit var sharedPref: SharedPreferences

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>


    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                videoCallStart()
            } else {
                requireContext().showToast(getString(R.string.permission_camera_rationale_message))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            phoneNumber = it.getString(PHONE_NUMBER)
            callType = it.getString(CALL_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_call, container, false)
        return binding.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPref = requireActivity().getSharedPreferences("sp_co", MODE_PRIVATE)
        binding.callFragment = this
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        parentFragmentManager.setFragmentResultListener(
            callType.toString(),
            this
        ) { requestKey, _ ->
            println("request key is $requestKey")
            callType = requestKey
            running = false
            runningAdded = false
            runningUser = false
            runningConference = false
        }

        when (callType) {
            END_CALL -> {
                binding.layoutMain.layoutTransition = layoutTransition
                binding.txtPhoneNumber.text = phoneNumber
                disableCallOptions()
            }
            ADD_CALL -> {
                binding.isSwapped = true
                binding.layoutHold.visibility = View.VISIBLE
                binding.layoutDialling.visibility = View.VISIBLE
                binding.layoutCall.visibility = View.GONE
                binding.layoutHold.alpha = 0.5f
                disableCallOptions()
            }
            CONFERENCE_CALL -> {
                binding.isSwapped = true
                binding.layoutConferenceHold.visibility = View.VISIBLE
                binding.layoutHold.visibility = View.GONE
                binding.layoutDialling.visibility = View.VISIBLE
                binding.layoutCall.visibility = View.GONE
                binding.layoutConferenceHold.alpha = 0.5f
                disableCallOptions()
            }

            else -> {
                disableCallOptions()
                mergeCall()
            }
        }

        addKeyPadBottomSheet()


        binding.btnOff.setOnClickListener {
            endCall()
        }

        binding.btnVideo.setOnClickListener {
            cameraPermission()
        }

        binding.btnMute.setOnClickListener {
            binding.isMute = !binding.isMute
        }

        binding.btnHold.setOnClickListener {
            if (callType == ADD_CALL || callType == CONFERENCE_CALL) {
                swapCall()
            } else {
                binding.isHold = !binding.isHold
            }
        }

        binding.btnSpeaker.setOnClickListener {
            binding.isSpeaker = !binding.isSpeaker
        }

        binding.btnAddCall.setOnClickListener {
            when (callType) {
                ADD_CALL -> {
                    if (isAddCall) {
                        callType = CONFERENCE_CALL
                        addCall()
                    } else {
                        mergeCall()
                    }
                }
                CONFERENCE_CALL -> {
                    if (isAddCall) {
                        callType = CONFERENCE_CALL
                        addCall()
                    } else {
                        isMergedConference = true
                        mergeCall()
                    }
                }
                else -> {
                    pauseDefaultChronometer()
                    addCall()
                }
            }
        }





        lifecycleScope.launchWhenStarted {
            delay(5000)
            binding.isCallStart = true
            println("delay call type is $callType")
            if (callType == ADD_CALL) {
                binding.isSwapped = false
                binding.btnAddCall.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_call_merge))
                binding.btnHold.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_swap_calls))
                enableCallOptions()
                startAddedUserChronometer()
            } else if (callType === CONFERENCE_CALL) {
                enableCallOptions()
                binding.isSwapped = false
                binding.btnAddCall.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_call_merge))
                binding.btnHold.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_swap_calls))
                startAddedUserChronometer()
            } else {
                enableCallOptions()
                binding.isCallStart = true
                startDefaultChronometer()
                if(callType == getString(R.string.merge_call)){
                    callType = CONFERENCE_CALL
                }
            }
        }
    }

    private fun disableCallOptions() {
        binding.btnAddCall.alpha = 0.5f
        binding.btnAddCall.isEnabled = false
        binding.btnHold.alpha = 0.5f
        binding.btnHold.isEnabled = false
        binding.btnVideo.alpha = 0.5f
        binding.btnVideo.isEnabled = false
        binding.btnMute.alpha = 0.5f
        binding.btnMute.isEnabled = false
    }

    private fun enableCallOptions() {
        binding.btnAddCall.alpha = 1.0f
        binding.btnAddCall.isEnabled = true
        binding.btnHold.alpha = 1.0f
        binding.btnHold.isEnabled = true
        binding.btnVideo.alpha = 1.0f
        binding.btnVideo.isEnabled = true
        binding.btnMute.alpha = 1.0f
        binding.btnMute.isEnabled = true
    }


    private fun swapCall() {
        binding.isSwapped = !binding.isSwapped
        when (callType) {
            ADD_CALL -> {
                if (binding.layoutHold.alpha == 0.5f) {
                    binding.layoutHold.alpha = 1.0f
                    binding.layoutDialling.alpha = 0.5f
                    binding.txtAddedCallStatus.text = getString(R.string.on_hold)
                    pauseAddedUserChronometer()
                    startUserChronometer()
                    binding.isUserOnHold = true
                } else {
                    binding.layoutHold.alpha = 0.5f
                    binding.layoutDialling.alpha = 1.0f
                    pauseUserChronometer()
                    startAddedUserChronometer()
                }
            }

            CONFERENCE_CALL -> {
                if (binding.layoutConferenceHold.alpha == 0.5f) {
                    binding.layoutConferenceHold.alpha = 1.0f
                    binding.layoutDialling.alpha = 0.5f
                    binding.txtAddedCallStatus.text = getString(R.string.on_hold)
                    binding.isUserOnHold = true
                    pauseAddedUserChronometer()
                    startConferenceChronometer()
                } else {
                    binding.layoutConferenceHold.alpha = 0.5f
                    binding.layoutDialling.alpha = 1.0f
                    binding.txtOnHold.text = getString(R.string.on_hold)
                    pauseConferenceChronometer()
                    startAddedUserChronometer()
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun mergeCall() {
        binding.layoutConferenceHold.visibility = View.GONE
        binding.layoutHold.visibility = View.GONE
        binding.layoutDialling.visibility = View.GONE
        binding.layoutCall.visibility = View.VISIBLE
        binding.txtPhoneNumber.text = ""
        binding.txtName.text = getString(R.string.conference_call)
        binding.imgProfile.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_round_profile))
        binding.btnAddCall.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_add_call))
        binding.btnHold.setImageDrawable(requireActivity().getDrawable(R.drawable.ic_call_hold))
        binding.isCallStart = callType != getString(R.string.merge_call)
        isAddCall = true
        startDefaultChronometer()
    }

    private fun endCall() {
        parentFragmentManager.setFragmentResult(
            END_CALL,
            Bundle()
        )
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun addCall() {
        if (callType == CONFERENCE_CALL) {
            pauseDefaultChronometer()
            getElapsedTime()
            parentFragmentManager.setFragmentResult(
                CONFERENCE_CALL,
                Bundle()
            )
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            parentFragmentManager.setFragmentResult(
                ADD_CALL,
                Bundle()
            )
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun videoCallStart() {
        if (isAddCall) {
            callType = getString(R.string.merge_call)
        }
        val ft: FragmentTransaction =
            requireActivity().supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, VideoCallFragment.newInstance(callType.toString()))
        ft.addToBackStack(null)
        ft.commit()
    }

    private fun cameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                videoCallStart()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun addKeyPadBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isDraggable = false
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    binding.isKeyPad = false
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    binding.isKeyPad = true
                }

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })

        binding.imgClose.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        }

        binding.btnKeyboard.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(phoneNumber: String, callType: String) =
            CallFragment().apply {
                arguments = Bundle().apply {
                    putString(PHONE_NUMBER, phoneNumber)
                    putString(CALL_TYPE, callType)
                }
            }
    }


    fun onClick(view: View) {
        when (view.id) {
            R.id.txt_one -> {
                addNumber(getString(R.string._1))
                playAudio(R.raw.dtmf_1)
            }

            R.id.txt_two -> {
                addNumber(getString(R.string._2))
                playAudio(R.raw.dtmf_2)
            }

            R.id.txt_three -> {
                addNumber(getString(R.string._3))
                playAudio(R.raw.dtmf_3)
            }

            R.id.txt_four -> {
                addNumber(getString(R.string._4))
                playAudio(R.raw.dtmf_4)
            }

            R.id.txt_five -> {
                addNumber(getString(R.string._5))
                playAudio(R.raw.dtmf_5)
            }

            R.id.txt_six -> {
                addNumber(getString(R.string._6))
                playAudio(R.raw.dtmf_6)
            }

            R.id.txt_seven -> {
                addNumber(getString(R.string._7))
                playAudio(R.raw.dtmf_7)
            }

            R.id.txt_eight -> {
                addNumber(getString(R.string._8))
                playAudio(R.raw.dtmf_8)
            }

            R.id.txt_nine -> {
                addNumber(getString(R.string._9))
                playAudio(R.raw.dtmf_9)
            }

            R.id.txt_zero -> {
                addNumber(getString(R.string._0))
                playAudio(R.raw.dtmf_0)
            }

            R.id.txt_star -> {
                addNumber(getString(R.string._star))
                playAudio(R.raw.dtmf_star)
            }

            R.id.txt_hash -> {
                addNumber(getString(R.string._hash))
                playAudio(R.raw.dtmf_hash)
            }

            else -> {

            }
        }
    }

    private fun addNumber(num: String) {
        val number = "${binding.txtNumber.text}${num}"
        binding.txtNumber.text = number
    }

    private fun playAudio(audioId: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(requireContext(), audioId)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }

    private fun startDefaultChronometer() {
        if (!running) {
            if (isAddCall) {
                if (isMergedConference) {
                    binding.chronometerTime.base = binding.chronometerTimeConference.base
                    binding.chronometerTime.start()
                    running = true
                } else {
                    binding.chronometerTime.base = binding.chronometerTimeUser.base
                    binding.chronometerTime.start()
                    running = true
                }
            } else {
                binding.chronometerTime.base = SystemClock.elapsedRealtime() - pauseOffsetDefault
                binding.chronometerTime.start()
                running = true
            }
        }
    }

    private fun pauseDefaultChronometer() {
        if (running) {
            binding.chronometerTime.stop()
            pauseOffsetDefault = SystemClock.elapsedRealtime() - binding.chronometerTime.base
            running = false
        }
    }

    private fun startUserChronometer() {
        if (!runningUser) {
            if (pauseOffsetUser == 0.toLong()) {
                binding.chronometerTimeUser.base = binding.chronometerTime.base
                binding.chronometerTimeUser.start()
                runningUser = true
            } else {
                binding.chronometerTimeUser.base = SystemClock.elapsedRealtime() - pauseOffsetUser
                binding.chronometerTimeUser.start()
                runningUser = true
            }
        }
    }

    private fun pauseUserChronometer() {
        if (runningUser) {
            binding.chronometerTime.stop()
            pauseOffsetUser = SystemClock.elapsedRealtime() - binding.chronometerTimeUser.base
            runningUser = false
        }
    }

    private fun startAddedUserChronometer() {
        if (!runningAdded) {
            binding.chronometerTimeAddedUser.base = SystemClock.elapsedRealtime() - pauseOffsetAdded
            binding.chronometerTimeAddedUser.start()
            runningAdded = true
        }
    }

    private fun pauseAddedUserChronometer() {
        if (runningAdded) {
            binding.chronometerTimeAddedUser.stop()
            pauseOffsetAdded =
                SystemClock.elapsedRealtime() - binding.chronometerTimeAddedUser.base
            runningAdded = false
        }
    }

    private fun startConferenceChronometer() {
        if (!runningConference) {
            if (pauseOffsetConference == 0.toLong()) {
                binding.chronometerTimeConference.base =
                    (SystemClock.elapsedRealtime() - sharedPref.getLong("Elapsed", 46885))
                binding.chronometerTimeConference.start()
                runningConference = true
            } else {
                binding.chronometerTimeConference.base =
                    SystemClock.elapsedRealtime() - pauseOffsetConference
                binding.chronometerTimeConference.start()
                runningConference = true
            }
        }
    }

    private fun pauseConferenceChronometer() {
        if (runningConference) {
            binding.chronometerTimeConference.stop()
            pauseOffsetConference =
                SystemClock.elapsedRealtime() - binding.chronometerTimeConference.base
            runningConference = false
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun getElapsedTime(): String {

        val elapsedMillis: Long = if (pauseOffsetDefault > 0) {
            pauseOffsetDefault
        } else {
            SystemClock.elapsedRealtime() - binding.chronometerTime.base
        }
        val hours = (elapsedMillis / 3600000).toInt()
        val minutes = (elapsedMillis - hours * 3600000).toInt() / 60000
        val seconds = (elapsedMillis - hours * 3600000 - minutes * 60000).toInt() / 1000
        val millis = (elapsedMillis - hours * 3600000 - minutes * 60000 - seconds * 1000).toInt()
        sharedPref.edit().putLong("Elapsed", elapsedMillis).apply()

        return "$hours:$minutes:$seconds:$millis"
    }
}