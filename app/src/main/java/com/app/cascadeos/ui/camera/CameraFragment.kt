package com.app.cascadeos.ui.camera

import android.animation.ObjectAnimator
import android.content.ContentValues
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.view.View
import androidx.core.animation.doOnCancel
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.app.cascadeos.R
import com.app.cascadeos.adapter.FilterAdapter
import com.app.cascadeos.databinding.FragmentCameraBinding
import com.app.cascadeos.model.CameraTimer
import com.app.cascadeos.utility.SharedPrefsManager
import com.app.cascadeos.utility.saveImageExternal
import com.app.cascadeos.utility.showSnackBar
import com.app.cascadeos.utility.showToast
import com.app.cascadeos.utility.toggleButton
import com.app.cascadeos.viewmodel.CameraVM
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Grid
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.Preview
import com.otaliastudios.cameraview.filter.Filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates


class CameraFragment : BaseFragment<FragmentCameraBinding>(R.layout.fragment_camera) {
    var currentOrientation: Int? = null
    private lateinit var videoValue: ContentValues
    private var videoUri: Uri? = null
    private val prefs by lazy {
        SharedPrefsManager.newInstance(requireContext())
    }
    private val allFilters = Filters.values()
    private var currentFilter = 0

    val cameraVM: CameraVM by activityViewModels()


    private val filterAdapter by lazy { FilterAdapter() }
    private var flashMode by Delegates.observable(Flash.OFF) { _, _, new ->
        binding.btnFlash.setImageResource(
            when (new) {
                Flash.ON -> R.drawable.ic_flash_on
                Flash.AUTO -> R.drawable.ic_flash_auto
                else -> R.drawable.ic_flash_off
            }
        )
    }
    private var hasGrid = false
    private var hasHdr = false
    private var selectedTimer = CameraTimer.OFF

    private val animateRecord by lazy {
        ObjectAnimator.ofFloat(binding.btnTakePicture, View.ALPHA, 1f, 0.5f).apply {
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            doOnCancel { binding.btnTakePicture.alpha = 1f }
        }
    }


