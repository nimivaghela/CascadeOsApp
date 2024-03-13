package com.app.cascadeos.ui.camera

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.GestureDetector
import android.view.View
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnCancel
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.app.cascadeos.R
import com.app.cascadeos.databinding.FragmentVideoBinding
import com.app.cascadeos.utility.SharedPrefsManager
import com.app.cascadeos.utility.SwipeGestureDetector
import com.app.cascadeos.utility.bottomMargin
import com.app.cascadeos.utility.endMargin
import com.app.cascadeos.utility.fitSystemWindows
import com.app.cascadeos.utility.mainExecutor
import com.app.cascadeos.utility.onWindowInsets
import com.app.cascadeos.utility.toggleButton
import com.app.cascadeos.viewmodel.LiveDataTimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates


@SuppressLint("RestrictedApi")
class VideoFragment : BaseFragment<FragmentVideoBinding>(R.layout.fragment_video) {
    // An instance for display manager to get display change callbacks
    private val displayManager by lazy { requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager }
    private lateinit var cameraInfo: CameraInfo
    lateinit var cameraControl: CameraControl

    var currentOrientation: Int? = null

    // An instance of a helper function to work with Shared Preferences
    private val prefs by lazy { SharedPrefsManager.newInstance(requireContext()) }
    var mMediaPlayer: MediaPlayer? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: androidx.camera.core.VideoCapture? = null

    private var displayId = -1

    // Selector showing which flash mode is selected (on, off or auto)
    private var flashMode by Delegates.observable(ImageCapture.FLASH_MODE_OFF) { _, _, new ->
        binding.btnFlashVideo.setImageResource(
            when (new) {
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto
                else -> R.drawable.ic_flash_off
            }
        )
    }

    // Selector showing is grid enabled or not
    private var hasGrid = false

    // Selector showing is flash enabled or not
    private var isTorchOn = false
    val liveDataTimerViewModel: LiveDataTimerViewModel by viewModels()

    // Selector showing is recording currently active
    private var isRecording = false
    private val animateRecord by lazy {
        ObjectAnimator.ofFloat(binding.btnRecordVideo, View.ALPHA, 1f, 0.5f).apply {
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            doOnCancel { binding.btnRecordVideo.alpha = 1f }
        }
    }
    var cnt = 0L
    var time = 0L
    lateinit var countDownTimer: CountDownTimer

    // A lazy instance of the current fragment's view binding
    override val binding: FragmentVideoBinding by lazy { FragmentVideoBinding.inflate(layoutInflater) }

    /**
     * A display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@VideoFragment.displayId) {
                preview?.targetRotation = view.display.rotation
                videoCapture?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasGrid = prefs.getBoolean(KEY_GRID, false)
        CameraActivity.cameraBinding.layoutHiddenActionbar.visibility = View.VISIBLE
        initViews()

        displayManager.registerDisplayListener(displayListener, null)

        binding.run {
            viewFinder.addOnAttachStateChangeListener(object :
                View.OnAttachStateChangeListener {
                override fun onViewDetachedFromWindow(v: View) =
                    displayManager.registerDisplayListener(displayListener, null)

                override fun onViewAttachedToWindow(v: View) =
                    displayManager.unregisterDisplayListener(displayListener)
            })
            binding.btnRecordVideo.setOnClickListener {
                binding.btnRecordVideo.setImageResource(R.drawable.ic_at_capture_video)
                if (mMediaPlayer == null) {
                    mMediaPlayer = MediaPlayer.create(requireContext(), R.raw.video_capture)
                    mMediaPlayer!!.isLooping = false
                    mMediaPlayer!!.start()
                } else mMediaPlayer!!.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    recordVideo()
                }, 2000)
            }
            btnGallery.setOnClickListener { openPreview() }
            btnSwitchCamera.setOnClickListener {
                isRecording = !isRecording
                toggleCamera()
            }
            btnGrid.setOnClickListener { toggleGrid() }
            btnFlashVideo.setOnClickListener { toggleFlash() }

            // This swipe gesture adds a fun gesture to switch between video and photo
            val swipeGestures = SwipeGestureDetector().apply {
                setSwipeCallback(left = {
                    Navigation.findNavController(view).navigate(R.id.action_video_to_camera)
                })
            }

            val gestureDetectorCompat = GestureDetector(requireContext(), swipeGestures)
            viewFinder.setOnTouchListener { _, motionEvent ->
                if (gestureDetectorCompat.onTouchEvent(motionEvent)) return@setOnTouchListener false
                return@setOnTouchListener true
            }

            tvPhoto.setOnClickListener {
//                tvPhoto.text = getString(R.string.video)
                Navigation.findNavController(view).navigate(R.id.action_video_to_camera)
            }
        }
    }

    /**
     * Create some initial states
     * */
    private fun initViews() {
        binding.btnGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        binding.groupGridLines.visibility = if (hasGrid) View.VISIBLE else View.GONE

        adjustInsets()

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_video)
        constraintSet.applyTo(binding.layoutMain)
        if (hasGrid) {
            binding.groupGridLines.visibility = View.VISIBLE
        } else {
            binding.groupGridLines.visibility = View.GONE
        }
        if (isRecording) {
            binding.btnSwitchCamera.visibility = View.GONE
            binding.btnGallery.visibility = View.INVISIBLE
        } else {
            binding.btnSwitchCamera.visibility = View.VISIBLE
            binding.btnGallery.visibility = View.VISIBLE
            binding.tvTimer.visibility = View.GONE
        }

    }

    override fun onResume() {
        super.onResume()
        if (currentOrientation != null) {
            if (currentOrientation != resources.configuration.orientation) {
                updateLayoutAsConfigChange()
            }
        }
    }

    private fun updateLayoutAsConfigChange() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_video)
        constraintSet.applyTo(binding.layoutMain)
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {
        activity?.window?.fitSystemWindows()
        binding.btnRecordVideo.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                view.bottomMargin =
                    windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            } else {
                view.endMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right
            }
        }
