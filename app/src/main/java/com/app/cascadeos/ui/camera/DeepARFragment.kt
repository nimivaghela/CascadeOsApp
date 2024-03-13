package com.app.cascadeos.ui.camera

import ai.deepar.ar.ARErrorType
import ai.deepar.ar.AREventListener
import ai.deepar.ar.CameraResolutionPreset
import ai.deepar.ar.DeepAR
import ai.deepar.ar.DeepARImageFormat
import android.animation.ObjectAnimator
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaActionSound
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.animation.doOnCancel
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import coil.decode.VideoFrameDecoder
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.app.cascadeos.R
import com.app.cascadeos.databinding.FragmentDeepARBinding
import com.app.cascadeos.utility.saveImageExternal
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.cameraview.controls.Mode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutionException

class DeepARFragment : BaseFragment<FragmentDeepARBinding>(R.layout.fragment_deep_a_r),
    AREventListener, SurfaceHolder.Callback {

    private var deepAR: DeepAR? = null
    private var currentEffect = 0
    private lateinit var effects: ArrayList<String>

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var width = 0
    private var height = 0
    private var buffersInitialized = false
    private lateinit var buffers: Array<ByteBuffer>
    private val NUMBER_OF_BUFFERS = 2
    private val useExternalCameraTexture = true
    private val defaultLensFacing = CameraSelector.LENS_FACING_FRONT
    private var lensFacing: Int = defaultLensFacing
    private var surfaceProvider: ARSurfaceProvider? = null
    private var recording = false
    private var currentSwitchRecording = false
    private var isVideoStarting = false
    private var baseTime = SystemClock.elapsedRealtime()
    private lateinit var videoFile: File

    override val binding: FragmentDeepARBinding by lazy {
        FragmentDeepARBinding.inflate(
            layoutInflater
        )
    }
    private var currentBuffer = 0

    private val animateRecord by lazy {
        ObjectAnimator.ofFloat(binding.btnTakePicture, View.ALPHA, 1f, 0.5f).apply {
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            doOnCancel { binding.btnTakePicture.alpha = 1f }
        }
    }

    override fun onBackPressed() {
        findNavController().popBackStack()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CameraActivity.cameraBinding.layoutHiddenActionbar.visibility = View.VISIBLE

    }

    override fun onPermissionGranted() {
        super.onPermissionGranted()
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
                                    result.source, options
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


    private fun initialize() {
        initializeDeepAR()
        initializeFilters()
        initViews()
    }

    private fun initViews() {
        binding.cameraView.holder.removeCallback(this)
        binding.cameraView.holder.addCallback(this)

        /*  binding.cameraView.visibility = View.GONE
          binding.cameraView.visibility = View.VISIBLE*/


        binding.btnFilter.setOnClickListener {
            gotoNext()
        }


        binding.btnSwitchCamera.setOnClickListener {
            switchCamera()
        }

        binding.btnGallery.setOnClickListener {
            openPreview()
        }


        binding.btnMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_photo -> {
                        binding.isVideo = false

                    }

                    R.id.btn_video -> {
                        binding.isVideo = true
                    }
                }
            }
        }

        binding.btnTakePicture.setOnClickListener {
            if (binding.isVideo) {
                if (isVideoStarting) {
                    deepAR?.stopVideoRecording()
                } else {
                    takeVideo()
                }
            } else {
                deepAR?.takeScreenshot()
            }
        }
    }

    private fun takeVideo() {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            getString(R.string.app_name)
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        videoFile = File(dir, "${System.currentTimeMillis()}.mp4")
        deepAR?.startVideoRecording(videoFile.path)
    }


    private fun initializeDeepAR() {
        deepAR = DeepAR(requireContext())
        deepAR?.setLicenseKey(getString(R.string.deep_ar_key))
        deepAR?.initialize(requireContext(), this)
        setupCamera()
    }

    override fun screenshotTaken(bitmap: Bitmap?) {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
        binding.btnTakePicture.setImageResource(R.drawable.ic_take_picture)
        bitmap?.let {
            val fileURI = it.saveImageExternal(
                "${System.currentTimeMillis()}.jpg", requireContext(), getString(R.string.app_name)
            )
            fileURI?.let { uri ->
                setGalleryThumbnail(uri, false)
            }
        }
    }

    override fun videoRecordingStarted() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.START_VIDEO_RECORDING)
        isVideoStarting = true
        binding.tvTimer.base = SystemClock.elapsedRealtime()
        baseTime = binding.tvTimer.base
        binding.tvTimer.start()
        binding.btnTakePicture.setImageResource(R.drawable.ic_at_capture_video)
        binding.btnSwitchCamera.visibility = View.INVISIBLE
        binding.btnGallery.visibility = View.INVISIBLE
        binding.btnFilter.visibility = View.INVISIBLE
        binding.btnMode.visibility = View.INVISIBLE
        animateRecord.start()

    }

    override fun videoRecordingFinished() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        isVideoStarting = false
        binding.tvTimer.stop()
        binding.tvTimer.base = SystemClock.elapsedRealtime()
        baseTime = binding.tvTimer.base
        binding.btnTakePicture.setImageResource(R.drawable.ic_capture_video)
        binding.btnSwitchCamera.visibility = View.VISIBLE
        binding.btnGallery.visibility = View.VISIBLE
        binding.btnFilter.visibility = View.VISIBLE
        binding.btnMode.visibility = View.VISIBLE
        animateRecord.cancel()
        val authority = requireContext().applicationContext.packageName + ".provider"
        setGalleryThumbnail(
            FileProvider.getUriForFile(
                requireContext(), authority, videoFile
            ), true
        )

    }

    override fun videoRecordingFailed() {

    }

    override fun videoRecordingPrepared() {

    }

    override fun shutdownFinished() {

    }

    override fun initialized() {
        // Restore effect state after deepar release
        deepAR?.switchEffect("effect", getFilterPath(effects[currentEffect]))
    }

    override fun faceVisibilityChanged(p0: Boolean) {

    }

    override fun imageVisibilityChanged(p0: String?, p1: Boolean) {

    }

    override fun frameAvailable(p0: Image?) {

    }

    override fun error(p0: ARErrorType?, p1: String?) {

    }

    override fun effectSwitched(p0: String?) {

    }


    private fun getFilterPath(filterName: String): String? {
        return if (filterName == "none") {
            null
        } else "file:///android_asset/$filterName"
    }

    private fun initializeFilters() {
        effects = ArrayList<String>()
        effects.add("none")
        effects.add("Vendetta_Mask.deepar")
        effects.add("Neon_Devil_Horns.deepar")
        effects.add("viking_helmet.deepar")
        effects.add("Humanoid.deepar")
        effects.add("Pixel_Hearts.deepar")
        effects.add("galaxy_background.deepar")
        effects.add("Hope.deepar")
        effects.add("burning_effect.deepar")
        effects.add("flower_face.deepar")
        effects.add("Elephant_Trunk.deepar")
        effects.add("Stallone.deepar")

        /*effects.add("MakeupLook.deepar")
        effects.add("Split_View_Look.deepar")
        effects.add("Emotions_Exaggerator.deepar")
        effects.add("Emotion_Meter.deepar")

        effects.add("Ping_Pong.deepar")
        effects.add("Snail.deepar")
        effects.add("Fire_Effect.deepar")*/


    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // If we are using on screen rendering we have to set surface view where DeepAR will render
        deepAR?.setRenderSurface(holder.surface, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        holder.surface.release()
        deepAR?.setRenderSurface(null, 0, 0)

    }

    private fun setupCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture?.addListener({
            try {
                val cameraProvider = cameraProviderFuture?.get()
                cameraProvider?.let { bindImageAnalysis(it) }
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindImageAnalysis(cameraProvider: ProcessCameraProvider) {
        val cameraResolutionPreset = CameraResolutionPreset.P1920x1080
        val width: Int
        val height: Int
        val orientation: Int = getScreenOrientation()
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE || orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            width = cameraResolutionPreset.width
            height = cameraResolutionPreset.height
        } else {
            width = cameraResolutionPreset.height
            height = cameraResolutionPreset.width
        }
        val cameraResolution = Size(width, height)
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        if (useExternalCameraTexture) {
            val preview = Preview.Builder().setTargetResolution(cameraResolution).build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, preview)
            if (surfaceProvider == null) {
                surfaceProvider = deepAR?.let { ARSurfaceProvider(requireContext(), it) }
            }
            preview.setSurfaceProvider(surfaceProvider)
            surfaceProvider?.setMirror(lensFacing == CameraSelector.LENS_FACING_FRONT)
        } else {
            val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(cameraResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(requireContext()), imageAnalyzer
            )
            buffersInitialized = false
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle((this as LifecycleOwner), cameraSelector, imageAnalysis)
        }
    }


    private fun getScreenOrientation(): Int {
        val rotation: Int = requireActivity().windowManager.defaultDisplay.rotation
        val dm = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(dm)
        width = dm.widthPixels
        height = dm.heightPixels
        // if the device's natural orientation is portrait:
        val orientation: Int =
            if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
                when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            } else {
                when (rotation) {
                    Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        return orientation
    }


    private fun initializeBuffers(size: Int) {
        this.buffers = Array(NUMBER_OF_BUFFERS) { i ->
            ByteBuffer.allocateDirect(size)
        }
        this.buffers.forEach {
            it.order(ByteOrder.nativeOrder())
            it.position(0)
        }
    }


    private val imageAnalyzer = ImageAnalysis.Analyzer { image ->
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        if (!buffersInitialized) {
            buffersInitialized = true
            initializeBuffers(ySize + uSize + vSize)
        }
        val byteData = ByteArray(ySize + uSize + vSize)
        val width = image.width
        val yStride = image.planes[0].rowStride
        val uStride = image.planes[1].rowStride
        val vStride = image.planes[2].rowStride
        var outputOffset = 0
        if (width == yStride) {
            yBuffer[byteData, outputOffset, ySize]
            outputOffset += ySize
        } else {
            var inputOffset = 0
            while (inputOffset < ySize) {
                yBuffer.position(inputOffset)
                yBuffer[byteData, outputOffset, Math.min(yBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += yStride
            }
        }
        //U and V are swapped
        if (width == vStride) {
            vBuffer[byteData, outputOffset, vSize]
            outputOffset += vSize
        } else {
            var inputOffset = 0
            while (inputOffset < vSize) {
                vBuffer.position(inputOffset)
                vBuffer[byteData, outputOffset, Math.min(vBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += vStride
            }
        }
        if (width == uStride) {
            uBuffer[byteData, outputOffset, uSize]
            outputOffset += uSize
        } else {
            var inputOffset = 0
            while (inputOffset < uSize) {
                uBuffer.position(inputOffset)
                uBuffer[byteData, outputOffset, Math.min(uBuffer.remaining(), width)]
                outputOffset += width
                inputOffset += uStride
            }
        }
        buffers[currentBuffer].put(byteData)
        buffers[currentBuffer].position(0)
        if (deepAR != null) {
            deepAR?.receiveFrame(
                buffers[currentBuffer],
                image.width,
                image.height,
                image.imageInfo.rotationDegrees,
                lensFacing == CameraSelector.LENS_FACING_FRONT,
                DeepARImageFormat.YUV_420_888,
                image.planes[1].pixelStride
            )
        }
        currentBuffer = (currentBuffer + 1) % NUMBER_OF_BUFFERS
        image.close()
    }


    private fun gotoNext() {
        currentEffect = (currentEffect + 1) % effects.size
        deepAR?.switchEffect("effect", getFilterPath(effects[currentEffect]))
    }

    override fun onStart() {
        super.onStart()
        initialize()
    }


    override fun onStop() {
        super.onStop()
        recording = false
        currentSwitchRecording = false
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = cameraProviderFuture?.get()
            cameraProvider?.unbindAll()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (surfaceProvider != null) {
            surfaceProvider?.stop()
            surfaceProvider = null
        }
        deepAR?.release()
        deepAR = null

    }

    override fun onDestroy() {
        super.onDestroy()
        if (surfaceProvider != null) {
            surfaceProvider?.stop()
        }
        if (deepAR == null) {
            return
        }
        deepAR?.setAREventListener(null)
        deepAR?.release()
        deepAR = null

    }


    private fun switchCamera() {
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        //unbind immediately to avoid mirrored frame.
        //unbind immediately to avoid mirrored frame.
        var cameraProvider: ProcessCameraProvider? = null
        try {
            cameraProvider = cameraProviderFuture?.get()
            cameraProvider?.unbindAll()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        setupCamera()
    }


    private fun openPreview() {
        if (getMedia().isEmpty()) return
        findNavController().navigate(R.id.action_deepARFragment_to_previewFragment)

    }


}