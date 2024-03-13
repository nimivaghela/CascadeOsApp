package com.app.cascadeos.ui.gallery

import android.os.Bundle
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import com.app.cascadeos.R
import com.app.cascadeos.adapter.GalleryMediaAdapter
import com.app.cascadeos.databinding.FragmentPreviewGalleryBinding
import com.app.cascadeos.model.GalleryMedia
import com.app.cascadeos.ui.camera.BaseFragment
import com.app.cascadeos.utility.bottomMargin
import com.app.cascadeos.utility.fitSystemWindows
import com.app.cascadeos.utility.onPageSelected
import com.app.cascadeos.utility.onWindowInsets
import com.app.cascadeos.utility.shareGalleryMedia


class PreviewGalleryFragment :
    BaseFragment<FragmentPreviewGalleryBinding>(R.layout.fragment_preview_gallery) {
    private var currentPage = 0
    var imageList: ArrayList<GalleryMedia> = arrayListOf()
    val appBarHandler = android.os.Handler(Looper.getMainLooper())

    companion object {
        const val ARG_POSITION = "ARG_POSITION"
        const val ARG_LIST = "ARG_LIST"

        fun newInstance(
            position: Int,
            imageList: ArrayList<GalleryMedia?>,
        ): PreviewGalleryFragment {
            val fragment = PreviewGalleryFragment()
            val bundle = Bundle().apply {
                putInt(ARG_POSITION, position)
                putParcelableArrayList(ARG_LIST, imageList)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override val binding: FragmentPreviewGalleryBinding by lazy {
        FragmentPreviewGalleryBinding.inflate(
            layoutInflater
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        currentPage = arguments?.getInt(ARG_POSITION)!!
        imageList = arguments?.getParcelableArrayList(ARG_LIST)!!
        return binding.root
    }

    private fun adjustInsets() {
        activity?.window?.fitSystemWindows()
//        binding.btnBack.onWindowInsets { view, windowInsets ->
//            view.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
//        }
        binding.btnShare.onWindowInsets { view, windowInsets ->
            view.bottomMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideAppBar()
        binding.pagerPhotos.apply {
            adapter = galleryMediaAdapter.apply { submitList(GalleryActivity.listOfAllImages) }
            post {
                setCurrentItem(currentPage, false)
            }
        }
        adjustInsets()
        binding.btnShare.setOnClickListener { shareImage() }
//        binding.btnDelete.setOnClickListener { deleteImage() }
        attachClickListenerForBackButton()

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
        binding.layoutHiddenActionbarCloseAtBottom?.setOnClickListener {
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

    private val galleryMediaAdapter = GalleryMediaAdapter(
//        currentPage,
        onItemClick = { isVideo, uri ->
//            if (!isVideo) {
//                val visibility =
//                    if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//                binding.groupPreviewActions.visibility = visibility
//            } else {
//                val visibility =
//                    if (binding.groupPreviewActions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
//                binding.groupPreviewActions.visibility = visibility
//            }
        },
        onDeleteClick = { isEmpty, uri ->
//            if (isEmpty) onBackPressed()
//
//            val resolver = requireContext().applicationContext.contentResolver
//            resolver.delete(uri, null, null)
        },
    )

    override fun onBackPressed() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }

    private fun shareImage() {
        galleryMediaAdapter.shareImage(binding.pagerPhotos.currentItem) { shareGalleryMedia(it) }
    }

    private fun deleteImage() {
        galleryMediaAdapter.deleteImage(currentPage)
    }

    override fun onResume() {
        super.onResume()
        if (view == null)
            return

        requireView().isFocusableInTouchMode = true
        requireView().requestFocus()
        requireView().setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                // handle back button's click listener
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
                true
            }
            false;

        }

    }

    override fun onDestroy() {
        galleryMediaAdapter.releasePlayer()
        super.onDestroy()
    }
}