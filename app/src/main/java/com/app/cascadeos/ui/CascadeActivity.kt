package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.View.OnTouchListener
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityCascadeBinding
import com.app.cascadeos.utility.*
import com.google.android.material.imageview.ShapeableImageView
import kotlin.math.abs


class CascadeActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCascadeBinding
    val scrollThreshold = 10f
    private var cascadePreviewDialog: Dialog? = null
    private val appBarHandler = Handler(Looper.getMainLooper())


    @SuppressLint("ClickableViewAccessibility")
    fun setTouchListenerForDialogs(view: View, dialogString: String) {
        view.setOnTouchListener(object : OnTouchListener {
            private var mHandler: Handler? = null
            private var mLongClickTriggered = false
            private val LONG_CLICK_TIME = 500 // set long press time threshold here
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mHandler = Handler(Looper.getMainLooper())
                        mHandler!!.postDelayed({
                            mLongClickTriggered = true
                            showPreviewDialog(dialogString)
                        }, LONG_CLICK_TIME.toLong())
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!mLongClickTriggered) {
                            showCascadeApps(dialogString)
                        } else {
                            dismissPreviewDialog()
                        }
                        mHandler!!.removeCallbacksAndMessages(null)
                        mLongClickTriggered = false
                        return true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        mHandler!!.removeCallbacksAndMessages(null)
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cascade)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_cascade)
        mBinding.layoutMain.layoutTransition = LayoutTransition()

        mBinding.apply {

            setTouchListenerForDialogs(tvVCall, getString(R.string.phone_andrea))
            setTouchListenerForDialogs(tvCoolTvFashion, getString(R.string.phone_andrea))
            setTouchListenerForDialogs(tvMulticastEmail, getString(R.string.phone_andrea))
            setTouchListenerForDialogs(tvWhatsapp, getString(R.string.game_angry_bird))
            setTouchListenerForDialogs(tvGameAngryBird, getString(R.string.game_angry_bird))
            setTouchListenerForDialogs(tvDigitalLocker, getString(R.string.digital_locker))


            layoutMain.setOnTouchListener(object : OnTouchListener {
                var x = 0f
                var y = 0f
                override fun onTouch(v: View?, motionEvent: MotionEvent?): Boolean {
                    when (motionEvent?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            //  enableKeyboard()
                            root.bringToFront()
                            x = motionEvent.x
                            y = motionEvent.y

                        }

                        MotionEvent.ACTION_UP -> {
                            if (abs(x - motionEvent.x) < scrollThreshold || abs(y - motionEvent.y) < scrollThreshold) {
                                if (actionBar.actionBar.visibility == View.GONE) {
                                    actionBar.actionBar.visibility = View.VISIBLE
                                } else {
                                    actionBar.actionBar.visibility = View.GONE
                                }
                            }

                        }
                        else -> {

                        }
                    }
                    return true
                }
            })
            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text = getString(R.string.cascade)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close).setOnClickListener { finish() }

            layoutHiddenActionbar.setOnClickListener {
                actionBar.actionBar.visibility = View.VISIBLE
                hideAppBar()
                hideSystemBars()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideAppBar()
    }

    private fun showCascadeApps(appsName: String) {
        val cascadeDialog = Dialog(this, R.style.CustomDialog)
        cascadeDialog.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        cascadeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        cascadeDialog.setContentView(R.layout.layout_cascade_dialog)
        cascadeDialog.setCancelable(false)
        cascadeDialog.setCanceledOnTouchOutside(false)
        when (appsName) {
            getString(R.string.phone_andrea) -> {
                cascadeDialog.findViewById<ShapeableImageView>(R.id.imgCascadeApps)
                    .setImageResource(R.drawable.img_cascade_dialog_phone_andrea)
            }
            getString(R.string.game_angry_bird) -> {
                cascadeDialog.findViewById<ShapeableImageView>(R.id.imgCascadeApps)
                    .setImageResource(R.drawable.img_cascade_dialog_angry_bird)
            }
            getString(R.string.digital_locker) -> {
                cascadeDialog.findViewById<ShapeableImageView>(R.id.imgCascadeApps)
                    .setImageResource(R.drawable.img_cascade_dialog_digital_locker)
            }
        }
        cascadeDialog.findViewById<TextView>(R.id.btnResume).setOnClickListener {
            cascadeDialog.dismiss()
            openApps(appsName)
        }
        cascadeDialog.findViewById<TextView>(R.id.btnClear).setOnClickListener { cascadeDialog.dismiss() }
        cascadeDialog.show()
        hideSystemBars()
    }

    private fun openApps(appsName: String) {
        val intent = Intent()
        intent.putExtra("ResumeApp", appsName)
        setResult(Activity.RESULT_OK, intent)
        onBackPressed()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showPreviewDialog(appsName: String) {
        cascadePreviewDialog = Dialog(this, R.style.previewImageDialog)
        cascadePreviewDialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT
        )
        cascadePreviewDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        cascadePreviewDialog?.setContentView(R.layout.layout_cascade_preview_dialog)
        cascadePreviewDialog?.setCanceledOnTouchOutside(true)
        when (appsName) {
            getString(R.string.phone_andrea) -> {
                cascadePreviewDialog?.findViewById<ShapeableImageView>(R.id.imgCascadePreview)
                    ?.setImageResource(R.drawable.img_cascade_dialog_phone_andrea)
            }
            getString(R.string.game_angry_bird) -> {
                cascadePreviewDialog?.findViewById<ShapeableImageView>(R.id.imgCascadePreview)
                    ?.setImageResource(R.drawable.img_cascade_dialog_angry_bird)
            }
            getString(R.string.digital_locker) -> {
                cascadePreviewDialog?.findViewById<ShapeableImageView>(R.id.imgCascadePreview)
                    ?.setImageResource(R.drawable.img_cascade_dialog_digital_locker)
            }
        }
        cascadePreviewDialog?.show()
        hideSystemBars()
    }

    private fun dismissPreviewDialog() {
        if (cascadePreviewDialog != null) {
            if (cascadePreviewDialog!!.isShowing) cascadePreviewDialog!!.dismiss()
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            mBinding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    /*@SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (v.id) {
            R.id.tvV_Call, R.id.tvCoolTvFashion, R.id.tvMulticastEmail -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        showPreviewDialog(getString(R.string.phone_andrea))
                    }
                    MotionEvent.ACTION_UP -> {
                        dismissPreviewDialog()
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> {
                        //
                    }
                }
            }
            R.id.tvWhatsapp, R.id.tvGameAngryBird -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        showPreviewDialog(getString(R.string.game_angry_bird))
                    }
                    MotionEvent.ACTION_UP -> {
                        dismissPreviewDialog()
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> {
                        //
                    }
                }
            }
            R.id.tvDigitalLocker -> {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        showPreviewDialog(getString(R.string.digital_locker))
                    }
                    MotionEvent.ACTION_UP -> {
                        dismissPreviewDialog()
                    }
                    MotionEvent.ACTION_BUTTON_PRESS -> {
                        //
                    }
                }
            }
        }
        return true
    }*/
}