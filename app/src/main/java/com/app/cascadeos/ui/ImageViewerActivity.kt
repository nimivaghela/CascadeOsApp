package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityImageViewerBinding
import com.app.cascadeos.utility.loadImage
import com.app.cascadeos.utility.showToast

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private var fileUrl: String? = ""
    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        fun start(context: Context, fileUrl: String) {
            val starter = Intent(context, ImageViewerActivity::class.java)
            starter.putExtra("url", fileUrl)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_viewer)
        setContentView(binding.root)
        binding.layoutMain.layoutTransition = LayoutTransition()

        fileUrl = intent.getStringExtra("url")
        hideAppBar()
        hideSystemBars()

        binding.layoutHiddenActionbar.setOnClickListener {
            binding.imgBack.visibility = View.VISIBLE
            hideAppBar()
        }
        binding.imgPreviewPhoto.loadImage(imageUrl = fileUrl!!, allowCaching = true,
            onStart = {
                binding.progressImageLoader.visibility = View.VISIBLE
            }, onSuccess = { _, _ ->
                binding.progressImageLoader.visibility = View.GONE
            }, onError = { _, error ->
                binding.progressImageLoader.visibility = View.GONE
                showToast(error.throwable.message)
            })
        binding.imgBack.setOnClickListener { finish() }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.imgBack.visibility = View.GONE
        }, 3000)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}