//        binding.btnFlashVideo.onWindowInsets { view, windowInsets ->
//            view.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//        }
    }

    /**
     * Change the facing of camera
     *  toggleButton() function is an Extension function made to animate button rotation
     * */
    private fun toggleCamera() {
        CameraActivity.lensFacing =
            if (CameraActivity.lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
        startCamera()
    }

    /**
     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
     * */
    private fun startCamera() {
        val viewFinder = binding.viewFinder

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            val aspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
            val rotation = viewFinder.display.rotation

            val localCameraProvider = cameraProvider
                ?: throw IllegalStateException("Camera initialization failed.")

            preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio) // set the camera aspect ratio
                .setTargetRotation(rotation) // set the camera rotation
                .build()

            val videoCaptureConfig =
                VideoCapture.DEFAULT_CONFIG.config

            videoCapture = VideoCapture.Builder
                .fromConfig(videoCaptureConfig)
                .build()

            localCameraProvider.unbindAll()

            try {
                camera = localCameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraActivity.lensFacing,
                    preview,
                    videoCapture
                )
                preview?.setSurfaceProvider(viewFinder.surfaceProvider)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private fun openPreview() {
        currentOrientation = resources.configuration.orientation
        view?.let { Navigation.findNavController(it).navigate(R.id.action_video_to_preview) }
    }

    @SuppressLint("MissingPermission")
    private fun recordVideo() {
        val localVideoCapture =
            videoCapture ?: throw IllegalStateException("Camera initialization failed.")

        // Options fot the output video file
        val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis())
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, outputDirectory)
            }

            requireContext().contentResolver.run {
                val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                VideoCapture.OutputFileOptions.Builder(this, contentUri, contentValues)
            }
        } else {
            File(outputDirectory).mkdirs()
            val file = File("$outputDirectory/${System.currentTimeMillis()}.mp4")

            VideoCapture.OutputFileOptions.Builder(file)
        }.build()

        if (!isRecording) {
            binding.btnSwitchCamera.visibility = View.GONE
            binding.btnGallery.visibility = View.INVISIBLE
            animateRecord.start()
            countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    cnt++
                    time++
                    if (cnt == 60L) {
                        cnt = 0L
                    }
                    val millis: Long = time
                    var seconds = (millis / 60).toInt()
                    val minutes = seconds / 60
                    seconds %= 60

                    binding.tvTimer.visibility = View.VISIBLE
                    binding.tvTimer.text =
                        String.format("%2d:%02d:%02d", minutes, seconds, cnt)
//                binding.tvTimer.text = millisUntilFinished
                }

                override fun onFinish() {
                    binding.btnSwitchCamera.visibility = View.VISIBLE
                    binding.btnGallery.visibility = View.VISIBLE
                    countDownTimer.cancel()
                    cnt = 0L
                    time = 0L
                }
            }.start()
            localVideoCapture.startRecording(
                outputOptions, // the options needed for the final video
                requireContext().mainExecutor(), // the executor, on which the task will run
                object : VideoCapture.OnVideoSavedCallback { // the callback after recording a video
                    override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                        // Create small preview
                        outputFileResults.savedUri
                            ?.let { uri ->
                                binding.btnRecordVideo.setImageResource(R.drawable.ic_capture_video)
                                countDownTimer.cancel()
                                cnt = 0L
                                time = 0L
                                setGalleryThumbnail(uri, true)
                            }
                            ?: setLastPictureThumbnail()
                    }

                    override fun onError(
                        videoCaptureError: Int,
                        message: String,
                        cause: Throwable?,
                    ) {
                        // This function is called if there is an error during recording process
                        animateRecord.cancel()
                        val msg = "Video capture failed: $message"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        cause?.printStackTrace()
                        binding.tvTimer.visibility = View.GONE
                    }
                })
        } else {
            animateRecord.cancel()
            binding.btnSwitchCamera.visibility = View.VISIBLE
            binding.btnGallery.visibility = View.VISIBLE
            binding.btnRecordVideo.setImageResource(R.drawable.ic_capture_video)
            localVideoCapture.stopRecording()
            if (this::countDownTimer.isInitialized)
                countDownTimer.cancel()
            cnt = 0L
            time = 0L
            binding.tvTimer.visibility = View.GONE
        }
        isRecording = !isRecording
    }

    /**
     * Turns on or off the grid on the screen
     * */
    private fun toggleGrid() = binding.btnGrid.toggleButton(
        flag = hasGrid,
        rotationAngle = 180f,
        firstIcon = R.drawable.ic_grid_off,
        secondIcon = R.drawable.ic_grid_on
    ) { flag ->
        hasGrid = flag
        prefs.putBoolean(KEY_GRID, flag)
        binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
    }

    /**
     * Turns on or off the flashlight
     * */
    private fun toggleFlash() = binding.btnFlashVideo.toggleButton(
        flag = flashMode == ImageCapture.FLASH_MODE_ON,
        rotationAngle = 360f,
        firstIcon = R.drawable.ic_flash_off,
        secondIcon = R.drawable.ic_flash_on
    ) { flag ->
        isTorchOn = flag
        flashMode = if (flag) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flag)
    }

    override fun onPermissionGranted() {
        // Each time apps is coming to foreground the need permission check is being processed
        binding.viewFinder.let { vf ->
            vf.post {
                // Setting current display ID
                displayId = vf.display.displayId
                startCamera()
                lifecycleScope.launch(Dispatchers.IO) {
                    // Do on IO Dispatcher
                    setLastPictureThumbnail()
                }
                camera?.cameraControl?.enableTorch(isTorchOn)
            }
        }
    }

    private fun setLastPictureThumbnail() = binding.btnGallery.post {
//        binding.btnRecordVideo.setImageResource(R.drawable.ic_capture_video)
//        countDownTimer.cancel()
//        cnt = 0L
//        time = 0L
        getMedia().firstOrNull() // check if there are any photos or videos in the app directory
            ?.let { setGalleryThumbnail(it.uri, it.isVideo) } // preview the last one
            ?: binding.btnGallery.setImageResource(R.drawable.ic_no_picture) // or the default placeholder
    }

    private fun setGalleryThumbnail(savedUri: Uri?, isVideo: Boolean) =
        binding.btnGallery.let { btnGallery ->
            // Do the work on view's thread, this is needed, because the function is called in a Coroutine Scope's IO Dispatcher
            btnGallery.post {
                btnGallery.load(savedUri) {
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
            }
        }

    override fun onBackPressed() = requireActivity().finish()

    override fun onStop() {
        super.onStop()
        camera?.cameraControl?.enableTorch(false)
    }

    companion object {
        const val KEY_GRID = "sPrefGridVideo"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0 // aspect ratio 4x3
        private const val RATIO_16_9_VALUE = 16.0 / 9.0 // aspect ratio 16x9
    }

}
