package com.app.cascadeos.ui.fragment

import android.animation.LayoutTransition
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.app.cascadeos.R
import com.app.cascadeos.databinding.FragmentCoolEcallVideoCallBinding
import com.app.cascadeos.model.CoolEcallModel

class CoolEcallVideoCallFragment(private val appModel: CoolEcallModel) : Fragment() {
    private var callType: String? = null
    private lateinit var binding: FragmentCoolEcallVideoCallBinding
    private var isClickedFabMute: Boolean = false
    private var isClickedFabVideoOff: Boolean = false
    private var isClickedFabSharingScreen: Boolean = false

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            callType = it.getString(CALL_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_cool_ecall_video_call,
            container,
            false
        )
        // binding.clickListener = mainOnClickListener
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition

        startCamera()


        appModel.fullImage?.let { binding.appCompatImageView2.setImageResource(it) }
        binding.fabMute.setOnClickListener {
            if (!isClickedFabMute) {
                binding.fabMute.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                binding.fabMute.supportImageTintList = ColorStateList.valueOf(Color.BLACK)
                isClickedFabMute = true

            } else {
                binding.fabMute.backgroundTintList =
                    ColorStateList.valueOf(resources.getColor(R.color.white_20))
                binding.fabMute.supportImageTintList = ColorStateList.valueOf(Color.WHITE)
                isClickedFabMute = false
            }
        }
        binding.fabVideoOff.setOnClickListener {
            if (!isClickedFabVideoOff) {
                binding.fabVideoOff.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white)
                binding.fabVideoOff.supportImageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.black)
                cameraProvider.unbind(preview)
                binding.ivVideoOf.visibility = View.VISIBLE
                isClickedFabVideoOff = true

            } else {
                binding.fabVideoOff.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white_20)
                binding.fabVideoOff.supportImageTintList = ColorStateList.valueOf(Color.WHITE)
                startCamera()
                binding.ivVideoOf.visibility = View.GONE
                isClickedFabVideoOff = false
            }
        }
        binding.fabSharingScreen.setOnClickListener {
            if (!isClickedFabSharingScreen) {
                binding.fabSharingScreen.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white)
                binding.fabSharingScreen.supportImageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.black)
                isClickedFabSharingScreen = true
            } else {
                binding.fabSharingScreen.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white_20)
                binding.fabSharingScreen.supportImageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white)
                isClickedFabSharingScreen = false
            }
        }
        binding.fabMessage.setOnClickListener {
            startChatScreen()
        }

        binding.btnOff.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                callType.toString(),
                Bundle()
            )
            requireActivity().supportFragmentManager.popBackStack()

        }
    }


    private fun startCamera() {
        binding.viewFinder.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            //   val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider = cameraProviderFuture.get()
            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    companion object {
        @JvmStatic
        fun newInstance(callType: String) =
            VideoCallFragment().apply {
                arguments = Bundle().apply {
                    putString(CALL_TYPE, callType)
                }
            }
    }


    private fun startChatScreen() {
        val ft: FragmentTransaction =
            parentFragmentManager.beginTransaction()
        ft.replace(
            R.id.fragment_container,
            CoolEcallVideoMessageFragment()
        )
        ft.addToBackStack(CoolEcallVideoMessageFragment::class.java.simpleName)
        ft.commit()
    }


}