    override val binding: FragmentCameraBinding by lazy {
        FragmentCameraBinding.inflate(
            layoutInflater
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cameraView.setLifecycleOwner(this)

        CameraActivity.cameraBinding.layoutHiddenActionbar.visibility = View.VISIBLE
        flashMode = Flash.valueOf(prefs.getString(KEY_FLASH, Flash.OFF.name).toString())
        hasGrid = prefs.getBoolean(KEY_GRID, false)
        hasHdr = prefs.getBoolean(KEY_HDR, false)

        initViews()
        startCamera()


        binding.run {
            /*   btnTakePicture.setOnLongClickListener(OnLongClickListener {
                   btnTakePicture.setImageResource(R.drawable.ic_at_capture_camera)
                   return@OnLongClickListener false
               })*/
            btnTakePicture.setOnClickListener {
                btnTakePicture.setImageResource(R.drawable.ic_at_capture_camera)
                if (cameraView.mode == Mode.PICTURE) {
                    takePicture()
                } else {
                    if (cameraView.isTakingVideo) {
                        cameraView.stopVideo()
                    } else {
                        takeVideo()
                    }
                }

            }
            btnGallery.setOnClickListener { openPreview() }

            btnSwitchCamera.setOnClickListener {
                binding.cameraView.facing = if (binding.cameraView.facing == Facing.BACK) {
                    Facing.FRONT
                } else {
                    Facing.BACK
                }

            }
            btnTimer.setOnClickListener {
                groupTimer.visibility = if (groupTimer.isVisible) View.GONE else View.VISIBLE
                hideAllMenuOption()
            }
            btnGrid.setOnClickListener {
                toggleGrid()
            }
            btnFlash.setOnClickListener { /*selectFlash()*/
                groupFlash.visibility = if (groupFlash.isVisible) View.GONE else View.VISIBLE
                hideAllMenuOption()
            }
//            btnHdr.setOnClickListener { toggleHdr() }
            btnTimerOff.setOnClickListener {
                selectTimer(CameraTimer.OFF)
                showAllMenuOption()
                groupTimer.visibility = View.GONE
            }
            btnTimer3.setOnClickListener {
                selectTimer(CameraTimer.S3)
                showAllMenuOption()
            }
            btnTimer10.setOnClickListener {
                selectTimer(CameraTimer.S10)
                showAllMenuOption()
            }
            btnFlashOff.setOnClickListener {
                selectFlash(Flash.OFF)
                showAllMenuOption()
            }
            btnFlashOn.setOnClickListener {
                selectFlash(Flash.ON)
                showAllMenuOption()
            }
            btnFlashAuto.setOnClickListener {
                selectFlash(Flash.AUTO)
                showAllMenuOption()
            }
            /* tvVideo.setOnClickListener {
                 binding.cameraView.mode = Mode.VIDEO
             }
             tvPhoto.setOnClickListener {
                 binding.cameraView.mode = Mode.PICTURE
             }*/

            btnFilter.setOnClickListener {
                changeCurrentFilter()
            }

            btnMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    when (checkedId) {
                        R.id.btn_photo -> {
                            binding.cameraView.mode = Mode.PICTURE
                            binding.isVideo = false
                            cameraVM.isVideo = false
                        }

                        R.id.btn_video -> {
                            binding.cameraView.mode = Mode.VIDEO
                            binding.isVideo = true
                            cameraVM.isVideo = true
                        }
                    }
                }
            }
        }
    }

    private fun takeVideo() {
        /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              videoValue = ContentValues().apply {
                  put(MediaStore.Video.Media.DISPLAY_NAME, System.currentTimeMillis())
                  put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                  put(
                      MediaStore.Video.Media.RELATIVE_PATH,
                      "${Environment.DIRECTORY_DCIM}/${getString(R.string.app_name)}"
                  )
                  put(MediaStore.Video.Media.IS_PENDING, 1)
              }

              val resolver = context?.contentResolver
              val collection =
                  MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
              videoUri = resolver?.insert(collection, videoValue)
              videoUri?.let {
                *//*  val fileDescriptor = resolver?.openFileDescriptor(it, "w")?.fileDescriptor
                fileDescriptor?.let { it1 -> binding.cameraView.takeVideoSnapshot(it1) }*//*
                binding.cameraView.takeVideoSnapshot(it.toFile())

            }

        } else {*/
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            getString(R.string.app_name)
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${System.currentTimeMillis()}.mp4")
        binding.cameraView.takeVideoSnapshot(file)
        /* }*/
    }

    private fun initViews() {
        binding.btnGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        setGrid(hasGrid)
        selectFlash(flashMode)
    }

    private fun showAllMenuOption() {
        if (binding.cameraView.mode == Mode.PICTURE) {
            binding.btnTimer.visibility = View.VISIBLE
        }

        binding.btnGrid.visibility = View.VISIBLE
        binding.btnFlash.visibility = View.VISIBLE
//        binding.btnHdr.visibility = View.VISIBLE
        //       binding.btnExposure.visibility = View.VISIBLE
        binding.viewBg2.visibility = View.VISIBLE
    }

    private fun hideAllMenuOption() {
        binding.btnTimer.visibility = View.GONE
        binding.btnGrid.visibility = View.GONE
        binding.btnFlash.visibility = View.GONE
//        binding.btnHdr.visibility = View.GONE
        //       binding.btnExposure.visibility = View.GONE
        binding.viewBg2.visibility = View.GONE
    }

    private fun openPreview() {
        currentOrientation = resources.configuration.orientation
        if (getMedia().isEmpty()) return
        view?.let { Navigation.findNavController(it).navigate(R.id.action_camera_to_preview) }
    }

    /* override fun onResume() {
         super.onResume()
         if (currentOrientation != null) {
             if (currentOrientation != resources.configuration.orientation) {
                 updateLayoutAsConfigChange()
             }
         }
     }*/

    /**
     * Show timer selection menu by circular reveal animation.
     *  circularReveal() function is an Extension function which is adding the circular reveal
     * */
    /*private fun selectTimer() = binding.llTimerOptions.circularReveal(binding.btnTimer)

    */
    /**
     * This function is called from XML view via Data Binding to select a timer
     *  possible values are OFF, S3 or S10
     *  circularClose() function is an Extension function which is adding circular close
     * *//*
    private fun closeTimerAndSelect(timer: CameraTimer) =
        binding.llTimerOptions.circularClose(binding.btnTimer) {
            selectedTimer = timer
            binding.btnTimer.setImageResource(
                when (timer) {
                    CameraTimer.S3 -> R.drawable.ic_timer_3
                    CameraTimer.S10 -> R.drawable.ic_timer_10
                    CameraTimer.OFF -> R.drawable.ic_timer_off
                }
            )
        }*/
    private fun selectTimer(timer: CameraTimer) {
        selectedTimer = timer
        binding.btnTimer.setImageResource(
            when (timer) {
                CameraTimer.S3 -> R.drawable.ic_timer_3
                CameraTimer.S10 -> R.drawable.ic_timer_10
                CameraTimer.OFF -> R.drawable.ic_timer_off
            }
        )
        binding.groupTimer.visibility = View.GONE

    }

    private fun selectFlash(flash: Flash) {
        flashMode = flash
        binding.btnFlash.setImageResource(
            when (flash) {
                Flash.ON -> R.drawable.ic_flash_on
                Flash.OFF -> R.drawable.ic_flash_off
                else -> R.drawable.ic_flash_auto
            }
        )
        /*  imageCapture?.flashMode = flashMode*/
        binding.cameraView.flash = flashMode
        prefs.putString(KEY_FLASH, flashMode.name)
        binding.groupFlash.visibility = View.GONE

    }

    private fun toggleGrid() {
        binding.btnGrid.toggleButton(
            flag = hasGrid,
            rotationAngle = 180f,
            firstIcon = R.drawable.ic_grid_off,
            secondIcon = R.drawable.ic_grid_on,
        ) { flag ->
            hasGrid = flag
            prefs.putBoolean(KEY_GRID, flag)
            setGrid(flag)
        }
    }

    private fun setGrid(isGrid: Boolean) {
        val grid = if (isGrid) {
            Grid.DRAW_3X3
        } else {
            Grid.OFF
        }

        binding.cameraView.grid = grid
    }

    private fun toggleHdr() {
        /*binding.btnHdr.toggleButton(
            flag = hasHdr,
            rotationAngle = 360f,
            firstIcon = R.drawable.ic_hdr_off,
            secondIcon = R.drawable.ic_hdr_on,
        ) { flag ->
            hasHdr = flag
            prefs.putBoolean(KEY_HDR, flag)
            startCamera()
        }*/
    }

    override fun onPermissionGranted() {
        // Each time apps is coming to foreground the need permission check is being processed

        lifecycleScope.launch(Dispatchers.IO) {
            // Do on IO Dispatcher
            setLastPictureThumbnail()
        }
    }

    private fun setLastPictureThumbnail() = binding.btnGallery.post {
        getMedia().firstOrNull() // check if there are any photos or videos in the app directory
            ?.let { setGalleryThumbnail(it.uri, it.isVideo) } // preview the last one
            ?: binding.btnGallery.setImageResource(R.drawable.ic_no_picture) // or the default placeholder
    }

    private val cameraListener = object : CameraListener() {

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            val sound = MediaActionSound()
            sound.play(MediaActionSound.SHUTTER_CLICK)
            binding.btnTakePicture.setImageResource(R.drawable.ic_take_picture)
            result.toBitmap {
                val fileURI = it?.saveImageExternal(
                    "${System.currentTimeMillis()}.jpg",
                    requireContext(),
                    getString(R.string.app_name)
                )
                fileURI?.let { uri ->
                    setGalleryThumbnail(uri, false)
                }
            }
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            /* result.fileDescriptor
             videoValue.clear()
             videoValue.put(MediaStore.Video.Media.IS_PENDING, 0)*/
            val authority = requireContext().applicationContext.packageName + ".provider"
            setGalleryThumbnail(FileProvider.getUriForFile(requireContext(), authority, result.file), true)

        }

        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            val sound = MediaActionSound()
            sound.play(MediaActionSound.START_VIDEO_RECORDING)
            cameraVM.isVideoStarting = true
            binding.tvTimer.base = SystemClock.elapsedRealtime()
            cameraVM.baseTime = binding.tvTimer.base
            binding.tvTimer.start()
            binding.btnTakePicture.setImageResource(R.drawable.ic_at_capture_video)
            binding.btnSwitchCamera.visibility = View.INVISIBLE
            binding.btnGallery.visibility = View.INVISIBLE
            animateRecord.start()
        }

        override fun onVideoRecordingEnd() {
            super.onVideoRecordingEnd()
            val sound = MediaActionSound()
            sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            cameraVM.isVideoStarting = false
            binding.tvTimer.stop()
            binding.tvTimer.base = SystemClock.elapsedRealtime()
            cameraVM.baseTime = binding.tvTimer.base
            binding.btnTakePicture.setImageResource(R.drawable.ic_capture_video)
            binding.btnSwitchCamera.visibility = View.VISIBLE
            binding.btnGallery.visibility = View.VISIBLE
            animateRecord.cancel()
        }


        override fun onOrientationChanged(orientation: Int) {
            super.onOrientationChanged(orientation)
            binding.rotation = 360 - orientation
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            exception.printStackTrace()
            requireContext().showToast(exception.localizedMessage)
        }
    }


    private fun startCamera() {
        binding.cameraView.apply {
            removeCameraListener(cameraListener)
            addCameraListener(cameraListener)
        }
    }


    private fun takePicture() = lifecycleScope.launch(Dispatchers.Main) {
        // Show a timer based on user selection
        when (selectedTimer) {
            CameraTimer.S3 -> for (i in 3 downTo 1) {
                binding.tvCountDown.text = i.toString()
                delay(1000)
            }

            CameraTimer.S10 -> for (i in 10 downTo 1) {
                binding.tvCountDown.text = i.toString()
                delay(1000)
            }

            else -> {}
        }
        binding.tvCountDown.text = ""
        captureImage()
    }

    private fun captureImage() {
        binding.cameraView.takePictureSnapshot()

    }

    private fun setGalleryThumbnail(savedUri: Uri?, isVideo: Boolean) =
        binding.btnGallery.load(savedUri) {
            placeholder(R.drawable.ic_no_picture)
            transformations(CircleCropTransformation())
            listener(object : ImageRequest.Listener {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    super.onError(request, result)
                    if (isVideo) {
                        binding.btnGallery.load(savedUri) {
                            decoderFactory { result, options, _ ->
                                VideoFrameDecoder(
                                    result.source,
                                    options
                                )
                            }
                            transformations(CircleCropTransformation())
                        }
                    } else {
                        binding.btnGallery.load(savedUri) {
                            placeholder(R.drawable.ic_no_picture)
                            transformations(CircleCropTransformation())
                        }
                    }
                }
            })
        }


    override fun onBackPressed() = when {
        binding.groupTimer.visibility == View.VISIBLE -> binding.groupTimer.visibility = View.GONE

        binding.groupFlash.visibility == View.VISIBLE -> binding.groupFlash.visibility = View.GONE

        else -> requireActivity().finish()
    }

    private fun changeCurrentFilter() {
        if (binding.cameraView.preview != Preview.GL_SURFACE) return run {
            requireContext().showToast("Filters are supported only when preview is Preview.GL_SURFACE.")
        }
        if (currentFilter < allFilters.size - 1) {
            currentFilter++
        } else {
            currentFilter = 0
        }
        val filter = allFilters[currentFilter]
        binding.root.showSnackBar(filter.toString())

        // Normal behavior:
        binding.cameraView.filter = filter.newInstance()

        // To test MultiFilter:
        // DuotoneFilter duotone = new DuotoneFilter();
        // duotone.setFirstColor(Color.RED);
        // duotone.setSecondColor(Color.GREEN);
        // camera.setFilter(new MultiFilter(duotone, filter.newInstance()));
    }

    companion object {
        const val KEY_FLASH = "sPrefFlashCamera"
        const val KEY_GRID = "sPrefGridCamera"
        const val KEY_HDR = "sPrefHDR"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
        private const val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9
    }

}