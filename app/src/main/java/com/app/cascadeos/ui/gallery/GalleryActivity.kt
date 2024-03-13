package com.app.cascadeos.ui.gallery

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.loader.content.CursorLoader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityGalleryBinding
import com.app.cascadeos.model.GalleryMedia


class GalleryActivity : AppCompatActivity(),
    GalleryImagesAdapter.GalleryImageClickListener {
    var path = ArrayList<Uri>()

    private lateinit var mBinding: ActivityGalleryBinding
    private val galleryImagesAdapter by lazy {
        return@lazy GalleryImagesAdapter(this)
    }
    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        val listOfAllImages: ArrayList<GalleryMedia?> = arrayListOf()
    }

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_gallery)
        mBinding.layoutMain.layoutTransition = LayoutTransition()
        mBinding.apply {
            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text =
                getString(R.string.gallery)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close)
                .setOnClickListener { finish() }

            layoutHiddenActionbar.setOnClickListener {
                actionBar.actionBar.visibility = View.VISIBLE
                hideAppBar()
            }
        }

        hideAppBar()
        findViewById<RecyclerView>(R.id.rvGallery).adapter = galleryImagesAdapter

        if (hasRequiredPermissionForReadExternalStorage()) {
            getAllGalleryMedia()
        } else {
            readExternalStoragePermissionResult.launch(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            mBinding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    private fun hasRequiredPermissionForReadExternalStorage(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val readExternalStoragePermissionResult: ActivityResultLauncher<Array<String>> by lazy {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val permissionGranted = permissions.entries.all { entries ->
                entries.value
            }
            if (permissionGranted) {
                getAllGalleryMedia()
            }
        }
    }

    @SuppressLint("Range")
    fun getAllGalleryMedia(): ArrayList<GalleryMedia> {
        listOfAllImages.clear()
        val result = ArrayList<GalleryMedia>()

        val listOfAllImagesId: MutableList<Long?> = mutableListOf()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.TITLE
        )

        val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

        val queryUri = MediaStore.Files.getContentUri("external")

        val cursorLoader = CursorLoader(
            this,
            queryUri,
            projection,
            selection,
            null,
            MediaStore.Files.FileColumns.DATE_ADDED + " DESC"
        )
        val cursor: Cursor? = cursorLoader.loadInBackground()
        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))
                val displayName =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME))
                val imagePath =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                val uriImage = Uri.withAppendedPath(uriExternal, "" + id)
                val dateAdded =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED))
                val mimeType =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE))
                result.add(GalleryMedia(id, displayName, imagePath, dateAdded, mimeType, uriImage))
                listOfAllImages.add(
                    GalleryMedia(
                        id,
                        displayName,
                        imagePath,
                        dateAdded,
                        mimeType,
                        uriImage
                    )
                )
                listOfAllImagesId.add(id)
            }
            cursor.close()
        }
        galleryImagesAdapter.galleryImageList = listOfAllImages as ArrayList<GalleryMedia?>
        galleryImagesAdapter.galleryImageId = listOfAllImagesId as ArrayList<Long?>
        galleryImagesAdapter.notifyDataSetChanged()

        println(listOfAllImages)
        return result
    }

    override fun onGalleryImageClick(uri: Uri, position: Int) {
        val fragment: PreviewGalleryFragment =
            PreviewGalleryFragment.newInstance(position, listOfAllImages)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, fragment).commit()
    }

    override fun onCameraClick() {

    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    /*override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_gallery_landscape)
            constraintSet.applyTo(mBinding.layoutMain)
            mBinding = DataBindingUtil.setContentView(this, R.layout.activity_gallery_landscape)

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_gallery)
            constraintSet.applyTo(mBinding.layoutMain)
            mBinding = DataBindingUtil.setContentView(this, R.layout.activity_gallery)
        }

    }*/

}