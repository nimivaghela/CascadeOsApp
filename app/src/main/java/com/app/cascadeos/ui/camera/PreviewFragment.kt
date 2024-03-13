package com.app.cascadeos.ui.camera

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.app.cascadeos.R
import com.app.cascadeos.adapter.MediaAdapter
import com.app.cascadeos.databinding.FragmentPreviewBinding
import com.app.cascadeos.utility.*

class PreviewFragment : BaseFragment<FragmentPreviewBinding>(R.layout.fragment_preview) {
    private val appBarHandler = Handler(Looper.getMainLooper())

    private val mediaAdapter = MediaAdapter(
        onItemClick = { isVideo, uri ->
//            if (!isVideo) {
//                val visibility =
//                    if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//                binding.groupPreviewActions.visibility = visibility
//                }
        },
        onDeleteClick = { isEmpty, uri ->
            if (isEmpty) onBackPressed()
            val resolver = requireContext().applicationContext.contentResolver
            resolver.delete(uri, null, null)

        },
    )


    private var currentPage = 0
    override val binding: FragmentPreviewBinding by lazy {
        FragmentPreviewBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adjustInsets()
        hideAppBar()
        attachClickListenerForBackButton()
        CameraActivity.cameraBinding.actionBar.actionBar.visibility = View.GONE
        CameraActivity.cameraBinding.layoutHiddenActionbar.visibility = View.GONE

        // Check for the permissions and show files
        if (allPermissionsGranted()) {
            binding.pagerPhotos.apply {
                adapter = mediaAdapter.apply { submitList(getMedia()) }
                onPageSelected { page ->
                    currentPage = page
                }

            }
        }

        binding.btnShare.setOnClickListener { shareImage() }

//        appBarHandler.postDelayed({
//            binding.btnBack.visibility = View.GONE
//            binding.btnShare.visibility = View.GONE
//        }, 3000)
//        binding.btnDelete.setOnClickListener { deleteImage() }
    }

    private fun attachClickListenerForBackButton() {
        binding.layoutHiddenActionbarClose.setOnClickListener {
            if (binding.groupPreviewActions.visibility == View.GONE) {
                binding.groupPreviewActions.visibility = View.VISIBLE
                hideAppBar()
            } else {
                binding.groupPreviewActions.visibility = View.GONE
            }
        }
        binding.layoutHiddenActionbarCloseAtBottom.setOnClickListener {
            if (binding.groupPreviewActions.visibility == View.GONE) {
                binding.groupPreviewActions.visibility = View.VISIBLE
                hideAppBar()
            } else {
                binding.groupPreviewActions.visibility = View.GONE
            }
        }
        binding.btnBack.setOnClickListener {
            binding.groupPreviewActions.visibility = View.GONE
            onBackPressed()
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.groupPreviewActions.visibility = View.GONE
        }, 3000)
    }

    /**
     * This methods adds all necessary margins to some views based on window insets and screen orientation
     * */
    private fun adjustInsets() {
        activity?.window?.fitSystemWindows()
//        binding.btnBack.onWindowInsets { view, windowInsets ->
//            view.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//        }
        binding.btnShare.onWindowInsets { view, windowInsets ->
            view.bottomMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        }
    }

    private fun shareImage() {
        mediaAdapter.shareImage(currentPage) { share(it) }
    }

    private fun deleteImage() {
        mediaAdapter.deleteImage(currentPage)
    }

    override fun onBackPressed() {
        findNavController().popBackStack()
        //view?.let { Navigation.findNavController(it).popBackStack() }
    }

    override fun onDestroy() {
        mediaAdapter.releasePlayer()
        super.onDestroy()
    }

}