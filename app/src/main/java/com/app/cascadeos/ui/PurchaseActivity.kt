package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityPurchaseBinding
import com.app.cascadeos.model.MediaType
import com.app.cascadeos.utility.AVTAR_URL
import com.app.cascadeos.utility.SELECTED_MEDIA_TYPE
import com.app.cascadeos.utility.start
import com.app.cascadeos.viewmodel.MainVM


class PurchaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurchaseBinding
    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        fun start(context: Context) {
            val starter = Intent(context, PurchaseActivity::class.java)
            context.startActivity(starter)
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        hideSystemBars()
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_purchase)
        binding.layoutMain.layoutTransition = LayoutTransition()
        setContentView(binding.root)
        setBlinkAnimationToCard()
        setDragAnimationToArrow()

        binding.apply {
            actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text = getString(R.string.purchase_video)
            actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close).setOnClickListener {
                MainVM.coolTvAppLivedata.postValue(getString(R.string.purchase_video))
                finish()
            }

            layoutHiddenActionbar.setOnClickListener {
                actionBar.actionBar.visibility = View.VISIBLE
                hideAppBar()
            }

            btnViewTrailer.setOnClickListener {
                VideoPlayerActivity.start(this@PurchaseActivity, AVTAR_URL)
            }
        }
        hideAppBar()
        dragView()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }


    private fun setDragAnimationToArrow() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.drag_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                setDragAnimationToArrow()
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        binding.imgDragTo.startAnimation(animation)
    }

    private fun setBlinkAnimationToCard() {
        val animation = AnimationUtils.loadAnimation(this@PurchaseActivity, R.anim.blink_animation)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
        binding.highlightView.visibility = View.VISIBLE
        binding.highlightView.startAnimation(animation)
        binding.highlightView.backgroundTintList = getColorStateList(R.color.color_clickVideo_blow)
    }


    @SuppressLint("ClickableViewAccessibility")
    fun dragView() {
        var lastX: Int = 0
        var originalX: Int = 0
        var dragLastX: Int = 0
        var dragFistX: Int = 0

        binding.imgPreview.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    originalX = view.left
                    dragLastX = binding.imgMediaDoor.right
                    dragFistX = binding.imgPreview.right
                    //  lastY = event.rawY.toInt()
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX.toInt() - lastX
                    //  val deltaY = event.rawY.toInt() - lastY
                    val newX: Int = view.left + deltaX
                    // val newY: Int = view.top + deltaY

                    lastX = event.rawX.toInt()
                    //lastY = event.rawY.toInt()

                    val rightX = newX + view.width
                    if (rightX in dragFistX..dragLastX) {
                        view.layout(newX, view.top, newX + view.width, view.bottom)
                        binding.highlightView.layout(newX, view.top, newX + view.width, view.bottom)

                        val pivotX = (binding.imgMediaDoor.width.toFloat())
                        val pivotY = (binding.imgMediaDoor.height / 2).toFloat()


                        val rotationY =
                            (180.0 * (newX - originalX) / (binding.root.width - view.getWidth())).toFloat()
                        ObjectAnimator.ofFloat(binding.imgMediaDoor, View.ROTATION_Y, rotationY)
                            .setDuration(0).start()
                        binding.imgMediaDoor.pivotX = pivotX
                        binding.imgMediaDoor.pivotY = pivotY

                        val alpha = (rotationY / 25).toInt()
                        binding.imgLightDoor.animate().alpha(alpha.toFloat()).start()

                    }


                }

                MotionEvent.ACTION_UP -> {

                    if (lastX < binding.imgLocker.left) {
                        binding.imgLightDoor.animate().alpha(0f).start()
                        view.layout(originalX, view.top, originalX + view.width, view.bottom)
                        binding.highlightView.layout(originalX, view.top, originalX + view.width, view.bottom)

                    } else {
                        playTone(R.raw.door_close)
                        Intent(this, DigitalLockerActivity::class.java).putExtra(
                            SELECTED_MEDIA_TYPE, MediaType.VIDEO.name
                        ).start(this)
                        finish()
                    }
                }
            }

            return@setOnTouchListener true
        }
    }


    private fun playTone(tone: Int) {
        val mediaPlayer: MediaPlayer = MediaPlayer.create(this, tone)
        mediaPlayer.setVolume(0.5f, 0.5f)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            mediaPlayer.release()
        }
    }
}