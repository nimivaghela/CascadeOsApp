package com.app.cascadeos.ui.camera

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.camera.core.CameraSelector
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityCameraBinding
import com.app.cascadeos.utility.bottomMargin
import com.app.cascadeos.utility.dpToPx
import com.app.cascadeos.utility.endMargin
import com.app.cascadeos.utility.startMargin
import com.app.cascadeos.utility.topMargin
import com.app.cascadeos.viewmodel.CameraVM
import com.app.cascadeos.viewmodel.MainVM

class CameraActivity : AppCompatActivity() {

    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        lateinit var cameraBinding: ActivityCameraBinding
        var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    }

    val cameraVM: CameraVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        cameraBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera)
        cameraBinding.apply {
            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text =
                getString(R.string.camera)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close)
                .setOnClickListener { finish() }

            layoutHiddenActionbar.setOnClickListener {
                if (!actionBar.actionBar.isVisible) {
                    actionBar.actionBar.visibility = View.VISIBLE
                    hideAppBar()
                }
            }
        }
        hideAppBar()
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            cameraBinding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            cameraBinding.layoutMain.apply {
                topMargin = 12.dpToPx(context = this@CameraActivity).toInt()
                bottomMargin = 0.dpToPx(context = this@CameraActivity).toInt()
                endMargin = 44.dpToPx(context = this@CameraActivity).toInt()
                startMargin = 12.dpToPx(context = this@CameraActivity).toInt()
            }
        } else {
            cameraBinding.layoutMain.apply {
                topMargin = 12.dpToPx(context = this@CameraActivity).toInt()
                bottomMargin = 44.dpToPx(context = this@CameraActivity).toInt()
                endMargin = 12.dpToPx(context = this@CameraActivity).toInt()
                startMargin = 0.dpToPx(context = this@CameraActivity).toInt()
            }
        }
    }


}